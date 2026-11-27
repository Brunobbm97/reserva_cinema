# Reserva Cinema

Backend de um sistema de reserva de ingressos de cinema, feito com Java 21, Spring Boot,
PostgreSQL e JPA/Hibernate. Baseado no desafio
[Movie Reservation System](https://roadmap.sh/projects/movie-reservation-system) do roadmap.sh.

As decisões de arquitetura estão registradas em [docs/decisoes.md](docs/decisoes.md).
Para rodar e entender cada etapa, veja os guias: [Etapa 1](docs/guia-etapa1.md), [Etapa 2](docs/guia-etapa2.md), [Etapa 3](docs/guia-etapa3.md), [Etapa 4](docs/guia-etapa4.md), [Etapa 5](docs/guia-etapa5.md).

## Etapas

- [x] 1. Estrutura do projeto e modelo de dados
- [x] 2. Autenticação (JWT com access + refresh token) e papéis admin/usuário
- [x] 3. Filmes (com upload de pôster), salas e sessões
- [x] 4. Reservas de assentos com proteção contra overbooking
- [x] 5. Pagamento simulado
- [ ] 6. Relatórios para o admin

## Como rodar localmente

Pré-requisitos: Java 21, Maven e PostgreSQL.

1. Crie o banco e o usuário no PostgreSQL:

   ```sql
   CREATE USER cinema WITH PASSWORD 'cinema';
   CREATE DATABASE reserva_cinema OWNER cinema;
   ```

2. Suba a aplicação:

   ```bash
   mvn spring-boot:run
   ```

   O Flyway cria as tabelas automaticamente na primeira execução.

Para usar outro banco, defina as variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## Endpoints disponíveis

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| POST | `/api/auth/register` | público | Cria conta (USER) e devolve tokens |
| POST | `/api/auth/login` | público | Login, devolve access + refresh token |
| POST | `/api/auth/refresh` | público | Troca o refresh token por um par novo |
| POST | `/api/auth/logout` | público | Revoga o refresh token |
| GET | `/api/users/me` | logado | Dados do usuário logado |
| GET | `/api/admin/users` | ADMIN | Lista usuários |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Promove ou rebaixa um usuário |

| POST/DELETE | `/api/admin/rooms`, `/api/admin/rooms/{id}` | ADMIN | Cria/remove sala com mapa de assentos |
| GET | `/api/rooms`, `/api/rooms/{id}` | logado | Lista salas / vê o mapa |
| POST/PUT/DELETE | `/api/admin/movies`... | ADMIN | CRUD de filmes |
| POST | `/api/admin/movies/{id}/poster` | ADMIN | Upload do pôster |
| GET | `/api/movies`, `/api/movies/{id}` | público | Catálogo de filmes |
| POST/DELETE | `/api/admin/showtimes`... | ADMIN | Cria/remove sessão |
| GET | `/api/showtimes?date=...`, `/api/showtimes/{id}` | público | Sessões (por dia) |
| GET | `/api/showtimes/{id}/seats` | público | Mapa de assentos livres/ocupados |
| GET | `/api/movies/{id}/showtimes` | público | Sessões de um filme |

| POST | `/api/reservations` | logado | Reserva assentos (409 se já ocupado) |
| GET | `/api/reservations`, `/api/reservations/{id}` | logado (dono) | Minhas reservas |
| POST | `/api/reservations/{id}/cancel` | logado (dono) | Cancela reserva futura |

| POST | `/api/reservations/{id}/payment` | logado (dono) | Pagamento simulado (confirma a reserva) |
| GET | `/api/reservations/{id}/payments` | logado (dono) | Histórico de pagamentos |

Admin inicial em desenvolvimento: `admin@cinema.com` / `admin123` (altere com `ADMIN_EMAIL`
e `ADMIN_PASSWORD`). Em produção, defina também `JWT_SECRET`.

## Organização das pastas

```
src/main/java/com/reservacinema/
├── auth/
│   ├── controller/    AuthController
│   ├── dto/           RegisterRequest, LoginRequest, RefreshRequest, TokenResponse
│   ├── entity/        RefreshToken
│   ├── repository/    RefreshTokenRepository
│   ├── security/      JwtAuthenticationFilter, AuthenticatedUser, JwtProperties
│   └── service/       AuthService(+Impl), JwtService(+Impl)
├── user/
│   ├── controller/    UserController
│   ├── dto/           UserResponse, ChangeRoleRequest
│   ├── entity/        User, Role
│   ├── repository/    UserRepository
│   └── service/       UserService(+Impl)
├── movie/
│   ├── entity/        Movie, Genre
│   └── repository/    MovieRepository, GenreRepository
├── room/              (mesma estrutura: entity/, repository/)
├── showtime/           entity/, repository/, service/, controller/, dto/
├── reservation/
├── config/          SecurityConfig, AdminSeeder
└── common/exception/ ApiException e filhas, GlobalExceptionHandler
```

Cada funcionalidade ganha `service/` (interface + `Impl`), `controller/` e `dto/` conforme as
etapas avançam. Veja as decisões D08 e D09.

## Modelo de dados

```
users ──< reservations >── showtimes >── movies >──< genres
              │                 │
              └──< reservation_seats >── seats >── rooms
```

- **rooms / seats:** cada sala tem seu mapa de assentos (fileira + número).
- **showtimes:** um filme em uma sala, num horário, com preço fixo.
- **reservations / reservation_seats:** a reserva de um usuário e os assentos dela. Um índice
  único parcial garante que um assento não seja reservado duas vezes na mesma sessão.
