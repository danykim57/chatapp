import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authStorage } from '../utils/authStorage';
import './LoginPage.css';

const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [guestName, setGuestName] = useState('');
  const [isGuestMode, setIsGuestMode] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    // Redirect if already logged in
    if (authStorage.isAuthenticated()) {
      navigate('/rooms');
    }
  }, [navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // TODO: Implement actual gRPC login call
      // const response = await authService.login({ username, password });

      // Temporary mock implementation
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Mock success
      const mockUser = {
        userId: '123',
        username: username,
        userType: 'REGISTERED' as const,
      };
      const mockToken = 'mock-jwt-token-' + Date.now();

      authStorage.setToken(mockToken);
      authStorage.setUser(mockUser);

      navigate('/rooms');
    } catch (err) {
      setError('Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  const handleGuestLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // TODO: Implement actual gRPC guest login call
      // const response = await authService.guestLogin({ guestName });

      // Temporary mock implementation
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Mock success
      const finalGuestName = guestName.trim() || 'Guest_' + Math.random().toString(36).substring(7);
      const mockUser = {
        userId: 'guest-' + Date.now(),
        username: finalGuestName,
        userType: 'GUEST' as const,
      };
      const mockToken = 'mock-guest-jwt-token-' + Date.now();

      authStorage.setToken(mockToken);
      authStorage.setUser(mockUser);

      navigate('/rooms');
    } catch (err) {
      setError('Guest login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>💬 Chat App</h1>
          <p>Connect and chat in real-time</p>
        </div>

        {!isGuestMode ? (
          <form onSubmit={handleLogin} className="login-form">
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                className="input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
                minLength={3}
                maxLength={30}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                minLength={8}
                disabled={loading}
              />
            </div>

            {error && <div className="error">{error}</div>}

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
            </button>

            <div className="divider">or</div>

            <button
              type="button"
              className="btn btn-secondary btn-block"
              onClick={() => setIsGuestMode(true)}
              disabled={loading}
            >
              Continue as Guest
            </button>
          </form>
        ) : (
          <form onSubmit={handleGuestLogin} className="login-form">
            <div className="form-group">
              <label htmlFor="guestName">Guest Name (Optional)</label>
              <input
                id="guestName"
                type="text"
                className="input"
                value={guestName}
                onChange={(e) => setGuestName(e.target.value)}
                placeholder="Enter a name or leave blank"
                maxLength={30}
                disabled={loading}
              />
              <small className="help-text">
                Leave blank to generate a random name
              </small>
            </div>

            {error && <div className="error">{error}</div>}

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? 'Joining...' : 'Join as Guest'}
            </button>

            <button
              type="button"
              className="btn btn-secondary btn-block"
              onClick={() => {
                setIsGuestMode(false);
                setGuestName('');
                setError('');
              }}
              disabled={loading}
            >
              Back to Login
            </button>
          </form>
        )}

        <div className="login-footer">
          <p>Don't have an account? Contact admin</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
