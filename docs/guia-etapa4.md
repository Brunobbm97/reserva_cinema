# Guia da Etapa 4: reserva de assentos (e a proteção contra overbooking)

Esta é a etapa mais importante do projeto. Aqui o usuário reserva assentos de uma sessão,
vê e cancela as próprias reservas, e o sistema garante que **um assento nunca seja vendido
duas vezes**, mesmo com muita gente clicando ao mesmo tempo.

---

## Parte A: atualizar e rodar

1. `git pull` e **Reload All Maven Projects** (sem dependências novas).
2. Rode a aplicação. Não há migração nova: as tabelas `reservations` e `reservation_seats`,
   e o índice que protege contra overbooking, já existem desde a Etapa 1.

---

## Parte B: a ideia central, em uma frase

> Em vez de o código perguntar "o assento está livre?" antes de reservar, deixamos o **banco
> de dados** recusar automaticamente qualquer segundo pedido do mesmo assento, e tratamos
> essa recusa como um conflito (HTTP 409).

Por que não perguntar antes? Porque entre a pergunta e a gravação, outra pessoa pode reservar.
Isso se chama **condição de corrida**. O banco é o único lugar que enxerga todos os pedidos
em ordem, então é ele quem deve decidir. A regra que faz isso é o índice único parcial
`uq_active_seat_per_showtime`, que já criamos na Etapa 1.

---

## Parte C: o que cada classe faz

### Pacote `reservation/` (ganhou service, controller e DTOs)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `ReservationController` | Rotas `/api/reservations` (todas exigem login) | O usuário vem sempre do token (`@AuthenticationPrincipal`), nunca do corpo |
| `ReservationService` / `ReservationServiceImpl` | As regras da reserva | `create`: valida e segura os assentos; `cancel`: libera; `findOwned`: garante que a reserva é do usuário |
| `CreateReservationRequest` | O que o cliente envia: `showtimeId` e `seatIds` | Repare que **não** há `userId` |
| `ReservationResponse` | A reserva como a API mostra | `from` monta só os assentos ativos |
| `Reservation` / `ReservationSeat` (entidades, da Etapa 1) | A reserva e seus assentos | `addSeat` soma o total; `cancel`/`release` liberam os assentos |

### O método `create`, passo a passo (a parte mais importante)

1. Busca a sessão (`404` se não existe) e recusa se ela já começou.
2. Remove ids repetidos e confere que todos os assentos existem **e** são da sala daquela sessão.
3. Cria a reserva com status `PENDING_PAYMENT` e adiciona os assentos.
4. Chama `saveAndFlush`: isto força o `INSERT` dos assentos **agora**, dentro de um `try`.
5. Se o banco recusar por causa do índice único (`DataIntegrityViolationException`),
   respondemos `409 Conflict`: "o assento acabou de ser reservado por outra pessoa".

**Por que o `flush` é essencial:** por padrão o Hibernate só conversa com o banco no fim da
transação (no commit), que acontece depois do nosso método. Sem o `flush`, a violação
estouraria fora do nosso `try` e viraria um erro `500`. Com o `flush`, o erro acontece dentro
do `try` e vira um `409` limpo. (Isto aconteceu de verdade no desenvolvimento: o primeiro teste
deu `500`; depois do ajuste, `409`.)

### Três conceitos de segurança que aparecem aqui

- **Condição de corrida e por que confiar no banco** (decisão D22).
- **IDOR** (decisão D23): o usuário vem do token, e buscamos a reserva por dono
  (`findByIdAndUserId`). A reserva de outra pessoa dá `404`, nem revelando que existe.
- **Soft delete / histórico** (decisão D25): cancelar não apaga, desativa. Libera o assento e
  mantém o registro para os relatórios.

---

## Parte D: testar no Insomnia ou Postman

Prepare o cenário como admin: crie um filme, uma sala (ex.: fileira A com 5 lugares) e uma
sessão futura. Pegue os ids dos assentos em `GET /api/showtimes/{id}/seats`. Crie dois
usuários comuns (ex.: Ana e Bia) e guarde o token de cada um.

**1. Ana reserva A1 e A2**
`POST /api/reservations` (token da Ana)
```json
{ "showtimeId": 1, "seatIds": [1, 2] }
```
Resposta `201` com status `PENDING_PAYMENT`, o total (2 × preço) e os assentos.

**2. Bia tenta o A2 (já ocupado)**
Mesma rota, token da Bia, `"seatIds": [2]` → `409 Conflict`. O overbooking foi impedido.

**3. Ver o mapa** `GET /api/showtimes/1/seats`: A1 e A2 aparecem com `available: false`.

**4. Segurança (IDOR):** com o token da **Bia**, tente `GET /api/reservations/{id da reserva
da Ana}` → `404`. Ela não enxerga a reserva de outra pessoa.

**5. Minhas reservas** `GET /api/reservations` (token da Ana): lista as dela.

**6. Ana cancela** `POST /api/reservations/{id}/cancel` → status `CANCELLED`. No mapa, A1 e A2
voltam a ficar livres, e agora a Bia consegue reservar o A2.

**7. Erros esperados:** reservar sem token (`401`), assento de outra sala (`400`), assento
inexistente (`404`), cancelar duas vezes (`400`).

### Experimento de concorrência (o mais legal)

No terminal, dispare vários pedidos **ao mesmo tempo** no mesmo assento:
```bash
TOKEN="<access token de um usuário>"
for i in $(seq 1 8); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/reservations \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d '{"showtimeId":1,"seatIds":[1]}' &
done
wait
```
O resultado é sempre **um `201` e o resto `409`**. No desenvolvimento, rodei isto com 8 pedidos
simultâneos: saiu exatamente 1 sucesso e 7 conflitos, com 1 linha ativa no banco. É a prova de
que a proteção funciona sob concorrência real.

---

## Parte E: ver no pgAdmin

- `reservations`: uma linha por reserva, com `status` e `total_amount`.
- `reservation_seats`: uma linha por assento reservado. Cancele uma reserva e veja as linhas
  dela com `active = false` (não somem: é o histórico).
- Rode no **Query Tool** para ver os assentos ocupados de uma sessão:
  ```sql
  SELECT s.row_label || s.seat_number AS assento, r.status
  FROM reservation_seats rs
  JOIN seats s ON s.id = rs.seat_id
  JOIN reservations r ON r.id = rs.reservation_id
  WHERE rs.showtime_id = 1 AND rs.active;
  ```
- Veja o índice que faz a mágica: na tabela `reservation_seats`, pasta **Indexes**,
  `uq_active_seat_per_showtime`.

---

## Resumo dos endpoints da Etapa 4

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| POST | `/api/reservations` | logado | Reserva assentos de uma sessão |
| GET | `/api/reservations` | logado | Minhas reservas |
| GET | `/api/reservations/{id}` | logado (dono) | Uma reserva minha |
| POST | `/api/reservations/{id}/cancel` | logado (dono) | Cancela uma reserva futura |
