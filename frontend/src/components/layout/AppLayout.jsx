import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function AppLayout() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="app-main">
        <div className="app-content-container">
          <Outlet />
        </div>
      </main>
      <footer className="app-footer">
        <div className="footer-container">
          <span>QueueSense &bull; Smart Campus Facility & Resource Management</span>
          <span className="footer-badge">Competition Functional Prototype</span>
        </div>
      </footer>
    </div>
  );
}
