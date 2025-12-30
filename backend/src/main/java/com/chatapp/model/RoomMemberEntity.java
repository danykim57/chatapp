package com.chatapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"roomId", "userId"}),
    indexes = {
        @Index(name = "idx_room_members_room", columnList = "roomId"),
        @Index(name = "idx_room_members_user", columnList = "userId")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;
}
