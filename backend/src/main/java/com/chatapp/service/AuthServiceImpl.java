package com.chatapp.service;

import com.chatapp.grpc.auth.*;
import com.chatapp.model.UserEntity;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final InputValidator inputValidator;

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();

            // Validate input
            if (!inputValidator.isValidUsername(username)) {
                sendErrorResponse(responseObserver, "Invalid username format");
                return;
            }

            // Find user
            UserEntity user = userRepository.findByUsername(username).orElse(null);

            if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                // Use same error message to prevent username enumeration
                sendErrorResponse(responseObserver, "Invalid username or password");
                return;
            }

            if (!user.isActive()) {
                sendErrorResponse(responseObserver, "Account is inactive");
                return;
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUserType().name(),
                user.getUsername()
            );

            // Build response
            LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setToken(token)
                .setUser(convertToProtoUser(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User logged in: {}", username);

        } catch (Exception e) {
            log.error("Error during login", e);
            sendErrorResponse(responseObserver, "Login failed");
        }
    }

    @Override
    public void guestLogin(GuestLoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            String guestName = request.getGuestName();

            // Generate guest name if not provided or invalid
            if (guestName == null || guestName.trim().isEmpty()) {
                guestName = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
            } else {
                guestName = guestName.trim();
                // Validate guest name
                if (!inputValidator.isValidUsername(guestName)) {
                    guestName = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
                }
            }

            // Check if username already exists
            int attempts = 0;
            String finalGuestName = guestName;
            while (userRepository.existsByUsername(finalGuestName) && attempts < 5) {
                finalGuestName = guestName + "_" + UUID.randomUUID().toString().substring(0, 4);
                attempts++;
            }

            if (attempts >= 5) {
                sendErrorResponse(responseObserver, "Unable to generate unique guest name");
                return;
            }

            // Create guest user
            UserEntity guestUser = new UserEntity();
            guestUser.setUsername(finalGuestName);
            guestUser.setUserType(UserEntity.UserType.GUEST);
            guestUser.setCreatedAt(Instant.now());
            guestUser.setActive(true);

            guestUser = userRepository.save(guestUser);

            // Generate JWT token with shorter expiration for guests
            String token = jwtUtil.generateToken(
                guestUser.getUserId(),
                UserType.GUEST.name(),
                guestUser.getUsername()
            );

            // Build response
            LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setToken(token)
                .setUser(convertToProtoUser(guestUser))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Guest user created: {}", finalGuestName);

        } catch (Exception e) {
            log.error("Error during guest login", e);
            sendErrorResponse(responseObserver, "Guest login failed");
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            // In a real application, you might want to blacklist the token
            // For now, we just return success
            LogoutResponse response = LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Logged out successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error during logout", e);
            LogoutResponse response = LogoutResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Logout failed")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            String token = request.getToken();

            if (!jwtUtil.validateToken(token)) {
                ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .setErrorMessage("Invalid or expired token")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            String userId = jwtUtil.extractUserId(token);
            UserEntity user = userRepository.findById(userId).orElse(null);

            if (user == null || !user.isActive()) {
                ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .setErrorMessage("User not found or inactive")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUser(convertToProtoUser(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error validating token", e);
            ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(false)
                .setErrorMessage("Token validation failed")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();
            String email = request.getEmail();

            // Validate input
            if (!inputValidator.isValidUsername(username)) {
                sendRegisterErrorResponse(responseObserver, "Invalid username format");
                return;
            }

            if (!inputValidator.isValidPassword(password)) {
                sendRegisterErrorResponse(responseObserver, "Password must be at least 8 characters");
                return;
            }

            if (email != null && !email.isEmpty() && !inputValidator.isValidEmail(email)) {
                sendRegisterErrorResponse(responseObserver, "Invalid email format");
                return;
            }

            // Check if username already exists
            if (userRepository.existsByUsername(username)) {
                sendRegisterErrorResponse(responseObserver, "Username already exists");
                return;
            }

            // Create new user
            UserEntity newUser = new UserEntity();
            newUser.setUsername(username);
            newUser.setPasswordHash(passwordEncoder.encode(password));
            newUser.setEmail(email);
            newUser.setUserType(UserEntity.UserType.REGISTERED);
            newUser.setCreatedAt(Instant.now());
            newUser.setActive(true);

            userRepository.save(newUser);

            RegisterResponse response = RegisterResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Registration successful")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User registered: {}", username);

        } catch (Exception e) {
            log.error("Error during registration", e);
            sendRegisterErrorResponse(responseObserver, "Registration failed");
        }
    }

    private User convertToProtoUser(UserEntity entity) {
        return User.newBuilder()
            .setUserId(entity.getUserId())
            .setUsername(entity.getUsername())
            .setUserType(UserType.valueOf(entity.getUserType().name()))
            .setCreatedAt(entity.getCreatedAt().toEpochMilli())
            .build();
    }

    private void sendErrorResponse(StreamObserver<LoginResponse> responseObserver, String errorMessage) {
        LoginResponse response = LoginResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendRegisterErrorResponse(StreamObserver<RegisterResponse> responseObserver, String errorMessage) {
        RegisterResponse response = RegisterResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage(errorMessage)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
