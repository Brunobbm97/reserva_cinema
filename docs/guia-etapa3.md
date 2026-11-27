# Guia da Etapa 3: filmes, salas e sessões

Agora o admin consegue montar o catálogo do cinema (filmes, salas e sessões) e qualquer
pessoa consegue consultar o que está em cartaz e ver os assentos de uma sessão.

---

## Parte A: atualizar e rodar

1. `git pull` na pasta do projeto (ou **Git → Pull** no IntelliJ).
2. **Reload All Maven Projects** (não entraram dependências novas, mas é um bom hábito).
3. Rode a `ReservaCinemaApplication`. O Flyway não tem migração nova nesta etapa (o modelo de
   dados já existia desde a Etapa 1); a novidade está no código Java.
4. Na primeira vez que você subir um pôster, aparece uma pasta `uploads/posters/` na raiz do
   projeto. Ela está no `.gitignore`, então não vai para o GitHub.

---

## Parte B: o que cada classe faz

### Pacote `room/` (ganhou service, controller e DTOs)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `RoomController` | `POST/DELETE /api/admin/rooms` (admin) e `GET /api/rooms` (logado) | — |
| `RoomService` / `RoomServiceImpl` | Cria a sala **com** o mapa de assentos numa transação | `create`: monta A1..A10, B1..B10 e bloqueia assento repetido; `delete`: bloqueia se a sala já tem sessões |
| `CreateRoomRequest` | Nome + lista de fileiras (`{label, seats}`) | `RowDefinition` é um record aninhado |
| `RoomResponse` / `SeatResponse` | A sala e seus assentos como a API mostra | `from` (completo) e `summary` (sem a lista de assentos) |

### Pacote `movie/` (ganhou service, controller, DTOs e upload)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `MovieController` | `GET` público; `POST/PUT/DELETE` e upload de pôster só admin | `uploadPoster` recebe `multipart/form-data`, não JSON |
| `MovieService` / `MovieServiceImpl` | CRUD de filme e troca de pôster | `resolveGenres`: reaproveita gêneros existentes e cria os novos; `delete`: apaga o arquivo do pôster junto |
| `MovieRequest` / `MovieResponse` | Entrada e saída; gêneros vão como **nomes** | `MovieResponse.from` monta a `posterUrl` pública |

### Pacote `showtime/` (ganhou service, controller e DTOs)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `ShowtimeController` | `GET` público (por data, por filme, por id, mapa de assentos); `POST/DELETE` só admin | `list` aceita `?date=2026-12-01` |
| `ShowtimeService` / `ShowtimeServiceImpl` | Agenda sessões e calcula disponibilidade | `create`: valida filme/sala e impede sobreposição na mesma sala; `seatAvailability`: cruza os assentos da sala com as reservas ativas |
| `ShowtimeRequest` / `ShowtimeResponse` | Entrada e saída | `ShowtimeRequest` valida `@Future` (sessão no futuro) |
| `SeatAvailabilityResponse` | Mapa de assentos: total, livres e a lista com `available` por assento | — |
| `ShowtimeRepository` | Consultas | `countOverlapping`: conta sessões sobrepostas; `findByStartsAt...Between`: sessões de um intervalo |

### Pacote `common/storage/` (novo)

| Classe | O que faz |
|---|---|
| `PosterStorageService` / `LocalPosterStorageService` | Grava e remove o arquivo do pôster no disco, com nome gerado (UUID) e validação de tipo de imagem |
| `StorageProperties` | Lê `app.storage.poster-dir` da configuração |
| `WebConfig` (em `config/`) | Faz a pasta `uploads/` ser servida pela URL `/files/**` |

### Dois conceitos que aparecem aqui

- **DTO protege contra `LazyInitializationException`.** As entidades têm relações `LAZY`
  (ex.: a sessão só carrega o filme quando preciso). Se devolvêssemos a entidade direto, o
  JSON tentaria ler essas relações **fora** da transação e quebraria. Convertendo para DTO
  dentro do service, isso não acontece (decisão D19).
