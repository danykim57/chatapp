package com.chatapp.repository;

import com.chatapp.model.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    List<ChatMessageEntity> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);

    @Query("SELECT m FROM ChatMessageEntity m WHERE m.roomId = :roomId AND m.timestamp < :beforeTimestamp ORDER BY m.timestamp DESC")
    List<ChatMessageEntity> findByRoomIdAndTimestampBefore(String roomId, Instant beforeTimestamp, Pageable pageable);

    @Query("SELECT m FROM ChatMessageEntity m WHERE m.roomId = :roomId ORDER BY m.timestamp DESC")
    List<ChatMessageEntity> findLatestMessageByRoomId(String roomId, Pageable pageable);
}
