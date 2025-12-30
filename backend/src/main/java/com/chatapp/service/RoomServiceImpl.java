package com.chatapp.service;

import com.chatapp.grpc.auth.User;
import com.chatapp.grpc.room.*;
import com.chatapp.model.ChatMessageEntity;
import com.chatapp.model.RoomEntity;
import com.chatapp.model.RoomMemberEntity;
import com.chatapp.model.UserEntity;
import com.chatapp.repository.ChatMessageRepository;
import com.chatapp.repository.RoomMemberRepository;
import com.chatapp.repository.RoomRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RoomServiceImpl extends RoomServiceGrpc.RoomServiceImplBase {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final InputValidator inputValidator;

    @Override
    public void getRooms(GetRoomsRequest request, StreamObserver<GetRoomsResponse> responseObserver) {
        try {
            // Validate token
            if (!jwtUtil.validateToken(request.getToken())) {
                sendErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            List<RoomEntity> rooms;
            String searchQuery = request.getSearchQuery();

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                rooms = roomRepository.findByRoomNameContainingIgnoreCaseAndActiveTrue(searchQuery.trim());
            } else {
                rooms = roomRepository.findByActiveTrue();
            }

            List<Room> protoRooms = rooms.stream()
                .map(this::convertToProtoRoom)
                .collect(Collectors.toList());

            GetRoomsResponse response = GetRoomsResponse.newBuilder()
                .setSuccess(true)
                .addAllRooms(protoRooms)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting rooms", e);
            sendErrorResponse(responseObserver, "Failed to retrieve rooms");
        }
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> responseObserver) {
        try {
            // Validate token
            if (!jwtUtil.validateToken(request.getToken())) {
                sendCreateRoomErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String roomName = request.getRoomName();
            String description = request.getDescription();

            // Validate input
            if (!inputValidator.isValidRoomName(roomName)) {
                sendCreateRoomErrorResponse(responseObserver, "Invalid room name (must be 3-100 characters)");
                return;
            }

            // Create room
            RoomEntity room = new RoomEntity();
            room.setRoomName(roomName.trim());
            room.setDescription(description != null ? description.trim() : "");
            room.setCreatedAt(Instant.now());
            room.setActive(true);

            room = roomRepository.save(room);

            CreateRoomResponse response = CreateRoomResponse.newBuilder()
                .setSuccess(true)
                .setRoom(convertToProtoRoom(room))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Room created: {} (ID: {})", roomName, room.getRoomId());

        } catch (Exception e) {
            log.error("Error creating room", e);
            sendCreateRoomErrorResponse(responseObserver, "Failed to create room");
        }
    }

    @Override
    public void joinRoom(JoinRoomRequest request, StreamObserver<JoinRoomResponse> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                sendJoinRoomErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();

            // Check if room exists
            RoomEntity room = roomRepository.findById(roomId).orElse(null);
            if (room == null || !room.isActive()) {
                sendJoinRoomErrorResponse(responseObserver, "Room not found");
                return;
            }

            // Check if user is already in the room
            RoomMemberEntity existingMember = roomMemberRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElse(null);

            if (existingMember != null && existingMember.isActive()) {
                // User is already in the room
                JoinRoomResponse response = JoinRoomResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Already in room")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Add user to room
            if (existingMember != null) {
                existingMember.setActive(true);
                existingMember.setJoinedAt(Instant.now());
                roomMemberRepository.save(existingMember);
            } else {
                RoomMemberEntity newMember = new RoomMemberEntity();
                newMember.setRoomId(roomId);
                newMember.setUserId(userId);
                newMember.setJoinedAt(Instant.now());
                newMember.setActive(true);
                roomMemberRepository.save(newMember);
            }

            JoinRoomResponse response = JoinRoomResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Joined room successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User {} joined room {}", userId, roomId);

        } catch (Exception e) {
            log.error("Error joining room", e);
            sendJoinRoomErrorResponse(responseObserver, "Failed to join room");
        }
    }

    @Override
    public void leaveRoom(LeaveRoomRequest request, StreamObserver<LeaveRoomResponse> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                sendLeaveRoomErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();

            // Find room membership
            RoomMemberEntity member = roomMemberRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElse(null);

            if (member != null && member.isActive()) {
                member.setActive(false);
                roomMemberRepository.save(member);
            }

            LeaveRoomResponse response = LeaveRoomResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Left room successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User {} left room {}", userId, roomId);

        } catch (Exception e) {
            log.error("Error leaving room", e);
            sendLeaveRoomErrorResponse(responseObserver, "Failed to leave room");
        }
    }

    @Override
    public void getRoomUsers(GetRoomUsersRequest request, StreamObserver<GetRoomUsersResponse> responseObserver) {
        try {
            // Validate token
            if (!jwtUtil.validateToken(request.getToken())) {
                sendGetRoomUsersErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String roomId = request.getRoomId();

            // Get room members
            List<RoomMemberEntity> members = roomMemberRepository.findByRoomIdAndActiveTrue(roomId);

            List<User> users = members.stream()
                .map(member -> userRepository.findById(member.getUserId()).orElse(null))
                .filter(user -> user != null && user.isActive())
                .map(this::convertToProtoUser)
                .collect(Collectors.toList());

            GetRoomUsersResponse response = GetRoomUsersResponse.newBuilder()
                .setSuccess(true)
                .addAllUsers(users)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting room users", e);
            sendGetRoomUsersErrorResponse(responseObserver, "Failed to get room users");
        }
    }

    private Room convertToProtoRoom(RoomEntity entity) {
        // Get active user count
        long activeUsers = roomMemberRepository.countActiveUsersByRoomId(entity.getRoomId());

        // Get last message
        List<ChatMessageEntity> lastMessages = chatMessageRepository
            .findLatestMessageByRoomId(entity.getRoomId(), PageRequest.of(0, 1));

        Room.Builder builder = Room.newBuilder()
            .setRoomId(entity.getRoomId())
            .setRoomName(entity.getRoomName())
            .setDescription(entity.getDescription())
            .setActiveUsers((int) activeUsers)
            .setCreatedAt(entity.getCreatedAt().toEpochMilli());

        if (!lastMessages.isEmpty()) {
            ChatMessageEntity lastMessage = lastMessages.get(0);
            builder.setLastMessage(lastMessage.getContent());
            builder.setLastMessageTime(lastMessage.getTimestamp().toEpochMilli());
        }

        return builder.build();
    }

    private User convertToProtoUser(UserEntity entity) {
        return User.newBuilder()
            .setUserId(entity.getUserId())
            .setUsername(entity.getUsername())
            .setUserType(com.chatapp.grpc.auth.UserType.valueOf(entity.getUserType().name()))
            .setCreatedAt(entity.getCreatedAt().toEpochMilli())
            .build();
    }

    private void sendErrorResponse(StreamObserver<GetRoomsResponse> responseObserver, String errorMessage) {
        GetRoomsResponse response = GetRoomsResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendCreateRoomErrorResponse(StreamObserver<CreateRoomResponse> responseObserver, String errorMessage) {
        CreateRoomResponse response = CreateRoomResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendJoinRoomErrorResponse(StreamObserver<JoinRoomResponse> responseObserver, String errorMessage) {
        JoinRoomResponse response = JoinRoomResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendLeaveRoomErrorResponse(StreamObserver<LeaveRoomResponse> responseObserver, String errorMessage) {
        LeaveRoomResponse response = LeaveRoomResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendGetRoomUsersErrorResponse(StreamObserver<GetRoomUsersResponse> responseObserver, String errorMessage) {
        GetRoomUsersResponse response = GetRoomUsersResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
