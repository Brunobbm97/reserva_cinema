package com.reservacinema.reservation.service;

import com.reservacinema.common.exception.BusinessException;
import com.reservacinema.common.exception.ConflictException;
import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.reservation.dto.CreateReservationRequest;
import com.reservacinema.reservation.dto.ReservationResponse;
import com.reservacinema.reservation.entity.Reservation;
import com.reservacinema.reservation.entity.ReservationStatus;
import com.reservacinema.reservation.repository.ReservationRepository;
import com.reservacinema.room.entity.Seat;
import com.reservacinema.room.repository.SeatRepository;
import com.reservacinema.showtime.entity.Showtime;
import com.reservacinema.showtime.repository.ShowtimeRepository;
import com.reservacinema.user.entity.User;
import com.reservacinema.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  ShowtimeRepository showtimeRepository,
                                  SeatRepository seatRepository,
                                  UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest request) {
        Showtime showtime = showtimeRepository.findById(request.showtimeId())
                .orElseThrow(() -> new NotFoundException("Sessão " + request.showtimeId() + " não encontrada"));

        // Não dá para reservar numa sessão que já começou/passou.
        if (!showtime.getStartsAt().isAfter(Instant.now())) {
            throw new BusinessException("Esta sessão já começou; não é possível reservar");
        }

        // O usuário manda ids de assento repetidos? Recusamos antes de ir ao banco.
        List<Long> seatIds = request.seatIds().stream().distinct().toList();
        if (seatIds.size() != request.seatIds().size()) {
            throw new BusinessException("Há assentos repetidos no pedido");
        }

        // Carrega os assentos e confere que todos existem E pertencem à sala desta sessão.
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("Um ou mais assentos não existem");
        }
        Long roomId = showtime.getRoom().getId();
        boolean allInRoom = seats.stream().allMatch(s -> s.getRoom().getId().equals(roomId));
        if (!allInRoom) {
            throw new BusinessException("Um ou mais assentos não pertencem à sala desta sessão");
        }

        User user = userRepository.getReferenceById(userId);
        Reservation reservation = new Reservation(user, showtime);
        seats.forEach(reservation::addSeat);
        // Nasce PENDING_PAYMENT: o assento já fica segurado, e a confirmação vem com o
        // pagamento (Etapa 5). Mesmo pendente, o assento conta como ocupado.
        try {
            // Com ids IDENTITY, o save já dispara os INSERTs dos assentos; o flush garante que
            // tudo seja enviado ao banco AQUI, dentro do try. Se outra pessoa já reservou o mesmo
            // assento, o índice único parcial do banco recusa e caímos no catch.
            // Sem isso, o erro só apareceria no commit, fora do nosso tratamento (virando 500).
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Um ou mais assentos acabaram de ser reservados por outra pessoa");
        }

        return ReservationResponse.from(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getMine(Long userId, Long reservationId) {
        return ReservationResponse.from(findOwned(userId, reservationId));
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long userId, Long reservationId) {
        Reservation reservation = findOwned(userId, reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Esta reserva já está cancelada");
        }
        // Só dá para cancelar enquanto a sessão não começou.
        if (!reservation.getShowtime().getStartsAt().isAfter(Instant.now())) {
            throw new BusinessException("A sessão já começou; não é possível cancelar");
        }

        reservation.cancel(); // muda o status e libera os assentos (active = false)
        return ReservationResponse.from(reservation);
    }

    /**
     * Busca a reserva garantindo que ela pertence ao usuário logado.
     * Se for de outra pessoa, devolvemos 404 (e não 403) para nem revelar que ela existe.
     * Isso evita o ataque de IDOR (trocar o id na URL para ver a reserva alheia).
     */
    private Reservation findOwned(Long userId, Long reservationId) {
        return reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Reserva " + reservationId + " não encontrada"));
    }
}
