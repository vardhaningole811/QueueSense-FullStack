import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
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
  Menu,
  X,
  LayoutTemplate,
  ChevronRight,
  Monitor,
} from 'lucide-react';
import './SidebarLayout.css';

export default function SidebarLayout({ layoutMode, onToggleLayout }) {
  const { user, isStudent, isOperator, logout, login } = useAuth();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

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

  // Derive readable breadcrumb from route path
  const getBreadcrumbTitle = () => {
    const p = location.pathname;
    if (p.includes('/dashboard')) return 'Dashboard Overview';
    if (p.includes('/elibrary')) return 'E-Library Seat Selection (Floor Plan)';
    if (p.includes('/libraries/')) return 'Resource Allocation';
    if (p.includes('/libraries')) return 'Libraries & Facilities';
    if (p.includes('/sports-inventory')) return 'Sports Inventory & Equipment';
    if (p.includes('/sports')) return 'Sports Complex & Courts';
    if (p.includes('/requests')) return isStudent ? 'My Reservation Requests' : 'Pending Approvals';
    if (p.includes('/resources')) return 'Resource Management';
    if (p.includes('/check-ins')) return 'Digital QR Check-in';
    if (p.includes('/confirmation')) return 'Digital Campus Pass';
    if (p.includes('/history')) return 'Allocation History';
    return isStudent ? 'Student Workspace' : 'Operator Console';
  };

  return (
    <div className="sidebar-layout-root">
      {/* Mobile Drawer Backdrop Overlay */}
      <div
        className={`sidebar-backdrop ${isMobileOpen ? 'active' : ''}`}
        onClick={() => setIsMobileOpen(false)}
        aria-hidden="true"
      />

      {/* Vertical Left Sidebar */}
      <aside className={`app-sidebar ${isMobileOpen ? 'sidebar-open' : ''}`}>
        {/* Header / Brand */}
        <div className="sidebar-header">
          <Link
            to={isStudent ? '/student/dashboard' : '/operator/dashboard'}
            className="sidebar-brand"
            onClick={() => setIsMobileOpen(false)}
          >
            <div className="sidebar-brand-icon-box">
              <Compass size={22} />
            </div>
            <div className="sidebar-brand-text">
              <span className="sidebar-brand-title">QueueSense</span>
              <span className="sidebar-brand-subtitle">Smart Campus</span>
            </div>
          </Link>

          <div className={`sidebar-portal-badge ${isStudent ? 'tag-student' : 'tag-operator'}`}>
            {isStudent ? <User size={13} /> : <ShieldCheck size={13} />}
            <span>{isStudent ? 'Student Portal' : 'Facility Operator'}</span>
          </div>
        </div>

        {/* Vertical Navigation Links */}
        <nav className="sidebar-nav-section" aria-label="Main Navigation">
          <span className="sidebar-section-title">Navigation</span>

          {isStudent && (
            <>
              <NavLink
                to="/student/dashboard"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <LayoutDashboard size={18} />
                <span>Dashboard</span>
              </NavLink>

              <NavLink
                to="/student/libraries"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <BookOpen size={18} />
                <span>Libraries</span>
              </NavLink>

              <NavLink
                to="/student/elibrary"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <Monitor size={18} />
                <span>E-Library Floor Plan</span>
              </NavLink>

              <NavLink
                to="/student/sports"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <Trophy size={18} />
                <span>Sports Complex</span>
              </NavLink>

              <NavLink
                to="/student/requests"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <ClipboardList size={18} />
                <span>My Requests</span>
              </NavLink>

              <NavLink
                to="/student/history"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <History size={18} />
                <span>History</span>
              </NavLink>
            </>
          )}

          {isOperator && (
            <>
              <NavLink
                to="/operator/dashboard"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <LayoutDashboard size={18} />
                <span>Dashboard</span>
              </NavLink>

              <NavLink
                to="/operator/requests"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <ClipboardList size={18} />
                <span>Pending Requests</span>
              </NavLink>

              <NavLink
                to="/operator/resources"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <Building2 size={18} />
                <span>Resources</span>
              </NavLink>

              <NavLink
                to="/operator/sports-inventory"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <Trophy size={18} />
                <span>Sports Inventory</span>
              </NavLink>

              <NavLink
                to="/operator/check-ins"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <QrCode size={18} />
                <span>QR Check-in</span>
              </NavLink>

              <NavLink
                to="/operator/history"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={() => setIsMobileOpen(false)}
              >
                <History size={18} />
                <span>History</span>
              </NavLink>
            </>
          )}
        </nav>

        {/* Sidebar Footer Controls */}
        <div className="sidebar-footer">
          <button
            type="button"
            onClick={handleQuickSwitch}
            className="sidebar-quick-switch-btn"
            title={`Switch to ${isStudent ? 'Operator' : 'Student'} demo view`}
          >
            <ArrowLeftRight size={14} />
            <span>Switch to {isStudent ? 'Operator' : 'Student'}</span>
          </button>

          {onToggleLayout && (
            <button
              type="button"
              onClick={onToggleLayout}
              className="sidebar-layout-toggle-btn"
              title="Revert to original top horizontal navigation layout"
            >
              <LayoutTemplate size={14} />
              <span>Switch to Top Nav</span>
            </button>
          )}

          <div className="sidebar-version-tag">
            <span>Prototype &bull; Experimental UI</span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="sidebar-layout-main">
        {/* Top Toolbar */}
        <header className="app-topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="topbar-mobile-menu-btn"
              onClick={() => setIsMobileOpen(!isMobileOpen)}
              aria-label="Toggle navigation drawer"
            >
              {isMobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>

            <div className="topbar-breadcrumbs">
              <span className="breadcrumb-root">{isStudent ? 'Student Portal' : 'Facility Operator'}</span>
              <ChevronRight size={14} className="breadcrumb-sep" />
              <span className="breadcrumb-current">{getBreadcrumbTitle()}</span>
            </div>
          </div>

          {/* Far Right: Layout Switcher, Quick Switch, User Profile, and LOGOUT Button */}
          <div className="topbar-right">
            {onToggleLayout && (
              <button
                type="button"
                onClick={onToggleLayout}
                className="topbar-switch-layout-btn"
                title="Toggle between experimental sidebar and original top navbar"
              >
                <LayoutTemplate size={14} />
                <span>{layoutMode === 'sidebar' ? 'Layout: Sidebar' : 'Layout: Top Nav'}</span>
              </button>
            )}

            <button
              type="button"
              onClick={handleQuickSwitch}
              className="topbar-switch-layout-btn"
              title={`Switch to ${isStudent ? 'Operator' : 'Student'} demo view`}
            >
              <ArrowLeftRight size={14} />
              <span>{isStudent ? 'Operator View' : 'Student View'}</span>
            </button>

            {/* User Profile Badge */}
            <div className="topbar-user-badge">
              <div className="topbar-avatar">
                {isStudent ? <User size={16} /> : <ShieldCheck size={16} />}
              </div>
              <div className="topbar-user-meta">
                <span className="topbar-user-name">{user?.name}</span>
                <span className="topbar-user-role">
                  {isStudent ? user?.studentId || 'Student' : user?.assignedFacilityName || 'Operator'}
                </span>
              </div>
            </div>

            {/* Logout Button placed prominently at the far right of the top toolbar */}
            <button
              type="button"
              onClick={handleLogout}
              className="topbar-logout-btn"
              title="Log out of QueueSense"
            >
              <LogOut size={16} />
              <span>Logout</span>
            </button>
          </div>
        </header>

        {/* Page Content Container */}
        <main className="sidebar-page-content">
          <Outlet />
        </main>

        {/* Footer */}
        <footer className="sidebar-layout-footer">
          <span>QueueSense &bull; Smart Campus Facility & Resource Management</span>
          <span className="footer-badge">Competition Functional Prototype</span>
        </footer>
      </div>
    </div>
  );
}
