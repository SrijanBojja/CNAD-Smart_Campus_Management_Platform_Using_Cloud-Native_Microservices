import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import academicService from "../services/academicService";
import attendanceService from "../services/attendanceService";
import authService from "../services/authService";
import studentService from "../services/studentService";
import "./Attendance.css";

/**
 * Format Time ("09:00:00" -> "9:00 AM")
 */
const formatTime = (timeStr) => {
  if (!timeStr) return "—";
  const parts = String(timeStr).split(":");
  if (parts.length >= 2) {
    let hours = parseInt(parts[0], 10);
    const minutes = parts[1];
    const ampm = hours >= 12 ? "PM" : "AM";
    hours = hours % 12 || 12;
    return `${hours}:${minutes} ${ampm}`;
  }
  return timeStr;
};

/**
 * Format DateTime ("2026-09-25T09:05:00" -> "Sep 25, 2026, 9:05 AM")
 */
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return "—";
  try {
    const d = new Date(dateTimeStr);
    return d.toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  } catch {
    return dateTimeStr;
  }
};

const initialSessionForm = {
  subjectId: "",
  facultyUserId: "",
  sessionDate: new Date().toISOString().split("T")[0],
  startTime: "09:00",
  endTime: "10:30",
  roomNumber: "",
  academicYear: "2026-2027",
  semester: 1,
  section: "A",
};

const initialRecordForm = {
  sessionId: "",
  studentId: "",
  status: "PRESENT",
  remarks: "",
};

/**
 * AttendancePage — Attendance Management module.
 * Provides Attendance Sessions and Attendance Records tabs with full CRUD,
 * live dropdown population, search, pagination, and role-based permissions.
 */
