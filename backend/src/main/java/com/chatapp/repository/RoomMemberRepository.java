package com.chatapp.repository;

import com.chatapp.model.RoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, String> {

    Optional<RoomMemberEntity> findByRoomIdAndUserId(String roomId, String userId);

    List<RoomMemberEntity> findByRoomIdAndActiveTrue(String roomId);

    List<RoomMemberEntity> findByUserIdAndActiveTrue(String userId);

    @Query("SELECT COUNT(rm) FROM RoomMemberEntity rm WHERE rm.roomId = :roomId AND rm.active = true")
    long countActiveUsersByRoomId(String roomId);

    boolean existsByRoomIdAndUserIdAndActiveTrue(String roomId, String userId);
}
