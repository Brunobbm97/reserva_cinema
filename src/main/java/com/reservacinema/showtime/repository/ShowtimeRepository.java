package com.reservacinema.showtime.repository;

import com.reservacinema.showtime.entity.Showtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // Sessões que começam dentro de um intervalo [from, to), ordenadas por horário.
    // Usado para "sessões de um dia".
    List<Showtime> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(Instant from, Instant to);

    List<Showtime> findByMovieIdOrderByStartsAt(Long movieId);

    /**
     * Conta sessões na MESMA sala cujo intervalo [startsAt, endsAt) encosta no intervalo
     * da nova sessão. Dois intervalos se sobrepõem quando cada um começa antes de o outro acabar.
     * Usado para impedir duas sessões ao mesmo tempo na mesma sala.
     */
    @Query("""
            select count(s) from Showtime s
            where s.room.id = :roomId
              and s.startsAt < :newEnds
              and :newStarts < s.endsAt""")
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("newStarts") Instant newStarts,
                          @Param("newEnds") Instant newEnds);
}
