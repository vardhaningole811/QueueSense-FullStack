import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  Compass,
  BookOpen,
  ClipboardList,
  History,
  QrCode,
  LayoutDashboard,
  LogOut,
  User,
  ShieldCheck,
  Building2,
  ArrowLeftRight,
  Trophy,
  LayoutTemplate,
  Monitor,
} from 'lucide-react';

export default function Navbar({ layoutMode, onToggleLayout }) {
  const { user, isStudent, isOperator, logout, login } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleQuickSwitch = async () => {
    try {
      if (isStudent) {
        await login('operator@queuesense.demo', 'Operator@123');
        navigate('/operator/dashboard');
      } else {
        await login('student@queuesense.demo', 'Student@123');
        navigate('/student/dashboard');
      }
    } catch (err) {
      console.error('Quick switch failed:', err);
    }
  };

  return (
    <header className="navbar-root">
      <div className="navbar-container">
        {/* Brand */}
        <div className="navbar-brand-section">
          <Link to={isStudent ? '/student/dashboard' : '/operator/dashboard'} className="brand-logo">
            <div className="brand-icon-box">
              <Compass size={20} className="brand-icon" />
            </div>
            <div className="brand-text">
              <span className="brand-title">QueueSense</span>
              <span className="brand-subtitle">Smart Campus</span>
            </div>
          </Link>
          <span className={`portal-tag ${isStudent ? 'tag-student' : 'tag-operator'}`}>
            {isStudent ? 'Student Portal' : 'Facility Operator'}
          </span>
        </div>

        {/* Navigation Links */}
        <nav className="navbar-nav">
          {isStudent && (
            <>
              <NavLink
                to="/student/dashboard"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <LayoutDashboard size={16} />
                <span>Dashboard</span>
              </NavLink>
              <NavLink
                to="/student/libraries"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <BookOpen size={16} />
                <span>Libraries</span>
              </NavLink>
              <NavLink
                to="/student/elibrary"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <Monitor size={16} />
                <span>E-Library Floor Plan</span>
              </NavLink>
              <NavLink
                to="/student/sports"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <Trophy size={16} />
                <span>Sports Complex</span>
              </NavLink>
              <NavLink
                to="/student/requests"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <ClipboardList size={16} />
                <span>My Requests</span>
              </NavLink>
              <NavLink
                to="/student/history"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <History size={16} />
                <span>History</span>
              </NavLink>
            </>
          )}

          {isOperator && (
            <>
              <NavLink
                to="/operator/dashboard"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <LayoutDashboard size={16} />
                <span>Dashboard</span>
              </NavLink>
              <NavLink
                to="/operator/requests"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <ClipboardList size={16} />
                <span>Pending Requests</span>
              </NavLink>
              <NavLink
                to="/operator/resources"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <Building2 size={16} />
                <span>Resources</span>
              </NavLink>
              <NavLink
                to="/operator/sports-inventory"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <Trophy size={16} />
                <span>Sports Inventory</span>
              </NavLink>
              <NavLink
                to="/operator/check-ins"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <QrCode size={16} />
                <span>QR Check-in</span>
              </NavLink>
              <NavLink
                to="/operator/history"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <History size={16} />
                <span>History</span>
              </NavLink>
            </>
          )}
        </nav>

        {/* User profile & Actions */}
        <div className="navbar-user-section">
          {onToggleLayout && (
            <button
              type="button"
              onClick={onToggleLayout}
              className="btn-quick-switch"
              title="Switch to experimental modern sidebar layout"
            >
              <LayoutTemplate size={14} />
              <span className="quick-switch-text">Try Sidebar Layout</span>
            </button>
          )}

          {/* Quick demo switch helper */}
          <button
            type="button"
            onClick={handleQuickSwitch}
            className="btn-quick-switch"
            title={`Switch to ${isStudent ? 'Operator' : 'Student'} demo view`}
          >
            <ArrowLeftRight size={14} />
            <span className="quick-switch-text">
              Switch to {isStudent ? 'Operator' : 'Student'}
            </span>
          </button>

          <div className="user-profile-badge">
            <div className="avatar-circle">
              {isStudent ? <User size={15} /> : <ShieldCheck size={15} />}
            </div>
            <div className="user-info-text">
              <span className="user-name">{user?.name}</span>
              <span className="user-detail">
                {isStudent ? user?.studentId || 'Student' : user?.assignedFacilityName || 'Operator'}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="btn-logout"
            title="Log out of QueueSense"
          >
            <LogOut size={16} />
            <span className="logout-text">Logout</span>
          </button>
        </div>
      </div>
    </header>
  );
}
