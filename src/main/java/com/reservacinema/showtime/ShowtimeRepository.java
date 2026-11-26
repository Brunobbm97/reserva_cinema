package com.reservacinema.showtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // O Spring Data gera a consulta a partir do nome do método:
    // sessões que começam entre "from" (inclusive) e "to" (exclusive), ordenadas por horário.
    List<Showtime> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(Instant from, Instant to);
}
