import { useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import DashboardOverview from "./DashboardOverview";

/**
 * Placeholder for future module pages.
 */
const PlaceholderPage = ({ title }) => (
  <div className="placeholder-page">
    <div className="placeholder-icon" aria-hidden="true">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    </div>
    <h2>{title}</h2>
    <p>This module is under development and will be available in a future update.</p>
  </div>
);

/**
 * Page title map.
 */
const pageTitles = {
  dashboard: "Dashboard",
  students: "Students",
  academics: "Academics",
  attendance: "Attendance",
  events: "Events",
  facilities: "Facilities",
  notifications: "Notifications",
};

/**
 * Dashboard — top-level page that manages which sub-page is rendered
 * within the dashboard layout. Currently only "dashboard" has real content.
 */
const Dashboard = () => {
  const [activePage, setActivePage] = useState("dashboard");

  const handleNavigate = (pageId) => {
    setActivePage(pageId);
  };

  const renderContent = () => {
    switch (activePage) {
      case "dashboard":
        return <DashboardOverview />;
      default:
        return <PlaceholderPage title={pageTitles[activePage] || activePage} />;
    }
  };

  return (
    <DashboardLayout
      activePage={activePage}
      onNavigate={handleNavigate}
      pageTitle={pageTitles[activePage] || "Dashboard"}
    >
      {renderContent()}
    </DashboardLayout>
  );
};

export default Dashboard;
