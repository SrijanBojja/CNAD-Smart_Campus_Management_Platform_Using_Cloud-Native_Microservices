import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import dashboardService from "../services/dashboardService";
import "./Dashboard.css";

/**
 * StatCard — individual stat tile with loading, error, and success states.
 */
const StatCard = ({ iconClass, icon, label, value, note, loading, error }) => (
  <div className="stat-card" role="group" aria-label={label}>
    <div className={`stat-card-icon ${iconClass}`} aria-hidden="true">
      {icon}
    </div>

    {loading ? (
      <div className="stat-card-loading">
        <div className="skeleton skeleton-text" />
        <div className="skeleton skeleton-number" />
        <div className="skeleton skeleton-small" />
      </div>
    ) : error ? (
      <div className="stat-card-error">
        <span className="error-label">{label}</span>
        <span className="error-msg">{error}</span>
      </div>
    ) : (
      <div className="stat-card-body">
        <div className="stat-card-label">{label}</div>
        <div className="stat-card-value">{value}</div>
        {note && <div className="stat-card-note">{note}</div>}
      </div>
    )}
  </div>
);

/**
 * DashboardOverview — the main dashboard content with stat cards
 * and quick-info panels. Fetches live data from the backend APIs.
 */
const DashboardOverview = () => {
  const { user } = useAuth();
  const role = user?.role || null;

  const [stats, setStats] = useState({
    students: { value: null, loading: true, error: null },
    courses: { value: null, loading: true, error: null },
    subjects: { value: null, loading: true, error: null },
    schedules: { value: null, loading: true, error: null },
  });

  useEffect(() => {
    const fetchStat = async (key, fetcher) => {
      try {
        const count = await fetcher();
        setStats((prev) => ({
          ...prev,
          [key]: { value: count, loading: false, error: null },
        }));
      } catch (err) {
        const status = err.response?.status;
        let msg = "Service unavailable";

        if (status === 403) {
          msg = "Access restricted";
        } else if (status === 401) {
          msg = "Session expired";
        } else if (err.code === "ERR_NETWORK") {
          msg = "Service offline";
        }

        setStats((prev) => ({
          ...prev,
          [key]: { value: null, loading: false, error: msg },
        }));
      }
    };

    // Students endpoint requires ADMIN or FACULTY role
    if (role === "ADMIN" || role === "FACULTY") {
      fetchStat("students", dashboardService.getStudentCount);
    } else {
      setStats((prev) => ({
        ...prev,
        students: { value: null, loading: false, error: "Access restricted" },
      }));
    }

    // These endpoints are accessible by all authenticated roles
    fetchStat("courses", dashboardService.getCourseCount);
    fetchStat("subjects", dashboardService.getSubjectCount);
    fetchStat("schedules", dashboardService.getScheduleCount);
  }, [role]);

  /** Greeting based on time of day */
  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";
    return "Good evening";
  };

  /** Friendly role label */
  const roleLabel = {
    ADMIN: "Administrator",
    FACULTY: "Faculty Member",
    STUDENT: "Student",
  };

  return (
    <>
      {/* Welcome Section */}
      <div className="dashboard-welcome">
        <h1>
          {getGreeting()}, {user?.username || "User"}
        </h1>
        <p>
          Welcome to Smart Campus Management Platform.{" "}
          {role === "ADMIN"
            ? "You have full administrative access."
            : role === "FACULTY"
              ? "You can view students and manage academics."
              : role === "STUDENT"
                ? "Here's your campus overview."
                : "Your account is active."}
        </p>
      </div>

      {/* Summary Stats */}
      <div className="stats-grid" id="dashboard-stats">
        <StatCard
          iconClass="students"
          icon={
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 00-3-3.87" />
              <path d="M16 3.13a4 4 0 010 7.75" />
            </svg>
          }
          label="Total Students"
          value={stats.students.value ?? "—"}
          note={
            role === "ADMIN" || role === "FACULTY"
              ? "From student service"
              : undefined
          }
          loading={stats.students.loading}
          error={stats.students.error}
        />

        <StatCard
          iconClass="courses"
          icon={
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
            </svg>
          }
          label="Courses"
          value={stats.courses.value ?? "—"}
          note="From academic service"
          loading={stats.courses.loading}
          error={stats.courses.error}
        />

        <StatCard
          iconClass="subjects"
          icon={
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2" />
              <polyline points="2 17 12 22 22 17" />
              <polyline points="2 12 12 17 22 12" />
            </svg>
          }
          label="Subjects"
          value={stats.subjects.value ?? "—"}
          note="From academic service"
          loading={stats.subjects.loading}
          error={stats.subjects.error}
        />

        <StatCard
          iconClass="schedules"
          icon={
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          }
          label="Class Schedules"
          value={stats.schedules.value ?? "—"}
          note="From academic service"
          loading={stats.schedules.loading}
          error={stats.schedules.error}
        />
      </div>

      {/* Quick Info */}
      <div className="dashboard-section">
        <h2 className="dashboard-section-title">Quick Information</h2>
        <div className="quick-info-grid">
          <div className="info-card">
            <h3>Your Role</h3>
            <p>
              You are signed in as{" "}
              <strong>{roleLabel[role] || role || "Unassigned"}</strong>.
              {role === "ADMIN"
                ? " You can manage students, courses, subjects, and schedules."
                : role === "FACULTY"
                  ? " You can view students, manage courses and schedules."
                  : role === "STUDENT"
                    ? " You can view courses, subjects, and your schedules."
                    : " Your permissions are restricted."}
            </p>
          </div>
          <div className="info-card">
            <h3>Platform Status</h3>
            <p>
              The Smart Campus platform is built with cloud-native microservices.
              Additional modules (Attendance, Events, Facilities, Notifications)
              are under development.
            </p>
          </div>
          <div className="info-card">
            <h3>Need Help?</h3>
            <p>
              Contact campus IT support or your department administrator for
              assistance with platform access or data inquiries.
            </p>
          </div>
        </div>
      </div>
    </>
  );
};

export default DashboardOverview;
