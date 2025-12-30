package com.chatapp.repository;

import com.chatapp.model.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    List<RoomEntity> findByActiveTrue();
    List<RoomEntity> findByRoomNameContainingIgnoreCaseAndActiveTrue(String roomName);
}
