package com.reservacinema.room.service;

import com.reservacinema.common.exception.ConflictException;
import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.room.dto.CreateRoomRequest;
import com.reservacinema.room.dto.RoomResponse;
import com.reservacinema.room.entity.Room;
import com.reservacinema.room.repository.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        Room room = new Room(request.name().trim());

        // Monta o mapa de assentos. Um Set evita cadastrar o mesmo lugar (fileira+número) duas vezes
        // dentro da mesma requisição, antes mesmo de chegar ao banco.
        Set<String> seen = new HashSet<>();
        for (CreateRoomRequest.RowDefinition row : request.rows()) {
            String label = row.label().trim().toUpperCase();
            for (int number = 1; number <= row.seats(); number++) {
                if (!seen.add(label + number)) {
                    throw new ConflictException("Assento repetido no mapa: " + label + number);
                }
                room.addSeat(label, number);
            }
        }

        try {
            roomRepository.save(room);
        } catch (DataIntegrityViolationException e) {
            // A coluna name é UNIQUE no banco: duas salas não podem ter o mesmo nome.
            throw new ConflictException("Já existe uma sala com o nome '" + request.name() + "'");
        }
        return RoomResponse.from(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> listAll() {
        return roomRepository.findAll().stream().map(RoomResponse::summary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getById(Long id) {
        return RoomResponse.from(findRoom(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = findRoom(id);
        try {
            roomRepository.delete(room);
        } catch (DataIntegrityViolationException e) {
            // Se já existem sessões usando a sala, o banco bloqueia a exclusão (chave estrangeira).
            throw new ConflictException("Não é possível excluir: a sala já tem sessões agendadas");
        }
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sala " + id + " não encontrada"));
    }
}
