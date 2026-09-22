import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';

// Student Pages
import StudentDashboard from './pages/student/StudentDashboard';
import LibrariesList from './pages/student/LibrariesList';
import FacilityDetails from './pages/student/FacilityDetails';
import MyRequests from './pages/student/MyRequests';
import DigitalConfirmation from './pages/student/DigitalConfirmation';
import StudentHistory from './pages/student/StudentHistory';

// Operator Pages
import OperatorDashboard from './pages/operator/OperatorDashboard';
import OperatorRequests from './pages/operator/OperatorRequests';
import OperatorResources from './pages/operator/OperatorResources';
import OperatorCheckIn from './pages/operator/OperatorCheckIn';
import OperatorHistory from './pages/operator/OperatorHistory';

import './App.css';

function RootRedirect() {
  const { user, isAuthenticated, loading } = useAuth();
  if (loading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user?.role === 'ROLE_OPERATOR') return <Navigate to="/operator/dashboard" replace />;
  return <Navigate to="/student/dashboard" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<RootRedirect />} />

          {/* Student Protected Routes */}
          <Route
            path="/student"
            element={
              <ProtectedRoute allowedRole="ROLE_STUDENT">
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/student/dashboard" replace />} />
            <Route path="dashboard" element={<StudentDashboard />} />
            <Route path="libraries" element={<LibrariesList />} />
            <Route path="libraries/:facilityId" element={<FacilityDetails />} />
            <Route path="requests" element={<MyRequests />} />
            <Route path="confirmation/:requestId" element={<DigitalConfirmation />} />
            <Route path="history" element={<StudentHistory />} />
          </Route>

          {/* Operator Protected Routes */}
          <Route
            path="/operator"
            element={
              <ProtectedRoute allowedRole="ROLE_OPERATOR">
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/operator/dashboard" replace />} />
            <Route path="dashboard" element={<OperatorDashboard />} />
            <Route path="requests" element={<OperatorRequests />} />
            <Route path="resources" element={<OperatorResources />} />
            <Route path="check-ins" element={<OperatorCheckIn />} />
            <Route path="history" element={<OperatorHistory />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<RootRedirect />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
