import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { authStorage } from '../utils/authStorage';
import { formatMessageTime } from '../utils/timeFormat';
import './ChatPage.css';

interface Message {
  messageId: string;
  content: string;
  senderUsername: string;
  senderId: string;
  timestamp: number;
  messageType: 'TEXT' | 'SYSTEM';
}

interface User {
  userId: string;
  username: string;
  userType: 'REGISTERED' | 'GUEST' | 'ADMIN';
}

const ChatPage = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [messages, setMessages] = useState<Message[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [roomName, setRoomName] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [showUserList, setShowUserList] = useState(true);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const currentUser = authStorage.getUser();

  useEffect(() => {
    if (!roomId) return;

    loadRoomData();
    loadMessages();
    loadUsers();

    // TODO: Set up gRPC message streaming
    // const stream = chatService.streamMessages({ token, roomId });
    // stream.on('data', handleNewMessage);

    return () => {
      // TODO: Cleanup stream
    };
  }, [roomId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadRoomData = async () => {
    // TODO: Load room details
    // For now, use mock data
    const roomNames: Record<string, string> = {
      '1': 'General',
      '2': 'Tech Talk',
      '3': 'Gaming',
      '4': 'Music',
    };
    setRoomName(roomNames[roomId!] || 'Chat Room');
  };

  const loadMessages = async () => {
    setLoading(true);

    try {
      // TODO: Implement actual gRPC getMessageHistory call
      // const token = authStorage.getToken();
      // const response = await chatService.getMessageHistory({ token, roomId, limit: 50 });

      // Temporary mock data
      await new Promise(resolve => setTimeout(resolve, 800));

      const mockMessages: Message[] = [
        {
          messageId: '1',
          content: 'Welcome to the chat room!',
          senderUsername: 'System',
          senderId: 'system',
          timestamp: Date.now() - 3600000,
          messageType: 'SYSTEM',
        },
        {
          messageId: '2',
          content: 'Hey everyone! How is it going?',
          senderUsername: 'Alice',
          senderId: 'user1',
          timestamp: Date.now() - 1800000,
          messageType: 'TEXT',
        },
        {
          messageId: '3',
          content: 'Pretty good! Just working on a new project',
          senderUsername: 'Bob',
          senderId: 'user2',
          timestamp: Date.now() - 1200000,
          messageType: 'TEXT',
        },
      ];

      setMessages(mockMessages);
    } catch (err) {
      console.error('Failed to load messages', err);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      // TODO: Implement actual gRPC getRoomUsers call
      // const token = authStorage.getToken();
      // const response = await roomService.getRoomUsers({ token, roomId });

      // Temporary mock data
      const mockUsers: User[] = [
        { userId: 'user1', username: 'Alice', userType: 'REGISTERED' },
        { userId: 'user2', username: 'Bob', userType: 'REGISTERED' },
        { userId: 'user3', username: 'Charlie', userType: 'GUEST' },
      ];

      if (currentUser) {
        mockUsers.push(currentUser);
      }

      setUsers(mockUsers);
    } catch (err) {
      console.error('Failed to load users', err);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedMessage = inputMessage.trim();
    if (!trimmedMessage || sending) return;

    setSending(true);

    try {
      // TODO: Implement actual gRPC sendMessage call
      // const token = authStorage.getToken();
      // const response = await chatService.sendMessage({ token, roomId, content: trimmedMessage });

      // Temporary mock implementation
      await new Promise(resolve => setTimeout(resolve, 300));

      const newMessage: Message = {
        messageId: Date.now().toString(),
        content: trimmedMessage,
        senderUsername: currentUser?.username || 'You',
        senderId: currentUser?.userId || 'current',
        timestamp: Date.now(),
        messageType: 'TEXT',
      };

      setMessages(prev => [...prev, newMessage]);
      setInputMessage('');
    } catch (err) {
      console.error('Failed to send message', err);
    } finally {
      setSending(false);
    }
  };

  const handleLeaveRoom = async () => {
    try {
      // TODO: Implement actual gRPC leaveRoom call
      // const token = authStorage.getToken();
      // await roomService.leaveRoom({ token, roomId });

      navigate('/rooms');
    } catch (err) {
      console.error('Failed to leave room', err);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const isOwnMessage = (senderId: string) => {
    return senderId === currentUser?.userId;
  };

  return (
    <div className="chat-container">
      <header className="chat-header">
        <button className="back-button" onClick={handleLeaveRoom}>
          ← Back
        </button>
        <div className="chat-header-info">
          <h2>{roomName}</h2>
          <span className="user-count">👥 {users.length} users</span>
        </div>
        <button
          className="btn btn-secondary"
          onClick={() => setShowUserList(!showUserList)}
        >
          {showUserList ? 'Hide Users' : 'Show Users'}
        </button>
      </header>

      <div className="chat-content">
        <div className="messages-section">
          {loading ? (
            <div className="loading">Loading messages...</div>
          ) : (
            <div className="messages-list">
              {messages.map((message) => (
                <div
                  key={message.messageId}
                  className={`message ${
                    message.messageType === 'SYSTEM'
                      ? 'system-message'
                      : isOwnMessage(message.senderId)
                      ? 'own-message'
                      : 'other-message'
                  }`}
                >
                  {message.messageType === 'SYSTEM' ? (
                    <div className="system-content">{message.content}</div>
                  ) : (
                    <div className="message-content">
                      <div className="message-header">
                        <span className="message-sender">
                          {message.senderUsername}
                        </span>
                        <span className="message-time">
                          {formatMessageTime(message.timestamp)}
                        </span>
                      </div>
                      <div className="message-text">{message.content}</div>
                    </div>
                  )}
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
          )}

          {typingUsers.length > 0 && (
            <div className="typing-indicator">
              {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
            </div>
          )}

          <form className="message-input-form" onSubmit={handleSendMessage}>
            <input
              type="text"
              className="input message-input"
              placeholder="Type your message..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              maxLength={2000}
              disabled={sending}
            />
            <button
              type="submit"
              className="btn btn-primary send-button"
              disabled={!inputMessage.trim() || sending}
            >
              Send
            </button>
            <div className="char-count">
              {inputMessage.length}/2000
            </div>
          </form>
        </div>

        {showUserList && (
          <aside className="users-sidebar">
            <h3 className="users-header">Users Online ({users.length})</h3>
            <div className="users-list">
              {users.map((user) => (
                <div key={user.userId} className="user-item">
                  <span className="user-avatar">👤</span>
                  <span className="user-name">
                    {user.username}
                    {user.userId === currentUser?.userId && ' (You)'}
                  </span>
                  {user.userType === 'GUEST' && (
                    <span className="user-badge guest">Guest</span>
                  )}
                  {user.userType === 'ADMIN' && (
                    <span className="user-badge admin">Admin</span>
                  )}
                </div>
              ))}
            </div>
          </aside>
        )}
      </div>
    </div>
  );
};

export default ChatPage;
