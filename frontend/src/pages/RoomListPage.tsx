import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authStorage } from '../utils/authStorage';
import { formatTimestamp } from '../utils/timeFormat';
import './RoomListPage.css';

interface Room {
  roomId: string;
  roomName: string;
  description: string;
  activeUsers: number;
  lastMessage: string;
  lastMessageTime: number;
}

const RoomListPage = () => {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const user = authStorage.getUser();

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    setLoading(true);
    setError('');

    try {
      // TODO: Implement actual gRPC getRooms call
      // const token = authStorage.getToken();
      // const response = await roomService.getRooms({ token, searchQuery });

      // Temporary mock data
      await new Promise(resolve => setTimeout(resolve, 1000));

      const mockRooms: Room[] = [
        {
          roomId: '1',
          roomName: 'General',
          description: 'Welcome to general discussion',
          activeUsers: 24,
          lastMessage: 'Hey everyone!',
          lastMessageTime: Date.now() - 120000,
        },
        {
          roomId: '2',
          roomName: 'Tech Talk',
          description: 'Discuss programming and tech',
          activeUsers: 15,
          lastMessage: 'Check this out',
          lastMessageTime: Date.now() - 300000,
        },
        {
          roomId: '3',
          roomName: 'Gaming',
          description: 'For gamers and game discussions',
          activeUsers: 8,
          lastMessage: 'Anyone playing?',
          lastMessageTime: Date.now() - 3600000,
        },
        {
          roomId: '4',
          roomName: 'Music',
          description: 'Share and discuss music',
          activeUsers: 5,
          lastMessage: 'Great song!',
          lastMessageTime: Date.now() - 10800000,
        },
      ];

      setRooms(mockRooms);
    } catch (err) {
      setError('Failed to load rooms');
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = async (roomId: string) => {
    try {
      // TODO: Implement actual gRPC joinRoom call
      // const token = authStorage.getToken();
      // await roomService.joinRoom({ token, roomId });

      navigate(`/chat/${roomId}`);
    } catch (err) {
      setError('Failed to join room');
    }
  };

  const handleLogout = () => {
    // TODO: Implement actual gRPC logout call
    authStorage.clear();
    navigate('/login');
  };

  const filteredRooms = rooms.filter(room =>
    room.roomName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="room-list-container">
      <header className="room-list-header">
        <div className="header-content">
          <h1>💬 Chat App</h1>
          <div className="header-right">
            <span className="user-info">
              👤 {user?.username}
              {user?.userType === 'GUEST' && <span className="guest-badge">Guest</span>}
            </span>
            <button className="btn btn-secondary" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="room-list-content">
        <div className="search-section">
          <input
            type="text"
            className="input search-input"
            placeholder="Search rooms..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {loading ? (
          <div className="loading">Loading rooms...</div>
        ) : error ? (
          <div className="error-message">
            <p>{error}</p>
            <button className="btn btn-primary" onClick={loadRooms}>
              Retry
            </button>
          </div>
        ) : filteredRooms.length === 0 ? (
          <div className="empty-state">
            <p>No rooms found</p>
          </div>
        ) : (
          <div className="rooms-grid">
            {filteredRooms.map((room) => (
              <div
                key={room.roomId}
                className="room-card"
                onClick={() => handleJoinRoom(room.roomId)}
              >
                <div className="room-header">
                  <h3 className="room-name">{room.roomName}</h3>
                  <span className="room-users">👥 {room.activeUsers}</span>
                </div>
                <p className="room-description">{room.description}</p>
                {room.lastMessage && (
                  <div className="room-footer">
                    <p className="last-message">
                      <span className="last-message-text">
                        {room.lastMessage}
                      </span>
                      <span className="last-message-time">
                        {formatTimestamp(room.lastMessageTime)}
                      </span>
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default RoomListPage;
