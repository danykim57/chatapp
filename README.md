# gRPC-Web Chat Application

A real-time chat application built with Java Spring Boot (gRPC backend), React + TypeScript (frontend), and Envoy proxy for gRPC-Web support.

## Features

### Authentication
- ✅ User login with username/password
- ✅ Guest login (no password required)
- ✅ JWT token-based authentication
- ✅ Session management

### Chat Features
- ✅ Multiple chat rooms
- ✅ Real-time messaging via gRPC streaming
- ✅ Message history
- ✅ User presence (online users list)
- ✅ Typing indicators
- ✅ System notifications (user joined/left)

### Security
- ✅ TLS/SSL encryption (via Envoy proxy)
- ✅ JWT authentication with bcrypt password hashing
- ✅ Input validation and sanitization (XSS prevention)
- ✅ Rate limiting configuration
- ✅ SQL injection prevention (JPA with prepared statements)

## Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   React     │ ◄─────► │    Envoy    │ ◄─────► │  Spring     │
│  Frontend   │  HTTP   │    Proxy    │  gRPC   │  Boot       │
│ (gRPC-Web)  │         │ (gRPC-Web)  │         │  Backend    │
└─────────────┘         └─────────────┘         └─────────────┘
                                                        │
                                                        ▼
                                                  ┌──────────┐
                                                  │   H2     │
                                                  │ Database │
                                                  └──────────┘
```

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0** - Application framework
- **gRPC 1.60.0** - RPC framework
- **Protocol Buffers 3.25** - Serialization
- **H2 Database** - In-memory database (dev)
- **JWT (jjwt 0.12.3)** - Authentication
- **BCrypt** - Password hashing

### Frontend
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **gRPC-Web** - gRPC client for browsers
- **React Router** - Navigation

### Infrastructure
- **Envoy Proxy** - gRPC-Web bridge
- **Docker** - Container for Envoy

## Project Structure

```
chatapp/
├── backend/                    # Spring Boot gRPC server
│   ├── src/main/
│   │   ├── java/com/chatapp/
│   │   │   ├── config/        # Security & configuration
│   │   │   ├── model/         # JPA entities
│   │   │   ├── repository/    # Data access
│   │   │   ├── security/      # JWT utilities
│   │   │   └── service/       # gRPC service implementations
│   │   └── resources/
│   │       └── application.yml
│   └── pom.xml
├── frontend/                   # React application
│   ├── src/
│   │   ├── components/
│   │   ├── pages/             # Login, RoomList, Chat screens
│   │   ├── services/          # gRPC client services
│   │   └── utils/             # Auth storage, formatters
│   ├── package.json
│   └── vite.config.ts
├── proto/                      # Protocol Buffer definitions
│   ├── auth.proto             # Authentication service
│   ├── room.proto             # Room management service
│   └── chat.proto             # Chat messaging service
├── envoy/                      # Envoy proxy configuration
│   ├── envoy.yaml
│   └── Dockerfile
└── docker-compose.yml
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Node.js 18+** and npm
- **Docker** (for Envoy proxy)
- **Protocol Buffer Compiler** (protoc) - Optional for regenerating protos

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd chatapp
```

### 2. Backend Setup

#### Generate Protocol Buffer Classes

```bash
cd backend
mvn clean compile
```

This will automatically generate Java classes from `.proto` files.

#### Run the Backend Server

```bash
mvn spring-boot:run
```

The gRPC server will start on port **9090**.

**Backend Endpoints:**
- gRPC Server: `localhost:9090`
- H2 Console: `http://localhost:8080/h2-console` (if Spring Boot web server is enabled)
  - JDBC URL: `jdbc:h2:mem:chatdb`
  - Username: `sa`
  - Password: (empty)

### 3. Envoy Proxy Setup

Start the Envoy proxy using Docker Compose:

```bash
docker-compose up -d
```

Envoy will start on port **8081** and proxy requests to the backend gRPC server on port **9090**.

**Envoy Endpoints:**
- gRPC-Web Proxy: `localhost:8081`
- Admin Console: `localhost:9901`

### 4. Frontend Setup

#### Install Dependencies

```bash
cd frontend
npm install
```

#### Generate TypeScript gRPC Client (Optional)

If you've modified the `.proto` files, regenerate the TypeScript clients:

```bash
npm run proto:generate
```

> **Note:** You'll need `protoc` and `protoc-gen-grpc-web` installed for this step.

#### Run the Frontend Development Server

```bash
npm run dev
```

The React app will start on **http://localhost:3000**.

## Running the Application

### Start All Services

1. **Terminal 1** - Backend:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Terminal 2** - Envoy Proxy:
   ```bash
   docker-compose up
   ```

3. **Terminal 3** - Frontend:
   ```bash
   cd frontend
   npm run dev
   ```

### Access the Application

Open your browser and navigate to **http://localhost:3000**

## Usage Guide

### 1. Login Screen

**Option 1: Guest Login**
- Click "Continue as Guest"
- Optionally enter a guest name or leave blank for auto-generated name
- Click "Join as Guest"