- **Segurança no upload.** O nome do arquivo nunca vem do usuário: geramos um UUID e
  conferimos que o caminho final fica dentro da pasta. Isso evita *path traversal* (decisão D18).

---

## Parte C: testar no Insomnia ou Postman

Faça login como admin (`admin@cinema.com` / `admin123`) e use o `accessToken` nas rotas de
admin (aba **Auth → Bearer Token**).

**1. Criar uma sala com mapa de assentos**
`POST /api/admin/rooms`
```json
{ "name": "Sala 1", "rows": [ { "label": "A", "seats": 10 }, { "label": "B", "seats": 10 } ] }
```
Resposta `201` com os 20 assentos (A1..A10, B1..B10). Anote o `id` da sala.

**2. Criar um filme**
`POST /api/admin/movies`
```json
{ "title": "Interestelar", "description": "Ficção científica", "durationMinutes": 169,
  "genres": ["Ficção", "Drama"] }
```
Anote o `id`. Crie um segundo filme com o gênero `"ficção"` (minúsculo) e repare que ele
reaproveita o mesmo gênero (não cria duplicado).

**3. Enviar o pôster**
`POST /api/admin/movies/{id}/poster` — em **Body**, escolha **Multipart / Form Data**,
campo `file` do tipo **File**, e selecione uma imagem JPEG/PNG. A resposta traz a `posterUrl`.
Abra `http://localhost:8080` + essa URL no navegador para ver a imagem. Tente enviar um `.txt`
e veja o erro `400`.

**4. Listar o catálogo (sem token, é público)**
`GET /api/movies` e `GET /api/movies/{id}`.

**5. Criar uma sessão**
`POST /api/admin/showtimes`
```json
{ "movieId": 1, "roomId": 1, "startsAt": "2027-01-10T23:00:00Z", "price": 30.00 }
```
`startsAt` é um horário com fuso (o `Z` é UTC). A resposta calcula o `endsAt` somando a
duração do filme. Tente criar outra sessão na **mesma sala** com horário sobreposto: `409`.

**6. Consultar sessões (público)**
- `GET /api/showtimes?date=2027-01-10` — sessões daquele dia.
- `GET /api/movies/{id}/showtimes` — sessões de um filme.
- `GET /api/showtimes/{id}/seats` — o mapa de assentos, com `available: true/false` por lugar.
  Por enquanto todos aparecem livres, porque as reservas vêm na Etapa 4.

---

## Parte D: ver no pgAdmin

- `movies`, `genres` e `movie_genres`: crie um filme com gêneros e veja as três tabelas.
  `movie_genres` é a tabela intermediária da relação N:N (um filme, vários gêneros).
- `rooms` e `seats`: a sala e seus assentos. Rode no **Query Tool**:
  ```sql
  SELECT r.name, count(s.id) AS capacidade
  FROM rooms r JOIN seats s ON s.room_id = r.id
  GROUP BY r.name;
  ```
- `showtimes`: repare que `starts_at` e `ends_at` são `timestamptz` (guardados em UTC). O
  pgAdmin mostra no fuso da sua conexão.

---

## Resumo dos endpoints da Etapa 3

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/admin/rooms` | ADMIN |
| DELETE | `/api/admin/rooms/{id}` | ADMIN |
| GET | `/api/rooms`, `/api/rooms/{id}` | logado |
| POST/PUT/DELETE | `/api/admin/movies`, `/api/admin/movies/{id}` | ADMIN |
| POST | `/api/admin/movies/{id}/poster` | ADMIN |
| GET | `/api/movies`, `/api/movies/{id}` | público |
| POST/DELETE | `/api/admin/showtimes`, `/api/admin/showtimes/{id}` | ADMIN |
| GET | `/api/showtimes?date=...`, `/api/showtimes/{id}` | público |
| GET | `/api/showtimes/{id}/seats` | público |
| GET | `/api/movies/{id}/showtimes` | público |
| GET | `/files/**` (pôsteres) | público |
