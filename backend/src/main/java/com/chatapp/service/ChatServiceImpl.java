package com.chatapp.service;

import com.chatapp.grpc.auth.User;
import com.chatapp.grpc.auth.UserType;
import com.chatapp.grpc.chat.*;
import com.chatapp.model.ChatMessageEntity;
import com.chatapp.model.UserEntity;
import com.chatapp.repository.ChatMessageRepository;
import com.chatapp.repository.RoomMemberRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

    private final ChatMessageRepository chatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final InputValidator inputValidator;

    // Store active stream observers for each room
    private final Map<String, List<StreamObserver<MessageEvent>>> roomStreams = new ConcurrentHashMap<>();

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                sendErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();
            String content = request.getContent();

            // Validate message content
            if (!inputValidator.isValidMessage(content)) {
                sendErrorResponse(responseObserver, "Invalid message (empty or too long)");
                return;
            }

            // Sanitize message content
            String sanitizedContent = inputValidator.sanitizeMessage(content);

            // Check if user is a member of the room
            if (!roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(roomId, userId)) {
                sendErrorResponse(responseObserver, "You are not a member of this room");
                return;
            }

            // Get user info
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                sendErrorResponse(responseObserver, "User not found");
                return;
            }

            // Create and save message
            ChatMessageEntity message = new ChatMessageEntity();
            message.setRoomId(roomId);
            message.setSenderId(userId);
            message.setSenderUsername(user.getUsername());
            message.setContent(sanitizedContent);
            message.setMessageType(ChatMessageEntity.MessageType.TEXT);
            message.setTimestamp(Instant.now());

            message = chatMessageRepository.save(message);

            // Build response
            ChatMessage protoMessage = convertToProtoChatMessage(message, user);
            SendMessageResponse response = SendMessageResponse.newBuilder()
                .setSuccess(true)
                .setMessage(protoMessage)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // Broadcast message to all connected clients in the room
            broadcastMessage(roomId, protoMessage);

            log.debug("Message sent by {} in room {}", user.getUsername(), roomId);

        } catch (Exception e) {
            log.error("Error sending message", e);
            sendErrorResponse(responseObserver, "Failed to send message");
        }
    }

    @Override
    public void streamMessages(StreamMessagesRequest request, StreamObserver<MessageEvent> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid token for stream messages");
                responseObserver.onCompleted();
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();

            // Check if user is a member of the room
            if (!roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(roomId, userId)) {
                log.warn("User {} attempted to stream from room {} without membership", userId, roomId);
                responseObserver.onCompleted();
                return;
            }

            // Add this observer to the room's stream list
            roomStreams.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(responseObserver);

            log.info("User {} started streaming messages from room {}", userId, roomId);

            // Send a welcome system message (optional)
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                broadcastUserEvent(roomId, user, UserEventType.JOINED);
            }

        } catch (Exception e) {
            log.error("Error setting up message stream", e);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getMessageHistory(GetMessageHistoryRequest request, StreamObserver<GetMessageHistoryResponse> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                sendHistoryErrorResponse(responseObserver, "Invalid or expired token");
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();
            int limit = request.getLimit() > 0 ? Math.min(request.getLimit(), 100) : 50;

            // Check if user is a member of the room
            if (!roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(roomId, userId)) {
                sendHistoryErrorResponse(responseObserver, "You are not a member of this room");
                return;
            }

            // Get messages
            List<ChatMessageEntity> messages;
            if (request.getBeforeTimestamp() > 0) {
                Instant beforeTime = Instant.ofEpochMilli(request.getBeforeTimestamp());
                messages = chatMessageRepository.findByRoomIdAndTimestampBefore(
                    roomId, beforeTime, PageRequest.of(0, limit)
                );
            } else {
                messages = chatMessageRepository.findByRoomIdOrderByTimestampDesc(
                    roomId, PageRequest.of(0, limit)
                );
            }

            // Convert to proto messages
            List<ChatMessage> protoMessages = messages.stream()
                .map(msg -> {
                    UserEntity sender = userRepository.findById(msg.getSenderId()).orElse(null);
                    return convertToProtoChatMessage(msg, sender);
                })
                .collect(Collectors.toList());

            GetMessageHistoryResponse response = GetMessageHistoryResponse.newBuilder()
                .setSuccess(true)
                .addAllMessages(protoMessages)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting message history", e);
            sendHistoryErrorResponse(responseObserver, "Failed to get message history");
        }
    }

    @Override
    public void sendTypingIndicator(TypingIndicatorRequest request, StreamObserver<TypingIndicatorResponse> responseObserver) {
        try {
            // Validate token
            String token = request.getToken();
            if (!jwtUtil.validateToken(token)) {
                TypingIndicatorResponse response = TypingIndicatorResponse.newBuilder()
                    .setSuccess(false)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            String roomId = request.getRoomId();
            boolean isTyping = request.getIsTyping();

            // Get user info
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Broadcast typing indicator
                broadcastTypingIndicator(roomId, user, isTyping);
            }

            TypingIndicatorResponse response = TypingIndicatorResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error sending typing indicator", e);
            TypingIndicatorResponse response = TypingIndicatorResponse.newBuilder()
                .setSuccess(false)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Helper methods for broadcasting

    private void broadcastMessage(String roomId, ChatMessage message) {
        List<StreamObserver<MessageEvent>> observers = roomStreams.get(roomId);
        if (observers != null) {
            MessageEvent event = MessageEvent.newBuilder()
                .setEventType(MessageEventType.NEW_MESSAGE)
                .setMessage(message)
                .build();

            observers.removeIf(observer -> {
                try {
                    observer.onNext(event);
                    return false;
                } catch (Exception e) {
                    log.warn("Failed to send message to observer, removing", e);
                    return true;
                }
            });
        }
    }

    private void broadcastUserEvent(String roomId, UserEntity user, UserEventType eventType) {
        List<StreamObserver<MessageEvent>> observers = roomStreams.get(roomId);
        if (observers != null) {
            UserEvent userEvent = UserEvent.newBuilder()
                .setUser(convertToProtoUser(user))
                .setRoomId(roomId)
                .setEventType(eventType)
                .build();

            MessageEvent event = MessageEvent.newBuilder()
                .setEventType(eventType == UserEventType.JOINED ?
                    MessageEventType.USER_JOINED : MessageEventType.USER_LEFT)
                .setUserEvent(userEvent)
                .build();

            observers.removeIf(observer -> {
                try {
                    observer.onNext(event);
                    return false;
                } catch (Exception e) {
                    log.warn("Failed to send user event to observer, removing", e);
                    return true;
                }
            });
        }
    }

    private void broadcastTypingIndicator(String roomId, UserEntity user, boolean isTyping) {
        List<StreamObserver<MessageEvent>> observers = roomStreams.get(roomId);
        if (observers != null) {
            TypingEvent typingEvent = TypingEvent.newBuilder()
                .setUser(convertToProtoUser(user))
                .setRoomId(roomId)
                .setIsTyping(isTyping)
                .build();

            MessageEvent event = MessageEvent.newBuilder()
                .setEventType(MessageEventType.TYPING_INDICATOR)
                .setTypingEvent(typingEvent)
                .build();

            observers.removeIf(observer -> {
                try {
                    observer.onNext(event);
                    return false;
                } catch (Exception e) {
                    log.warn("Failed to send typing indicator to observer, removing", e);
                    return true;
                }
            });
        }
    }

    private ChatMessage convertToProtoChatMessage(ChatMessageEntity entity, UserEntity sender) {
        ChatMessage.Builder builder = ChatMessage.newBuilder()
            .setMessageId(entity.getMessageId())
            .setRoomId(entity.getRoomId())
            .setContent(entity.getContent())
            .setTimestamp(entity.getTimestamp().toEpochMilli())
            .setMessageType(MessageType.valueOf(entity.getMessageType().name()));

        if (sender != null) {
            builder.setSender(convertToProtoUser(sender));
        } else {
            // Fallback if sender not found
            builder.setSender(User.newBuilder()
                .setUserId(entity.getSenderId())
                .setUsername(entity.getSenderUsername())
                .setUserType(UserType.REGISTERED)
                .build());
        }

        return builder.build();
    }

    private User convertToProtoUser(UserEntity entity) {
        return User.newBuilder()
            .setUserId(entity.getUserId())
            .setUsername(entity.getUsername())
            .setUserType(UserType.valueOf(entity.getUserType().name()))
            .setCreatedAt(entity.getCreatedAt().toEpochMilli())
            .build();
    }

    private void sendErrorResponse(StreamObserver<SendMessageResponse> responseObserver, String errorMessage) {
        SendMessageResponse response = SendMessageResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendHistoryErrorResponse(StreamObserver<GetMessageHistoryResponse> responseObserver, String errorMessage) {
        GetMessageHistoryResponse response = GetMessageHistoryResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