const AttendancePage = () => {
  const { user } = useAuth();
  const role = user?.role;
  const isAdmin = role === "ADMIN";
  const isFaculty = role === "FACULTY";
  const isAdminOrFaculty = isAdmin || isFaculty;

  const [activeTab, setActiveTab] = useState("sessions");
  const [toast, setToast] = useState(null); // { message, type: 'success' | 'error' }

  // Shared dropdown resources
  const [dropdownSubjects, setDropdownSubjects] = useState([]);
  const [dropdownFaculty, setDropdownFaculty] = useState([]);
  const [dropdownSessions, setDropdownSessions] = useState([]);
  const [dropdownStudents, setDropdownStudents] = useState([]);

  // ---- Sessions State ----
  const [sessionsState, setSessionsState] = useState({
    items: [],
    loading: true,
    refreshing: false,
    error: null,
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0,
    search: "",
  });

  // ---- Records State ----
  const [recordsState, setRecordsState] = useState({
    items: [],
    loading: true,
    refreshing: false,
    error: null,
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0,
    statusFilter: "ALL",
    search: "",
  });

  // ---- Modal States ----
  // Sessions
  const [sessionModal, setSessionModal] = useState({ open: false, mode: "create", item: null });
  const [sessionFormData, setSessionFormData] = useState(initialSessionForm);
  const [sessionFormErrors, setSessionFormErrors] = useState({});
  const [sessionSubmitting, setSessionSubmitting] = useState(false);
  const [sessionModalError, setSessionModalError] = useState(null);

  const [deleteSessionModal, setDeleteSessionModal] = useState({ open: false, item: null, loading: false, error: null });

  // Records
  const [recordModal, setRecordModal] = useState({ open: false, mode: "create", item: null });
  const [recordFormData, setRecordFormData] = useState(initialRecordForm);
  const [recordFormErrors, setRecordFormErrors] = useState({});
  const [recordSubmitting, setRecordSubmitting] = useState(false);
  const [recordModalError, setRecordModalError] = useState(null);

  const [deleteRecordModal, setDeleteRecordModal] = useState({ open: false, item: null, loading: false, error: null });

  // Auto-dismiss toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Load dropdown resources
  const loadDropdownData = async () => {
    try {
      const [sData, fData, sessData, studData] = await Promise.allSettled([
        academicService.getSubjects({ size: 100 }),
        authService.getFacultyUsers(),
        attendanceService.getSessions({ size: 100 }),
        studentService.getStudents({ size: 100 }),
      ]);

      if (sData.status === "fulfilled") setDropdownSubjects(sData.value?.content || []);
      if (fData.status === "fulfilled") setDropdownFaculty(fData.value || []);
      if (sessData.status === "fulfilled") setDropdownSessions(sessData.value?.content || []);
      if (studData.status === "fulfilled") setDropdownStudents(studData.value?.content || []);
    } catch {
      // Non-blocking fallback
    }
  };

  useEffect(() => {
    loadDropdownData();
  }, []);

  // ---- Fetch Sessions ----
  const fetchSessions = async (page = sessionsState.page, size = sessionsState.size, isRefresh = false) => {
    setSessionsState((prev) => ({
      ...prev,
      loading: !isRefresh,
      refreshing: isRefresh,
      error: null,
    }));

    try {
      const data = await attendanceService.getSessions({ page, size });
      setSessionsState((prev) => ({
        ...prev,
        items: data.content || [],
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
        page: data.number ?? page,
        size: data.size ?? size,
        loading: false,
        refreshing: false,
      }));
    } catch (err) {
      const msg =
        err.response?.status === 401
          ? "Session expired. Please log in again."
          : err.response?.status === 403
            ? "Access restricted."
            : "Attendance Service unreachable. Please check connection.";
      setSessionsState((prev) => ({
        ...prev,
        loading: false,
        refreshing: false,
        error: msg,
      }));
    }
  };

  // ---- Fetch Records ----
  const fetchRecords = async (
    page = recordsState.page,
    size = recordsState.size,
    status = recordsState.statusFilter,
    isRefresh = false
  ) => {
    setRecordsState((prev) => ({
      ...prev,
      loading: !isRefresh,
      refreshing: isRefresh,
      error: null,
    }));

    try {
      const data = await attendanceService.getRecords({
        page,
        size,
        status: status !== "ALL" ? status : undefined,
      });
      setRecordsState((prev) => ({
        ...prev,
        items: data.content || [],
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
        page: data.number ?? page,
        size: data.size ?? size,
        loading: false,
        refreshing: false,
      }));
    } catch (err) {
      const msg =
        err.response?.status === 401
          ? "Session expired. Please log in again."
          : err.response?.status === 403
            ? "Access restricted."
            : "Attendance Service unreachable. Please check connection.";
      setRecordsState((prev) => ({
        ...prev,
        loading: false,
        refreshing: false,
        error: msg,
      }));
    }
  };

  useEffect(() => {
    fetchSessions(0, sessionsState.size);
    fetchRecords(0, recordsState.size, recordsState.statusFilter);
  }, []);

  // Filtered Sessions
  const filteredSessions = useMemo(() => {
    const term = sessionsState.search.trim().toLowerCase();
    if (!term) return sessionsState.items;
    return sessionsState.items.filter((s) => {
      const sub = (s.subjectName || "").toLowerCase();
      const code = (s.subjectCode || "").toLowerCase();
      const room = (s.roomNumber || "").toLowerCase();
      const sec = (s.section || "").toLowerCase();
      const date = (s.sessionDate || "").toLowerCase();
      return (
        sub.includes(term) ||
        code.includes(term) ||
        room.includes(term) ||
        sec.includes(term) ||
        date.includes(term)
      );
    });
  }, [sessionsState.items, sessionsState.search]);

  // Filtered Records
  const filteredRecords = useMemo(() => {
    const term = recordsState.search.trim().toLowerCase();
    if (!term) return recordsState.items;
    return recordsState.items.filter((r) => {
      const sNum = (r.studentNumber || "").toLowerCase();
      const sName = (r.studentName || "").toLowerCase();
      const sub = (r.subjectName || "").toLowerCase();
      const remarks = (r.remarks || "").toLowerCase();
      return (
        sNum.includes(term) ||
        sName.includes(term) ||
        sub.includes(term) ||
        remarks.includes(term)
      );
    });
  }, [recordsState.items, recordsState.search]);

  const handleRefresh = () => {
    if (activeTab === "sessions") fetchSessions(sessionsState.page, sessionsState.size, true);
    else if (activeTab === "records") fetchRecords(recordsState.page, recordsState.size, recordsState.statusFilter, true);
  };

  const isCurrentRefreshing =
    (activeTab === "sessions" && sessionsState.refreshing) ||
    (activeTab === "records" && recordsState.refreshing);

  const isCurrentLoading =
    (activeTab === "sessions" && sessionsState.loading) ||
    (activeTab === "records" && recordsState.loading);

  // ==========================================
  // SESSION HANDLERS
  // ==========================================
  const handleOpenAddSession = () => {
    setSessionFormData({
      ...initialSessionForm,
      subjectId: dropdownSubjects[0]?.id || "",
      facultyUserId: dropdownFaculty[0]?.id || "",
    });
    setSessionFormErrors({});
    setSessionModalError(null);
    setSessionModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditSession = (sess) => {
    setSessionFormData({
      subjectId: sess.subjectId || "",
      facultyUserId: sess.facultyUserId || "",
      sessionDate: sess.sessionDate || new Date().toISOString().split("T")[0],
      startTime: sess.startTime ? String(sess.startTime).substring(0, 5) : "09:00",
      endTime: sess.endTime ? String(sess.endTime).substring(0, 5) : "10:30",
      roomNumber: sess.roomNumber || "",
      academicYear: sess.academicYear || "2026-2027",
      semester: sess.semester || 1,
      section: sess.section || "A",
    });
    setSessionFormErrors({});
    setSessionModalError(null);
    setSessionModal({ open: true, mode: "edit", item: sess });
  };

  const handleOpenViewSession = (sess) => {
    setSessionModal({ open: true, mode: "view", item: sess });
  };

  const validateSessionForm = () => {
    const errs = {};
    if (!sessionFormData.subjectId) errs.subjectId = "Please select a Subject";
    if (!sessionFormData.facultyUserId) errs.facultyUserId = "Please select a Faculty member";
    if (!sessionFormData.sessionDate) errs.sessionDate = "Session Date is required";
    if (!sessionFormData.startTime) errs.startTime = "Start Time is required";
    if (!sessionFormData.endTime) errs.endTime = "End Time is required";
    if (!sessionFormData.academicYear.trim()) errs.academicYear = "Academic Year is required";
    if (!sessionFormData.semester || Number(sessionFormData.semester) < 1) errs.semester = "Semester must be at least 1";
    setSessionFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitSession = async (e) => {
    e.preventDefault();
    if (sessionModal.mode === "view") return;
    if (!validateSessionForm()) return;

    setSessionSubmitting(true);
    setSessionModalError(null);

    const formatTimeToApi = (t) => (t && t.length === 5 ? `${t}:00` : t);

    try {
      if (sessionModal.mode === "create") {
        await attendanceService.createSession({
          subjectId: Number(sessionFormData.subjectId),
          facultyUserId: Number(sessionFormData.facultyUserId),
          sessionDate: sessionFormData.sessionDate,
          startTime: formatTimeToApi(sessionFormData.startTime),
          endTime: formatTimeToApi(sessionFormData.endTime),
          roomNumber: sessionFormData.roomNumber.trim() || undefined,
          academicYear: sessionFormData.academicYear.trim(),
          semester: Number(sessionFormData.semester),
          section: sessionFormData.section.trim() || undefined,
        });
        setToast({ message: "Attendance session scheduled successfully!", type: "success" });
      } else {
        await attendanceService.updateSession(sessionModal.item.id, {
          subjectId: Number(sessionFormData.subjectId),
          facultyUserId: Number(sessionFormData.facultyUserId),
          sessionDate: sessionFormData.sessionDate,
          startTime: formatTimeToApi(sessionFormData.startTime),
          endTime: formatTimeToApi(sessionFormData.endTime),
          roomNumber: sessionFormData.roomNumber.trim() || undefined,
          academicYear: sessionFormData.academicYear.trim(),
          semester: Number(sessionFormData.semester),
          section: sessionFormData.section.trim() || undefined,
        });
        setToast({ message: "Attendance session updated successfully!", type: "success" });
      }

      setSessionModal({ open: false, mode: "create", item: null });
      fetchSessions(sessionsState.page, sessionsState.size, true);
      loadDropdownData();
    } catch (err) {
      setSessionModalError(err.response?.data?.message || "Failed to save attendance session. Please check inputs.");
    } finally {
      setSessionSubmitting(false);
    }
  };

  const handleDeleteSession = async () => {
    if (!deleteSessionModal.item) return;
    setDeleteSessionModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await attendanceService.deleteSession(deleteSessionModal.item.id);
      setToast({ message: "Attendance session deleted successfully!", type: "success" });
      setDeleteSessionModal({ open: false, item: null, loading: false, error: null });
      fetchSessions(sessionsState.page, sessionsState.size, true);
      loadDropdownData();
    } catch (err) {
      setDeleteSessionModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete attendance session.",
      }));
    }
  };

  // ==========================================
  // RECORD HANDLERS
  // ==========================================
  const handleOpenAddRecord = () => {
    setRecordFormData({
      ...initialRecordForm,
      sessionId: dropdownSessions[0]?.id || "",
      studentId: dropdownStudents[0]?.id || "",
    });
    setRecordFormErrors({});
    setRecordModalError(null);
    setRecordModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditRecord = (rec) => {
    setRecordFormData({
      sessionId: rec.sessionId || "",
      studentId: rec.studentId || "",
      status: rec.status || "PRESENT",
      remarks: rec.remarks || "",
    });
    setRecordFormErrors({});
    setRecordModalError(null);
    setRecordModal({ open: true, mode: "edit", item: rec });
  };

  const handleOpenViewRecord = (rec) => {
    setRecordModal({ open: true, mode: "view", item: rec });
  };

  const validateRecordForm = () => {
    const errs = {};
    if (!recordFormData.sessionId) errs.sessionId = "Please select an Attendance Session";
    if (!recordFormData.studentId) errs.studentId = "Please select a Student";
    if (!recordFormData.status) errs.status = "Please select a Status";
    setRecordFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitRecord = async (e) => {
    e.preventDefault();
    if (recordModal.mode === "view") return;
    if (!validateRecordForm()) return;

    setRecordSubmitting(true);
    setRecordModalError(null);

    try {
      if (recordModal.mode === "create") {
        await attendanceService.createRecord({
          sessionId: Number(recordFormData.sessionId),
          studentId: Number(recordFormData.studentId),
          status: recordFormData.status,
          remarks: recordFormData.remarks.trim() || undefined,
        });
        setToast({ message: "Student attendance recorded successfully!", type: "success" });
      } else {
        await attendanceService.updateRecord(recordModal.item.id, {
          status: recordFormData.status,
          remarks: recordFormData.remarks.trim() || undefined,
        });
        setToast({ message: "Attendance record updated successfully!", type: "success" });
      }

      setRecordModal({ open: false, mode: "create", item: null });
      fetchRecords(recordsState.page, recordsState.size, recordsState.statusFilter, true);
    } catch (err) {
      setRecordModalError(err.response?.data?.message || "Failed to save attendance record. It may already be recorded.");
    } finally {
      setRecordSubmitting(false);
    }
  };

  const handleDeleteRecord = async () => {
    if (!deleteRecordModal.item) return;
    setDeleteRecordModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await attendanceService.deleteRecord(deleteRecordModal.item.id);
      setToast({ message: "Attendance record deleted successfully!", type: "success" });
      setDeleteRecordModal({ open: false, item: null, loading: false, error: null });
      fetchRecords(recordsState.page, recordsState.size, recordsState.statusFilter, true);
    } catch (err) {
      setDeleteRecordModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete attendance record.",
      }));
    }
  };

  return (
    <div className="attendance-page">
      {/* Toast Notification */}
      {toast && (
        <div className={`toast-notification ${toast.type}`}>
          <span>{toast.message}</span>
          <button className="toast-close-btn" onClick={() => setToast(null)}>✕</button>
        </div>
      )}

      {/* Header */}
      <div className="attendance-header">
        <div className="attendance-header-left">
          <div className="attendance-title-row">
            <h1>Attendance Management</h1>
          </div>
          <p className="attendance-subtitle">
            Schedule lecture attendance sessions, track student presence, and manage class roll call records.
          </p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="attendance-tabs" role="tablist">
        <button
          className={`attendance-tab-btn ${activeTab === "sessions" ? "active" : ""}`}
          onClick={() => setActiveTab("sessions")}
          role="tab"
          aria-selected={activeTab === "sessions"}
        >
          <span>Attendance Sessions</span>
          <span className="tab-badge">
            {sessionsState.loading ? "…" : sessionsState.totalElements}
          </span>
        </button>

        <button
          className={`attendance-tab-btn ${activeTab === "records" ? "active" : ""}`}
          onClick={() => setActiveTab("records")}
          role="tab"
          aria-selected={activeTab === "records"}
        >
          <span>Attendance Records</span>
          <span className="tab-badge">
            {recordsState.loading ? "…" : recordsState.totalElements}
          </span>
        </button>
      </div>

      {/* ==============================================================
          TAB 1: SESSIONS
          ============================================================== */}
      {activeTab === "sessions" && (
        <div className="tab-content">
          {sessionsState.error && (
            <div className="attendance-error-banner" role="alert">
              <div className="error-left">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{sessionsState.error}</span>
              </div>
              <button className="retry-btn" onClick={() => fetchSessions(sessionsState.page, sessionsState.size, true)}>
                Retry
              </button>
            </div>
          )}

          {/* Toolbar */}
          <div className="attendance-toolbar">
            <div className="attendance-search-wrapper">
              <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                className="attendance-search-input"
                placeholder="Search sessions by subject, room, date, section..."
                value={sessionsState.search}
                onChange={(e) => setSessionsState((prev) => ({ ...prev, search: e.target.value }))}
              />
              {sessionsState.search && (
                <button
                  className="search-clear-btn"
                  onClick={() => setSessionsState((prev) => ({ ...prev, search: "" }))}
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="attendance-toolbar-actions">
              {isAdminOrFaculty && (
                <button
                  className="btn-add-entity"
                  onClick={handleOpenAddSession}
                  id="btn-add-session"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <span>Add Session</span>
                </button>
              )}

              {sessionsState.search && (
                <span className="search-feedback">
                  Showing {filteredSessions.length} of {sessionsState.items.length}
                </span>
              )}
              <button
                className={`refresh-btn ${isCurrentRefreshing ? "spinning" : ""}`}
                onClick={handleRefresh}
                disabled={isCurrentLoading || isCurrentRefreshing}
                title="Refresh sessions"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                </svg>
                <span>{isCurrentRefreshing ? "Refreshing..." : "Refresh"}</span>
              </button>
            </div>
          </div>

          {/* Table Card */}
          <div className="attendance-table-card">
            <div className="table-responsive">
              <table className="attendance-table" id="sessions-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Time Slot</th>
                    <th>Subject</th>
                    <th>Room</th>
                    <th>Section</th>
                    <th>Semester</th>
                    <th style={{ textAlign: "right" }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sessionsState.loading ? (
                    Array.from({ length: 4 }).map((_, i) => (
                      <tr key={`sessions-skel-${i}`} className="skeleton-row">
                        <td><div className="skeleton-pulse" style={{ width: 90 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 130 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 180 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 70 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 45 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 50 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80, marginLeft: "auto" }} /></td>
                      </tr>
                    ))
                  ) : filteredSessions.length === 0 ? (
                    <tr>
                      <td colSpan="7">
                        <div className="attendance-empty">
                          <div className="empty-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="8" y1="12" x2="16" y2="12" />
                            </svg>
                          </div>
                          <h3>{sessionsState.search ? "No matching sessions" : "No attendance sessions found"}</h3>
                          <p>
                            {sessionsState.search
                              ? `No sessions matched "${sessionsState.search}".`
                              : "No class attendance sessions have been created yet."}
                          </p>
                          {isAdminOrFaculty && !sessionsState.search && (
                            <button className="empty-action-btn" onClick={handleOpenAddSession}>
                              + Add First Attendance Session
                            </button>
                          )}
                          {sessionsState.search && (
                            <button
                              className="empty-action-btn"
                              onClick={() => setSessionsState((prev) => ({ ...prev, search: "" }))}
                            >
                              Clear Search
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredSessions.map((sess) => (
                      <tr key={sess.id}>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>{sess.sessionDate}</strong>
                        </td>
                        <td>
                          <span className="time-pill">
                            {formatTime(sess.startTime)} – {formatTime(sess.endTime)}
                          </span>
                        </td>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>
                            {sess.subjectName || `Subject #${sess.subjectId}`}
                          </strong>
                          {sess.subjectCode && (
                            <span style={{ fontSize: 12, color: "var(--color-text-muted)", marginLeft: 6 }}>
                              ({sess.subjectCode})
                            </span>
                          )}
                        </td>
                        <td>{sess.roomNumber || "—"}</td>
                        <td>
                          <span className="secondary-pill">{sess.section ? `Sec ${sess.section}` : "—"}</span>
                        </td>
                        <td>Sem {sess.semester ?? "—"}</td>
                        <td style={{ textAlign: "right" }}>
                          <div className="action-buttons-cell" style={{ justifyContent: "flex-end" }}>
                            <button
                              className="btn-action-icon view"
                              title="View Details"
                              onClick={() => handleOpenViewSession(sess)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                              </svg>
                            </button>
                            {isAdminOrFaculty && (
                              <>
                                <button
                                  className="btn-action-icon edit"
                                  title="Edit Session"
                                  onClick={() => handleOpenEditSession(sess)}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                                  </svg>
                                </button>
                                <button
                                  className="btn-action-icon delete"
                                  title="Delete Session"
                                  onClick={() => setDeleteSessionModal({ open: true, item: sess, loading: false, error: null })}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <polyline points="3 6 5 6 21 6" />
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                  </svg>
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!sessionsState.loading && sessionsState.totalPages > 1 && (
              <div className="attendance-pagination">
                <span className="pagination-info">
                  Page {sessionsState.page + 1} of {sessionsState.totalPages} ({sessionsState.totalElements} sessions)
                </span>
                <div className="pagination-buttons">
                  <button
                    className="pagination-btn"
                    disabled={sessionsState.page === 0}
                    onClick={() => fetchSessions(sessionsState.page - 1, sessionsState.size)}
                  >
                    Previous
                  </button>
                  <button
                    className="pagination-btn"
                    disabled={sessionsState.page >= sessionsState.totalPages - 1}
                    onClick={() => fetchSessions(sessionsState.page + 1, sessionsState.size)}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ==============================================================
          TAB 2: RECORDS
          ============================================================== */}
      {activeTab === "records" && (
        <div className="tab-content">
          {recordsState.error && (
            <div className="attendance-error-banner" role="alert">
              <div className="error-left">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{recordsState.error}</span>
              </div>
              <button className="retry-btn" onClick={() => fetchRecords(recordsState.page, recordsState.size, recordsState.statusFilter, true)}>
                Retry
              </button>
            </div>
          )}

          {/* Toolbar */}
          <div className="attendance-toolbar">
            <div className="attendance-toolbar-left" style={{ display: "flex", gap: 12, flexWrap: "wrap", flex: 1 }}>
              <div className="attendance-search-wrapper" style={{ flex: 1, minWidth: 220 }}>
                <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  type="text"
                  className="attendance-search-input"
                  placeholder="Search records by student name, number, remarks..."
                  value={recordsState.search}
                  onChange={(e) => setRecordsState((prev) => ({ ...prev, search: e.target.value }))}
                />
                {recordsState.search && (
                  <button
                    className="search-clear-btn"
                    onClick={() => setRecordsState((prev) => ({ ...prev, search: "" }))}
                    aria-label="Clear search"
                  >
                    ✕
                  </button>
                )}
              </div>

              <select
                className="form-select"
                style={{ width: "auto", minWidth: 150 }}
                value={recordsState.statusFilter}
                onChange={(e) => {
                  const newStat = e.target.value;
                  setRecordsState((prev) => ({ ...prev, statusFilter: newStat }));
                  fetchRecords(0, recordsState.size, newStat, true);
                }}
              >
                <option value="ALL">All Statuses</option>
                <option value="PRESENT">Present</option>
                <option value="ABSENT">Absent</option>
                <option value="LATE">Late</option>
                <option value="EXCUSED">Excused</option>
              </select>
            </div>

            <div className="attendance-toolbar-actions">
              {isAdminOrFaculty && (
                <button
                  className="btn-add-entity"
                  onClick={handleOpenAddRecord}
                  id="btn-mark-attendance"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <span>Mark Attendance</span>
                </button>
              )}

              {recordsState.search && (
                <span className="search-feedback">
                  Showing {filteredRecords.length} of {recordsState.items.length}
                </span>
              )}
              <button
                className={`refresh-btn ${isCurrentRefreshing ? "spinning" : ""}`}
                onClick={handleRefresh}
                disabled={isCurrentLoading || isCurrentRefreshing}
                title="Refresh records"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                </svg>
                <span>{isCurrentRefreshing ? "Refreshing..." : "Refresh"}</span>
              </button>
            </div>
          </div>

          {/* Table Card */}
          <div className="attendance-table-card">
            <div className="table-responsive">
              <table className="attendance-table" id="records-table">
                <thead>
                  <tr>
                    <th>Student Number</th>
                    <th>Student Name</th>
                    <th>Subject</th>
                    <th>Date Marked</th>
                    <th>Status</th>
                    <th>Remarks</th>
                    <th style={{ textAlign: "right" }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {recordsState.loading ? (
                    Array.from({ length: 4 }).map((_, i) => (
                      <tr key={`records-skel-${i}`} className="skeleton-row">
                        <td><div className="skeleton-pulse" style={{ width: 100 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 150 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 140 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 110 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 70 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 120 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80, marginLeft: "auto" }} /></td>
                      </tr>
                    ))
                  ) : filteredRecords.length === 0 ? (
                    <tr>
                      <td colSpan="7">
                        <div className="attendance-empty">
                          <div className="empty-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="8" y1="12" x2="16" y2="12" />
                            </svg>
                          </div>
                          <h3>{recordsState.search ? "No matching records" : "No attendance records found"}</h3>
                          <p>
                            {recordsState.search
                              ? `No attendance records matched "${recordsState.search}".`
                              : "No student roll call records have been submitted."}
                          </p>
                          {isAdminOrFaculty && !recordsState.search && (
                            <button className="empty-action-btn" onClick={handleOpenAddRecord}>
                              + Mark First Attendance
                            </button>
                          )}
                          {recordsState.search && (
                            <button
                              className="empty-action-btn"
                              onClick={() => setRecordsState((prev) => ({ ...prev, search: "" }))}
                            >
                              Clear Search
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredRecords.map((rec) => (
                      <tr key={rec.id}>
                        <td>
                          <span className="code-badge">{rec.studentNumber || `Student #${rec.studentId}`}</span>
                        </td>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>{rec.studentName || "—"}</strong>
                        </td>
                        <td>{rec.subjectName || `Session #${rec.sessionId}`}</td>
                        <td>{formatDateTime(rec.markedAt)}</td>
                        <td>
                          <span className={`status-pill status-${(rec.status || "present").toLowerCase()}`}>
                            {rec.status || "PRESENT"}
                          </span>
                        </td>
                        <td>
                          <span style={{ color: rec.remarks ? "var(--color-text)" : "var(--color-text-muted)" }}>
                            {rec.remarks || "—"}
                          </span>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <div className="action-buttons-cell" style={{ justifyContent: "flex-end" }}>
                            <button
                              className="btn-action-icon view"
                              title="View Details"
                              onClick={() => handleOpenViewRecord(rec)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                              </svg>
                            </button>
                            {isAdminOrFaculty && (
                              <>
                                <button
                                  className="btn-action-icon edit"
                                  title="Edit Record"
                                  onClick={() => handleOpenEditRecord(rec)}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                                  </svg>
                                </button>
                                <button
                                  className="btn-action-icon delete"
                                  title="Delete Record"
                                  onClick={() => setDeleteRecordModal({ open: true, item: rec, loading: false, error: null })}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <polyline points="3 6 5 6 21 6" />
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                  </svg>
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!recordsState.loading && recordsState.totalPages > 1 && (
              <div className="attendance-pagination">
                <span className="pagination-info">
                  Page {recordsState.page + 1} of {recordsState.totalPages} ({recordsState.totalElements} records)
                </span>
                <div className="pagination-buttons">
                  <button
                    className="pagination-btn"
                    disabled={recordsState.page === 0}
                    onClick={() => fetchRecords(recordsState.page - 1, recordsState.size, recordsState.statusFilter)}
                  >
                    Previous
                  </button>
                  <button
                    className="pagination-btn"
                    disabled={recordsState.page >= recordsState.totalPages - 1}
                    onClick={() => fetchRecords(recordsState.page + 1, recordsState.size, recordsState.statusFilter)}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ==============================================================
          SESSION MODALS (ADD / EDIT / VIEW / DELETE)
          ============================================================== */}
      {sessionModal.open && (
        <div className="modal-overlay" onClick={() => setSessionModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h2>
                  {sessionModal.mode === "create" && "Schedule Attendance Session"}
                  {sessionModal.mode === "edit" && `Edit Session: ${sessionModal.item?.subjectName || ""}`}
                  {sessionModal.mode === "view" && `Attendance Session Details`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setSessionModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            {sessionModal.mode === "view" ? (
              <div className="modal-body">
                <div className="modal-view-row">
                  <span className="modal-view-label">Subject</span>
                  <span className="modal-view-value">{sessionModal.item?.subjectName} ({sessionModal.item?.subjectCode})</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Session Date</span>
                  <span className="modal-view-value">{sessionModal.item?.sessionDate}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Time Slot</span>
                  <span className="modal-view-value">{formatTime(sessionModal.item?.startTime)} – {formatTime(sessionModal.item?.endTime)}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Room Number</span>
                  <span className="modal-view-value">{sessionModal.item?.roomNumber || "—"}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Section & Sem</span>
                  <span className="modal-view-value">Section {sessionModal.item?.section || "A"} • Semester {sessionModal.item?.semester}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Academic Year</span>
                  <span className="modal-view-value">{sessionModal.item?.academicYear}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Faculty User</span>
                  <span className="modal-view-value">Faculty ID #{sessionModal.item?.facultyUserId}</span>
                </div>
                <div className="modal-footer">
                  <button
                    className="btn-modal-cancel"
                    onClick={() => setSessionModal({ open: false, mode: "create", item: null })}
                  >
                    Close
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmitSession}>
                <div className="modal-body">
                  {sessionModalError && (
                    <div className="modal-alert-error" role="alert">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{sessionModalError}</span>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label" htmlFor="sess-subject">
                      Subject <span className="required-star">*</span>
                    </label>
                    <select
                      id="sess-subject"
                      className={`form-select ${sessionFormErrors.subjectId ? "input-error" : ""}`}
                      value={sessionFormData.subjectId}
                      onChange={(e) => setSessionFormData({ ...sessionFormData, subjectId: e.target.value })}
                      disabled={sessionSubmitting}
                    >
                      <option value="">Select a Subject...</option>
                      {dropdownSubjects.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.subjectCode} - {s.subjectName}
                        </option>
                      ))}
                    </select>
                    {sessionFormErrors.subjectId && <span className="form-error-msg">{sessionFormErrors.subjectId}</span>}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="sess-faculty">
                      Faculty Member <span className="required-star">*</span>
                    </label>
                    <select
                      id="sess-faculty"
                      className={`form-select ${sessionFormErrors.facultyUserId ? "input-error" : ""}`}
                      value={sessionFormData.facultyUserId}
                      onChange={(e) => setSessionFormData({ ...sessionFormData, facultyUserId: e.target.value })}
                      disabled={sessionSubmitting}
                    >
                      <option value="">Select Faculty...</option>
                      {dropdownFaculty.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.firstName} {f.lastName} ({f.username})
                        </option>
                      ))}
                    </select>
                    {sessionFormErrors.facultyUserId && <span className="form-error-msg">{sessionFormErrors.facultyUserId}</span>}
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-date">
                        Session Date <span className="required-star">*</span>
                      </label>
                      <input
                        id="sess-date"
                        type="date"
                        className={`form-input ${sessionFormErrors.sessionDate ? "input-error" : ""}`}
                        value={sessionFormData.sessionDate}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, sessionDate: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                      {sessionFormErrors.sessionDate && <span className="form-error-msg">{sessionFormErrors.sessionDate}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-room">
                        Room Number
                      </label>
                      <input
                        id="sess-room"
                        type="text"
                        className="form-input"
                        placeholder="e.g. Lab-204"
                        value={sessionFormData.roomNumber}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, roomNumber: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                    </div>
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-start">
                        Start Time <span className="required-star">*</span>
                      </label>
                      <input
                        id="sess-start"
                        type="time"
                        className={`form-input ${sessionFormErrors.startTime ? "input-error" : ""}`}
                        value={sessionFormData.startTime}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, startTime: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                      {sessionFormErrors.startTime && <span className="form-error-msg">{sessionFormErrors.startTime}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-end">
                        End Time <span className="required-star">*</span>
                      </label>
                      <input
                        id="sess-end"
                        type="time"
                        className={`form-input ${sessionFormErrors.endTime ? "input-error" : ""}`}
                        value={sessionFormData.endTime}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, endTime: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                      {sessionFormErrors.endTime && <span className="form-error-msg">{sessionFormErrors.endTime}</span>}
                    </div>
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-ay">
                        Academic Year <span className="required-star">*</span>
                      </label>
                      <input
                        id="sess-ay"
                        type="text"
                        className={`form-input ${sessionFormErrors.academicYear ? "input-error" : ""}`}
                        placeholder="e.g. 2026-2027"
                        value={sessionFormData.academicYear}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, academicYear: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                      {sessionFormErrors.academicYear && <span className="form-error-msg">{sessionFormErrors.academicYear}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sess-sem">
                        Semester <span className="required-star">*</span>
                      </label>
                      <input
                        id="sess-sem"
                        type="number"
                        min="1"
                        max="12"
                        className={`form-input ${sessionFormErrors.semester ? "input-error" : ""}`}
                        value={sessionFormData.semester}
                        onChange={(e) => setSessionFormData({ ...sessionFormData, semester: e.target.value })}
                        disabled={sessionSubmitting}
                      />
                      {sessionFormErrors.semester && <span className="form-error-msg">{sessionFormErrors.semester}</span>}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="sess-section">
                      Section
                    </label>
                    <input
                      id="sess-section"
                      type="text"
                      className="form-input"
                      placeholder="e.g. A"
                      value={sessionFormData.section}
                      onChange={(e) => setSessionFormData({ ...sessionFormData, section: e.target.value })}
                      disabled={sessionSubmitting}
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn-modal-cancel"
                    onClick={() => setSessionModal({ open: false, mode: "create", item: null })}
                    disabled={sessionSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-modal-submit"
                    disabled={sessionSubmitting}
                  >
                    {sessionSubmitting ? "Saving..." : sessionModal.mode === "create" ? "Schedule Session" : "Save Changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Delete Session Dialog */}
      {deleteSessionModal.open && (
        <div className="modal-overlay" onClick={() => !deleteSessionModal.loading && setDeleteSessionModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>Confirm Deletion</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteSessionModal.loading}
                onClick={() => setDeleteSessionModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteSessionModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteSessionModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete the attendance session on <strong>{deleteSessionModal.item?.sessionDate}</strong> for <strong>{deleteSessionModal.item?.subjectName}</strong>?
              </p>
              <p style={{ color: "var(--color-text-muted)", fontSize: 13, margin: 0 }}>
                This will also delete any attendance records marked for this session.
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteSessionModal.loading}
                onClick={() => setDeleteSessionModal({ open: false, item: null, loading: false, error: null })}
              >
                Cancel
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteSessionModal.loading}
                onClick={handleDeleteSession}
              >
                {deleteSessionModal.loading ? "Deleting..." : "Delete Session"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==============================================================
          RECORD MODALS (ADD / EDIT / VIEW / DELETE)
          ============================================================== */}
      {recordModal.open && (
        <div className="modal-overlay" onClick={() => setRecordModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h2>
                  {recordModal.mode === "create" && "Mark Student Attendance"}
                  {recordModal.mode === "edit" && `Update Attendance: ${recordModal.item?.studentName || ""}`}
                  {recordModal.mode === "view" && `Attendance Record Details`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setRecordModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            {recordModal.mode === "view" ? (
              <div className="modal-body">
                <div className="modal-view-row">
                  <span className="modal-view-label">Student</span>
                  <span className="modal-view-value">{recordModal.item?.studentName} (<span className="code-badge">{recordModal.item?.studentNumber}</span>)</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Subject</span>
                  <span className="modal-view-value">{recordModal.item?.subjectName}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Status</span>
                  <span className="modal-view-value">
                    <span className={`status-pill status-${(recordModal.item?.status || "present").toLowerCase()}`}>
                      {recordModal.item?.status || "PRESENT"}
                    </span>
                  </span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Marked At</span>
                  <span className="modal-view-value">{formatDateTime(recordModal.item?.markedAt)}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Remarks</span>
                  <span className="modal-view-value">{recordModal.item?.remarks || "None"}</span>
                </div>
                <div className="modal-footer">
                  <button
                    className="btn-modal-cancel"
                    onClick={() => setRecordModal({ open: false, mode: "create", item: null })}
                  >
                    Close
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmitRecord}>
                <div className="modal-body">
                  {recordModalError && (
                    <div className="modal-alert-error" role="alert">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{recordModalError}</span>
                    </div>
                  )}

                  {recordModal.mode === "create" ? (
                    <>
                      <div className="form-group">
                        <label className="form-label" htmlFor="rec-session">
                          Attendance Session <span className="required-star">*</span>
                        </label>
                        <select
                          id="rec-session"
                          className={`form-select ${recordFormErrors.sessionId ? "input-error" : ""}`}
                          value={recordFormData.sessionId}
                          onChange={(e) => setRecordFormData({ ...recordFormData, sessionId: e.target.value })}
                          disabled={recordSubmitting}
                        >
                          <option value="">Select a Session...</option>
                          {dropdownSessions.map((sess) => (
                            <option key={sess.id} value={sess.id}>
                              {sess.sessionDate} • {sess.subjectName || `Subject #${sess.subjectId}`} (Sec {sess.section || "A"})
                            </option>
                          ))}
                        </select>
                        {recordFormErrors.sessionId && <span className="form-error-msg">{recordFormErrors.sessionId}</span>}
                      </div>

                      <div className="form-group">
                        <label className="form-label" htmlFor="rec-student">
                          Student <span className="required-star">*</span>
                        </label>
                        <select
                          id="rec-student"
                          className={`form-select ${recordFormErrors.studentId ? "input-error" : ""}`}
                          value={recordFormData.studentId}
                          onChange={(e) => setRecordFormData({ ...recordFormData, studentId: e.target.value })}
                          disabled={recordSubmitting}
                        >
                          <option value="">Select a Student...</option>
                          {dropdownStudents.map((st) => (
                            <option key={st.id} value={st.id}>
                              {st.studentNumber} - {st.firstName} {st.lastName}
                            </option>
                          ))}
                        </select>
                        {recordFormErrors.studentId && <span className="form-error-msg">{recordFormErrors.studentId}</span>}
                      </div>
                    </>
                  ) : (
                    <div className="form-group">
                      <span className="form-label">Student</span>
                      <strong style={{ color: "var(--color-text-heading)" }}>
                        {recordModal.item?.studentName} ({recordModal.item?.studentNumber})
                      </strong>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label" htmlFor="rec-status">
                      Attendance Status <span className="required-star">*</span>
                    </label>
                    <select
                      id="rec-status"
                      className="form-select"
                      value={recordFormData.status}
                      onChange={(e) => setRecordFormData({ ...recordFormData, status: e.target.value })}
                      disabled={recordSubmitting}
                    >
                      <option value="PRESENT">Present</option>
                      <option value="ABSENT">Absent</option>
                      <option value="LATE">Late</option>
                      <option value="EXCUSED">Excused</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="rec-remarks">
                      Remarks / Notes
                    </label>
                    <input
                      id="rec-remarks"
                      type="text"
                      className="form-input"
                      placeholder="e.g. On-time arrival, Medical excuse"
                      value={recordFormData.remarks}
                      onChange={(e) => setRecordFormData({ ...recordFormData, remarks: e.target.value })}
                      disabled={recordSubmitting}
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn-modal-cancel"
                    onClick={() => setRecordModal({ open: false, mode: "create", item: null })}
                    disabled={recordSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-modal-submit"
                    disabled={recordSubmitting}
                  >
                    {recordSubmitting ? "Saving..." : recordModal.mode === "create" ? "Record Attendance" : "Save Changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Delete Record Dialog */}
      {deleteRecordModal.open && (
        <div className="modal-overlay" onClick={() => !deleteRecordModal.loading && setDeleteRecordModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>Confirm Deletion</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteRecordModal.loading}
                onClick={() => setDeleteRecordModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteRecordModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteRecordModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete the attendance record for <strong>{deleteRecordModal.item?.studentName}</strong>?
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteRecordModal.loading}
                onClick={() => setDeleteRecordModal({ open: false, item: null, loading: false, error: null })}
              >
                Cancel
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteRecordModal.loading}
                onClick={handleDeleteRecord}
              >
                {deleteRecordModal.loading ? "Deleting..." : "Delete Record"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AttendancePage;
