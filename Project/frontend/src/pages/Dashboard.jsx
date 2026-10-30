import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import DashboardOverview from "./DashboardOverview";
import StudentsPage from "./StudentsPage";
import FacultyPage from "./FacultyPage";
import AcademicsPage from "./AcademicsPage";
import AttendancePage from "./AttendancePage";
import EventsPage from "./EventsPage";
import FacilitiesPage from "./FacilitiesPage";

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
  students: "Students Management",
  faculty: "Faculty Management",
  academics: "Academics Management",
  attendance: "Attendance Management",
  events: "Events Management",
  facilities: "Facilities Management",
  notifications: "Notifications",
};

/**
 * Dashboard — top-level page that manages which sub-page is rendered
 * within the dashboard layout. Synchronizes active page with browser routes.
 */
const Dashboard = ({ initialPage }) => {
  const location = useLocation();
  const navigate = useNavigate();

  const getPageFromPath = (path) => {
    if (path === "/students") return "students";
    if (path === "/faculty") return "faculty";
    if (path === "/academics") return "academics";
    if (path === "/attendance") return "attendance";
    if (path === "/events") return "events";
    if (path === "/facilities") return "facilities";
    return "dashboard";
  };

  const [activePage, setActivePage] = useState(() => {
    if (initialPage) return initialPage;
    return getPageFromPath(location.pathname);
  });

  useEffect(() => {
    const page = getPageFromPath(location.pathname);
    setActivePage(page);
  }, [location.pathname]);

  const handleNavigate = (pageId) => {
    setActivePage(pageId);
    if (pageId === "dashboard") {
      navigate("/");
    } else {
      navigate(`/${pageId}`);
    }
  };

  const renderContent = () => {
    switch (activePage) {
      case "dashboard":
        return <DashboardOverview onNavigate={handleNavigate} />;
      case "students":
        return <StudentsPage onNavigate={handleNavigate} />;
      case "faculty":
        return <FacultyPage onNavigate={handleNavigate} />;
      case "academics":
        return <AcademicsPage onNavigate={handleNavigate} />;
      case "attendance":
        return <AttendancePage onNavigate={handleNavigate} />;
      case "events":
        return <EventsPage onNavigate={handleNavigate} />;
      case "facilities":
        return <FacilitiesPage onNavigate={handleNavigate} />;
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
