# Guia da Etapa 5: pagamento simulado

Agora a reserva criada na Etapa 4 (que nasce "aguardando pagamento") pode ser **confirmada**
por um pagamento. O pagamento é **simulado**: não há dinheiro nem gateway real, mas o fluxo e
o registro no banco são de verdade.

---

## Parte A: atualizar e rodar

1. `git pull` e **Reload All Maven Projects**.
2. Rode a aplicação. No console você verá a migração nova:
   `Migrating schema "public" to version "3 - pagamentos"`. Ela cria a tabela `payments`.

---

## Parte B: o fluxo

```
Reserva criada (Etapa 4)  ──▶  status = PENDING_PAYMENT  (assento já segurado)
        │
        │  POST /api/reservations/{id}/payment  {cardNumber, cardHolder, method}
        ▼
  PaymentService.pay:
    - a reserva é do usuário? está PENDING? a sessão é futura?
    - "gateway" simulado decide: cartão termina em 0000 → RECUSADO, senão APROVADO
        ├─ APROVADO  → grava payment APPROVED + reserva vira CONFIRMED → 200
        └─ RECUSADO  → grava payment DECLINED, reserva continua PENDING → 402 (pode tentar de novo)
```

**Regra da simulação:** cartão cujo número termina em **`0000`** é recusado; qualquer outro
é aprovado. É assim de propósito, para você testar os dois caminhos.

---

## Parte C: o que cada classe faz

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `PaymentController` | `POST /api/reservations/{id}/payment` e `GET .../payments` (histórico), ambos do usuário logado | — |
| `PaymentService` / `PaymentServiceImpl` | A regra do pagamento | `pay`: valida dono/status/sessão, simula o gateway, confirma a reserva ou registra a recusa |
| `Payment` (entidade) | Uma tentativa de pagamento (aprovada ou recusada) | `isApproved` |
| `PaymentRequest` / `PaymentResponse` | Entrada (cartão) e saída (resultado + status da reserva) | `PaymentRequest` valida 16 dígitos e o método |
| `PaymentStatus` | `APPROVED` ou `DECLINED` | — |
| `PaymentDeclinedException` | Vira HTTP `402 Payment Required` | — |
| `PaymentRepository` | Acesso à tabela `payments` | `findByReservationIdOrderByCreatedAtDesc` |

### Dois pontos que valem a atenção

- **`noRollbackFor` de novo** (decisão D27): quando o pagamento é recusado, salvamos o
  registro e **depois** lançamos a exceção. Sem `noRollbackFor`, a transação desfaria o insert
  do pagamento recusado. É o mesmo padrão do refresh token (Etapa 2).
- **Mesma proteção de dono** (decisão D28): o usuário vem do token; pagar a reserva de outro
  dá `404`.

---

## Parte D: testar no Insomnia ou Postman

Faça uma reserva (Etapa 4) com um usuário comum e anote o `id` dela (ela estará
`PENDING_PAYMENT`). Use o token desse usuário.

**1. Pagamento recusado** (cartão termina em 0000)
`POST /api/reservations/{id}/payment`
```json
{ "cardNumber": "4111111111110000", "cardHolder": "Seu Nome", "method": "CREDIT" }
```
Resposta `402`. A reserva continua `PENDING_PAYMENT` (confira em `GET /api/reservations/{id}`).

**2. Pagamento aprovado**
```json
{ "cardNumber": "4111111111111234", "cardHolder": "Seu Nome", "method": "PIX" }
```
Resposta `200` com `status: APPROVED`, `reservationStatus: CONFIRMED` e um `transactionRef`
(o "comprovante"). A reserva agora está confirmada.

**3. Pagar de novo** a mesma reserva → `400` ("não está aguardando pagamento").

**4. Histórico** `GET /api/reservations/{id}/payments`: mostra as duas tentativas
(a recusada e a aprovada).

**5. Erros esperados:** cartão com menos de 16 dígitos (`400` de validação), método diferente
de CREDIT/DEBIT/PIX (`400`), pagar a reserva de outra pessoa (`404`), sem token (`401`).

---

## Parte E: ver no pgAdmin

- Tabela `payments`: uma linha por tentativa. Repare que as recusadas também ficam, com
  `status = DECLINED` e `transaction_ref` vazio.
- Rode no **Query Tool** para ver reserva + pagamento aprovado:
  ```sql
  SELECT r.id AS reserva, r.status, p.status AS pagamento, p.amount, p.transaction_ref
  FROM reservations r
  LEFT JOIN payments p ON p.reservation_id = r.id AND p.status = 'APPROVED'
  ORDER BY r.id;
  ```

---

## Resumo dos endpoints da Etapa 5

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| POST | `/api/reservations/{id}/payment` | logado (dono) | Paga a reserva (confirma se aprovado) |
| GET | `/api/reservations/{id}/payments` | logado (dono) | Histórico de pagamentos da reserva |
