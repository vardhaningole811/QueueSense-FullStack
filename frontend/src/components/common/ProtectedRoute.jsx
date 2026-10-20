import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

export default function ProtectedRoute({ children, allowedRole }) {
  const { user, loading, isAuthenticated } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Checking authentication..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const isRoleAllowed =
    !allowedRole ||
    user.role === allowedRole ||
    (allowedRole === 'ROLE_OPERATOR' && user.role === 'ROLE_ADMIN');

  if (!isRoleAllowed) {
    // Redirect to the user's primary dashboard if role mismatch
    if (user.role === 'ROLE_STUDENT') {
      return <Navigate to="/student/dashboard" replace />;
    } else {
      return <Navigate to="/operator/dashboard" replace />;
    }
  }

  return children;
}
