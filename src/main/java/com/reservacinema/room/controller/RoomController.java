package com.reservacinema.room.controller;

import com.reservacinema.room.dto.CreateRoomRequest;
import com.reservacinema.room.dto.RoomResponse;
import com.reservacinema.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ---- Admin (protegido por /api/admin/** no SecurityConfig) ----

    @PostMapping("/api/admin/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(request);
    }

    @DeleteMapping("/api/admin/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }

    // ---- Leitura: qualquer usuário logado ----

    @GetMapping("/api/rooms")
    public List<RoomResponse> list() {
        return roomService.listAll();
    }

    @GetMapping("/api/rooms/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return roomService.getById(id);
    }
}
