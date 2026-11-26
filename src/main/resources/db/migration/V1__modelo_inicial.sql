-- =========================================================
-- V1: modelo de dados inicial do sistema de reserva de cinema
-- =========================================================

-- Usuários do sistema. O papel (role) define o que cada um pode fazer.
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Gêneros de filme (Ação, Comédia, Drama...).
CREATE TABLE genres (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE movies (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    duration_minutes INT          NOT NULL CHECK (duration_minutes > 0),
    poster_path      VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Um filme pode ter vários gêneros e um gênero pode estar em vários filmes (N:N).
CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id),
    PRIMARY KEY (movie_id, genre_id)
);

-- Salas do cinema.
CREATE TABLE rooms (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

-- Cada sala tem seu próprio mapa de assentos (fileira + número), ex.: A1, A2, B1...
CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    row_label   VARCHAR(5)  NOT NULL,
    seat_number INT         NOT NULL CHECK (seat_number > 0),
    CONSTRAINT uq_seat_position UNIQUE (room_id, row_label, seat_number)
);

-- Sessão: um filme passando em uma sala, em um horário, com preço fixo por assento.
CREATE TABLE showtimes (
    id        BIGSERIAL PRIMARY KEY,
    movie_id  BIGINT        NOT NULL REFERENCES movies (id),
    room_id   BIGINT        NOT NULL REFERENCES rooms (id),
    starts_at TIMESTAMPTZ   NOT NULL,
    ends_at   TIMESTAMPTZ   NOT NULL,
    price     NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    CHECK (ends_at > starts_at)
);
CREATE INDEX ix_showtimes_starts_at ON showtimes (starts_at);

-- Reserva: um usuário reserva um ou mais assentos de uma sessão.
CREATE TABLE reservations (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users (id),
    showtime_id  BIGINT         NOT NULL REFERENCES showtimes (id),
    status       VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ
);
CREATE INDEX ix_reservations_user ON reservations (user_id);

-- Cada assento reservado vira uma linha aqui.
-- showtime_id é repetido (já está em reservations) de propósito: assim o banco
-- consegue garantir a regra "um assento só pode ser ocupado uma vez por sessão".
CREATE TABLE reservation_seats (
    id             BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT  NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    showtime_id    BIGINT  NOT NULL REFERENCES showtimes (id),
    seat_id        BIGINT  NOT NULL REFERENCES seats (id),
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

-- PROTEÇÃO CONTRA OVERBOOKING:
-- só pode existir UMA linha ativa para cada (sessão, assento).
-- O índice é "parcial" (WHERE active): quando uma reserva é cancelada,
-- marcamos active = false e o assento volta a ficar livre, sem apagar o histórico.
CREATE UNIQUE INDEX uq_active_seat_per_showtime
    ON reservation_seats (showtime_id, seat_id)
    WHERE active;