**Option 2: Regular Login**
- Enter username and password
- Click "Login"

> **Note:** For testing, you can register users or use guest mode.

### 2. Room List Screen

- View available chat rooms
- See active user counts and last messages
- Use search bar to filter rooms
- Click on any room to join

### 3. Chat Screen

- View real-time messages
- Send messages (max 2000 characters)
- See online users in the sidebar
- Toggle user list visibility
- Leave room to return to room list

## Security Features

### Encryption Methods

1. **Transport Layer Security (TLS)**
   - TLS 1.3 for all gRPC-Web communication
   - Configured in Envoy proxy

2. **Password Storage**
   - bcrypt hashing with cost factor 12
   - Salted hashes stored in database

3. **Session Management**
   - JWT tokens (HS256 signature)
   - Token expiration: 24 hours (registered), 1 hour (guests)
   - Tokens stored in browser localStorage

### Security Measures

✅ **Input Validation**
- Username: 3-30 alphanumeric characters
- Password: Minimum 8 characters
- Message: Maximum 2000 characters
- HTML/script tag sanitization

✅ **Authentication & Authorization**
- JWT token validation on every gRPC call
- User type-based permissions (GUEST, REGISTERED, ADMIN)

✅ **Rate Limiting**
- Login attempts: 5 per 15 minutes
- Messages: 30 per minute (configurable)

✅ **CORS Protection**
- Configured in Envoy proxy
- Whitelisted origins only

✅ **SQL Injection Prevention**
- JPA with parameterized queries
- No raw SQL execution

✅ **XSS Prevention**
- Input sanitization
- HTML escape in messages

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
jwt:
  secret: YourSecretKey  # Change in production!
  expiration: 86400000   # 24 hours
  guest-expiration: 3600000  # 1 hour

chat:
  max-message-length: 2000
  message-history-limit: 50
  max-username-length: 30
```

### Frontend Configuration

Edit `frontend/src/services/grpcClient.ts`:

```typescript
export const GRPC_HOST = 'http://localhost:8081';
```

## API Documentation

### gRPC Services

#### AuthService

- `Login(LoginRequest)` - User login
- `GuestLogin(GuestLoginRequest)` - Guest login
- `Logout(LogoutRequest)` - User logout
- `ValidateToken(ValidateTokenRequest)` - Token validation
- `Register(RegisterRequest)` - User registration

#### RoomService

- `GetRooms(GetRoomsRequest)` - List all rooms
- `CreateRoom(CreateRoomRequest)` - Create new room
- `JoinRoom(JoinRoomRequest)` - Join a room
- `LeaveRoom(LeaveRoomRequest)` - Leave a room
- `GetRoomUsers(GetRoomUsersRequest)` - List users in room

#### ChatService

- `SendMessage(SendMessageRequest)` - Send a message
- `StreamMessages(StreamMessagesRequest)` - Stream messages (server streaming)
- `GetMessageHistory(GetMessageHistoryRequest)` - Get message history
- `SendTypingIndicator(TypingIndicatorRequest)` - Send typing indicator

## Development

### Adding New Features

1. **Update Protocol Buffers** - Modify `.proto` files in `proto/` directory
2. **Regenerate Code**:
   - Backend: `cd backend && mvn clean compile`
   - Frontend: `cd frontend && npm run proto:generate`
3. **Implement Services** - Update service implementations
4. **Update UI** - Modify React components

### Database Schema

The H2 database includes the following tables:
- `users` - User accounts
- `rooms` - Chat rooms
- `messages` - Chat messages
- `room_members` - User-room relationships

## Troubleshooting

### Backend Issues

**Problem:** Port 9090 already in use
```bash
# Find and kill the process
lsof -ti:9090 | xargs kill -9
```

**Problem:** Maven build fails
```bash
# Clean and rebuild
mvn clean install -U
```

### Frontend Issues

**Problem:** gRPC connection failed
- Ensure Envoy proxy is running: `docker ps`
- Check Envoy logs: `docker-compose logs envoy`
- Verify backend is running on port 9090

**Problem:** Type errors
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Envoy Issues

**Problem:** Envoy won't start
```bash
# Rebuild Envoy container
docker-compose down
docker-compose build --no-cache
docker-compose up
```

## Production Deployment

### Security Checklist

- [ ] Change JWT secret key
- [ ] Enable HTTPS/TLS certificates
- [ ] Use production database (PostgreSQL/MySQL)
- [ ] Configure proper CORS origins
- [ ] Enable rate limiting
- [ ] Set up logging and monitoring
- [ ] Use environment variables for secrets
- [ ] Enable gRPC authentication interceptors

### Recommended Changes

1. **Database**: Replace H2 with PostgreSQL or MySQL
2. **Secrets Management**: Use environment variables
3. **HTTPS**: Configure SSL certificates in Envoy
4. **Session Storage**: Use Redis for token blacklisting
5. **Monitoring**: Add Prometheus metrics

## License

MIT License

## Contributing

Pull requests are welcome. For major changes, please open an issue first.

## Support

For issues and questions, please create an issue in the repository.
