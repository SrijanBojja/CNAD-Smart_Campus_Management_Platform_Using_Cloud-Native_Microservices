/**
 * Sidebar — Collapsible navigation sidebar for the dashboard.
 *
 * Props:
 *   collapsed        — boolean  — whether the sidebar is collapsed
 *   onToggle         — function — callback to toggle collapsed state
 *   mobileOpen       — boolean  — whether the sidebar is open on mobile
 *   onMobileClose    — function — callback to close the mobile sidebar
 *   activePage       — string   — id of the currently active nav item
 *   onNavigate       — function — callback when a nav item is clicked
 *   role             — string   — user role (ADMIN | FACULTY | STUDENT)
 */

const navItems = [
  {
    id: "dashboard",
    label: "Dashboard",
    implemented: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    ),
  },
  {
    id: "students",
    label: "Students",
    implemented: true,
    roles: ["ADMIN", "FACULTY"],
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 00-3-3.87" />
        <path d="M16 3.13a4 4 0 010 7.75" />
      </svg>
    ),
  },
  {
    id: "faculty",
    label: "Faculty",
    implemented: true,
    roles: ["ADMIN"],
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <polyline points="16 11 18 13 22 9" />
      </svg>
    ),
  },
  {
    id: "academics",
    label: "Academics",
    implemented: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
        <line x1="8" y1="7" x2="16" y2="7" />
        <line x1="8" y1="11" x2="13" y2="11" />
      </svg>
    ),
  },
  {
    id: "attendance",
    label: "Attendance",
    implemented: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2" />
        <rect x="8" y="2" width="8" height="4" rx="1" />
        <path d="M9 14l2 2 4-4" />
      </svg>
    ),
  },
  {
    id: "events",
    label: "Events",
    implemented: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="2" />
        <line x1="16" y1="2" x2="16" y2="6" />
        <line x1="8" y1="2" x2="8" y2="6" />
        <line x1="3" y1="10" x2="21" y2="10" />
        <circle cx="12" cy="16" r="1" />
      </svg>
    ),
  },
  {
    id: "facilities",
    label: "Facilities",
    implemented: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 21h18" />
        <path d="M5 21V7l8-4v18" />
        <path d="M19 21V11l-6-4" />
        <line x1="9" y1="9" x2="9" y2="9.01" />
        <line x1="9" y1="13" x2="9" y2="13.01" />
        <line x1="9" y1="17" x2="9" y2="17.01" />
      </svg>
    ),
  },
  {
    id: "notifications",
    label: "Notifications",
    implemented: false,
    badge: "Soon",
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 01-3.46 0" />
      </svg>
    ),
  },
];

const Sidebar = ({
  collapsed,
  onToggle,
  mobileOpen,
  onMobileClose,
  activePage,
  onNavigate,
  role,
}) => {
  const visibleItems = navItems.filter(
    (item) => !item.roles || (Boolean(role) && item.roles.includes(role))
  );

  const handleNavClick = (item) => {
    if (!item.implemented) return;
    onNavigate(item.id);
    onMobileClose();
  };

  return (
    <>
      {/* Mobile backdrop */}
      <div
        className={`sidebar-overlay ${mobileOpen ? "visible" : ""}`}
        onClick={onMobileClose}
        aria-hidden="true"
      />

      <aside
        className={`sidebar ${collapsed ? "collapsed" : ""} ${mobileOpen ? "mobile-open" : ""}`}
        id="dashboard-sidebar"
      >
        {/* Brand */}
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M2 10l10-7 10 7" />
              <path d="M4 10v10a1 1 0 001 1h14a1 1 0 001-1V10" />
              <path d="M10 21V14h4v7" />
            </svg>
          </div>
          <div className="sidebar-brand-text">
            <h2>Smart Campus</h2>
            <span>Management Portal</span>
          </div>
        </div>

        {/* Collapse toggle */}
        <button
          className="sidebar-toggle"
          onClick={onToggle}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>

        {/* Navigation */}
        <nav className="sidebar-nav" aria-label="Main navigation">
          <div className="nav-section-label">Menu</div>
          {visibleItems.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${activePage === item.id ? "active" : ""} ${!item.implemented ? "disabled" : ""}`}
              onClick={() => handleNavClick(item)}
              title={item.implemented ? item.label : `${item.label} — Coming soon`}
              aria-current={activePage === item.id ? "page" : undefined}
              disabled={!item.implemented}
            >
              <span className="nav-item-icon" aria-hidden="true">
                {item.icon}
              </span>
              <span className="nav-item-label">{item.label}</span>
              {item.badge && (
                <span className="nav-item-badge">{item.badge}</span>
              )}
            </button>
          ))}
        </nav>
      </aside>
    </>
  );
};

export default Sidebar;
