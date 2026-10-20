import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import SidebarLayout from './SidebarLayout';

export default function AppLayout() {
  const [layoutMode, setLayoutMode] = useState(() => {
    return localStorage.getItem('queuesense_layout_mode') || 'sidebar';
  });

  const toggleLayoutMode = () => {
    const nextMode = layoutMode === 'sidebar' ? 'topbar' : 'sidebar';
    setLayoutMode(nextMode);
    localStorage.setItem('queuesense_layout_mode', nextMode);
  };

  if (layoutMode === 'sidebar') {
    return <SidebarLayout layoutMode={layoutMode} onToggleLayout={toggleLayoutMode} />;
  }

  // Original horizontal top navigation layout
  return (
    <div className="app-shell">
      <Navbar layoutMode={layoutMode} onToggleLayout={toggleLayoutMode} />
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
