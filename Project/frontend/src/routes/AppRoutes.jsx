import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Login from "../pages/Login";
import Dashboard from "../pages/Dashboard";

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="spinner" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/students"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="students" />
          </ProtectedRoute>
        }
      />

      <Route
        path="/faculty"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="faculty" />
          </ProtectedRoute>
        }
      />

      <Route
        path="/academics"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="academics" />
          </ProtectedRoute>
        }
      />

      <Route
        path="/attendance"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="attendance" />
          </ProtectedRoute>
        }
      />

      <Route
        path="/events"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="events" />
          </ProtectedRoute>
        }
      />

      <Route
        path="/facilities"
        element={
          <ProtectedRoute>
            <Dashboard initialPage="facilities" />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRoutes;