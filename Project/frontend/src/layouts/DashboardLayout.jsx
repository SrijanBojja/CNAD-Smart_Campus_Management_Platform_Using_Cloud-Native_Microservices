import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import Sidebar from "../components/Sidebar";
import "../pages/Dashboard.css";

/**
 * DashboardLayout — Shell layout with sidebar, header, and content area.
 *
 * Props:
 *   activePage   — string    — current active nav item id
 *   onNavigate   — function  — callback when nav item is clicked
 *   pageTitle    — string    — title shown in the header
 *   children     — ReactNode — page content
 */
const DashboardLayout = ({ activePage, onNavigate, pageTitle, children }) => {
  const { user, logout } = useAuth();
  const role = user?.role || null;
  const username = user?.username || "User";

  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const avatarInitial = username.charAt(0).toUpperCase();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="dashboard-layout">
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed((prev) => !prev)}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
        activePage={activePage}
        onNavigate={onNavigate}
        role={role}
      />

      <main
        className="dashboard-main"
        style={{ marginLeft: sidebarCollapsed ? 72 : 260 }}
      >
        {/* Top Header */}
        <header className="dashboard-header" id="dashboard-header">
          {/* Mobile menu button */}
          <button
            className="mobile-menu-btn"
            onClick={() => setMobileOpen(true)}
            aria-label="Open navigation menu"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>

          <h1 className="header-title">{pageTitle || "Dashboard"}</h1>

          <div className="header-right">
            <div className="header-user-info">
              <span className="header-username">{username}</span>
              <span className="header-role">{role || "UNASSIGNED"}</span>
            </div>
            <div className="header-avatar" aria-hidden="true" title={username}>
              {avatarInitial}
            </div>
            <button
              className="logout-btn"
              onClick={handleLogout}
              id="logout-button"
              title="Sign out"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              <span>Logout</span>
            </button>
          </div>
        </header>

        {/* Page Content */}
        <div className="dashboard-content">{children}</div>
      </main>
    </div>
  );
};

export default DashboardLayout;
