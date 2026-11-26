package com.reservacinema.room.service;

import com.reservacinema.room.dto.CreateRoomRequest;
import com.reservacinema.room.dto.RoomResponse;

import java.util.List;

public interface RoomService {

    RoomResponse create(CreateRoomRequest request);

    List<RoomResponse> listAll();

    RoomResponse getById(Long id);

    void delete(Long id);
}
