import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { authStorage } from './utils/authStorage';
import LoginPage from './pages/LoginPage';
import RoomListPage from './pages/RoomListPage';
import ChatPage from './pages/ChatPage';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  return authStorage.isAuthenticated() ? <>{children}</> : <Navigate to="/login" />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/rooms"
          element={
            <PrivateRoute>
              <RoomListPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/chat/:roomId"
          element={
            <PrivateRoute>
              <ChatPage />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/rooms" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
