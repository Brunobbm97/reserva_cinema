package com.reservacinema.showtime.service;

import com.reservacinema.common.exception.ConflictException;
import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.movie.entity.Movie;
import com.reservacinema.movie.repository.MovieRepository;
import com.reservacinema.reservation.repository.ReservationSeatRepository;
import com.reservacinema.room.entity.Room;
import com.reservacinema.room.entity.Seat;
import com.reservacinema.room.repository.RoomRepository;
import com.reservacinema.showtime.dto.SeatAvailabilityResponse;
import com.reservacinema.showtime.dto.SeatAvailabilityResponse.SeatStatus;
import com.reservacinema.showtime.dto.ShowtimeRequest;
import com.reservacinema.showtime.dto.ShowtimeResponse;
import com.reservacinema.showtime.entity.Showtime;
import com.reservacinema.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {

    // Dia interpretado no fuso de Brasília (uma sessão "do dia 1º" é no horário local).
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepository,
                               MovieRepository movieRepository,
                               RoomRepository roomRepository,
                               ReservationSeatRepository reservationSeatRepository) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.reservationSeatRepository = reservationSeatRepository;
    }

    @Override
    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new NotFoundException("Filme " + request.movieId() + " não encontrado"));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException("Sala " + request.roomId() + " não encontrada"));

        // O construtor calcula endsAt = startsAt + duração do filme.
        Showtime showtime = new Showtime(movie, room, request.startsAt(), request.price());

        // Regra de negócio: a mesma sala não pode ter duas sessões sobrepostas no tempo.
        if (showtimeRepository.countOverlapping(room.getId(), showtime.getStartsAt(), showtime.getEndsAt()) > 0) {
            throw new ConflictException("A sala já tem uma sessão nesse horário");
        }

        showtimeRepository.save(showtime);
        return ShowtimeResponse.from(showtime);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Showtime showtime = findShowtime(id);
        try {
            showtimeRepository.delete(showtime);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Não é possível excluir: a sessão já tem reservas");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getById(Long id) {
        return ShowtimeResponse.from(findShowtime(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> listByDate(LocalDate date) {
        List<Showtime> showtimes;
        if (date == null) {
            // Sem data: todas as sessões a partir de agora.
            showtimes = showtimeRepository
                    .findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(
                            Instant.now(), Instant.now().plus(java.time.Duration.ofDays(3650)));
        } else {
            // O dia local vira um intervalo em UTC: [00:00 do dia, 00:00 do dia seguinte).
            Instant from = date.atStartOfDay(ZONE).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
            showtimes = showtimeRepository
                    .findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(from, to);
        }
        return showtimes.stream().map(ShowtimeResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> listByMovie(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Filme " + movieId + " não encontrado");
        }
        return showtimeRepository.findByMovieIdOrderByStartsAt(movieId).stream()
                .map(ShowtimeResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilityResponse seatAvailability(Long showtimeId) {
        Showtime showtime = findShowtime(showtimeId);

        // Ids dos assentos já ocupados (reserva ativa) nesta sessão.
        Set<Long> takenSeatIds = reservationSeatRepository.findByShowtimeIdAndActiveTrue(showtimeId).stream()
                .map(rs -> rs.getSeat().getId())
                .collect(Collectors.toSet());

        List<Seat> allSeats = showtime.getRoom().getSeats();
        List<SeatStatus> statuses = allSeats.stream()
                .map(seat -> new SeatStatus(
                        seat.getId(), seat.getLabel(), seat.getRowLabel(), seat.getSeatNumber(),
                        !takenSeatIds.contains(seat.getId())))
                .toList();

        int available = allSeats.size() - takenSeatIds.size();
        return new SeatAvailabilityResponse(showtimeId, allSeats.size(), available, statuses);
    }

    private Showtime findShowtime(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sessão " + id + " não encontrada"));
    }
}
