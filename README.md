# Reserva Cinema

Backend de um sistema de reserva de ingressos de cinema, feito com Java 21, Spring Boot,
PostgreSQL e JPA/Hibernate. Baseado no desafio
[Movie Reservation System](https://roadmap.sh/projects/movie-reservation-system) do roadmap.sh.

As decisões de arquitetura estão registradas em [docs/decisoes.md](docs/decisoes.md).
Para rodar e entender cada etapa, veja os guias: [Etapa 1](docs/guia-etapa1.md).

## Etapas

- [x] 1. Estrutura do projeto e modelo de dados
- [ ] 2. Autenticação (JWT com access + refresh token) e papéis admin/usuário
- [ ] 3. Filmes (com upload de pôster), salas e sessões
- [ ] 4. Reservas de assentos com proteção contra overbooking
- [ ] 5. Pagamento simulado
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
