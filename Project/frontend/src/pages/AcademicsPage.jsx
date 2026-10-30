import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import academicService from "../services/academicService";
import authService from "../services/authService";
import "./Academics.css";

/**
 * Time formatter (converts "09:30:00" or "09:30" to "9:30 AM")
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
 * Format Day of Week nicely (e.g. "MONDAY" -> "Monday")
 */
const formatDay = (day) => {
  if (!day) return "—";
  return day.charAt(0).toUpperCase() + day.slice(1).toLowerCase();
};

const DAYS_OF_WEEK = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const initialCourseForm = {
  courseCode: "",
  courseName: "",
  department: "",
  durationYears: 4,
  description: "",
};

const initialSubjectForm = {
  courseId: "",
  subjectCode: "",
  subjectName: "",
  credits: 3,
  semester: 1,
  facultyUserId: "",
};

const initialScheduleForm = {
  subjectId: "",
  facultyUserId: "",
  dayOfWeek: "MONDAY",
  startTime: "09:00",
  endTime: "10:30",
  roomNumber: "",
  academicYear: "2026-2027",
  semester: 1,
  section: "A",
};

/**
 * AcademicsPage — Comprehensive Academic Management module.
 * Features 3 tabs: Courses, Subjects, and Class Schedules.
 * Displays real API data, pagination, search, full CRUD modals, and role-based access.
 */
const AcademicsPage = () => {
  const { user } = useAuth();
  const role = user?.role;
  const isAdmin = role === "ADMIN";
  const isFaculty = role === "FACULTY";
  const isAdminOrFaculty = isAdmin || isFaculty;

  const [activeTab, setActiveTab] = useState("courses");
  const [toast, setToast] = useState(null); // { message, type: 'success' | 'error' }

  // Shared dropdown data for modal selects
  const [dropdownCourses, setDropdownCourses] = useState([]);
  const [dropdownSubjects, setDropdownSubjects] = useState([]);
  const [dropdownFaculty, setDropdownFaculty] = useState([]);

  // Tab data states
  const [coursesState, setCoursesState] = useState({
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

  const [subjectsState, setSubjectsState] = useState({
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

  const [schedulesState, setSchedulesState] = useState({
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

  // ---- Modal States ----
  // Courses
  const [courseModal, setCourseModal] = useState({ open: false, mode: "create", item: null });
  const [courseFormData, setCourseFormData] = useState(initialCourseForm);
  const [courseFormErrors, setCourseFormErrors] = useState({});
  const [courseSubmitting, setCourseSubmitting] = useState(false);
  const [courseModalError, setCourseModalError] = useState(null);

  const [deleteCourseModal, setDeleteCourseModal] = useState({ open: false, item: null, loading: false, error: null });

  // Subjects
  const [subjectModal, setSubjectModal] = useState({ open: false, mode: "create", item: null });
  const [subjectFormData, setSubjectFormData] = useState(initialSubjectForm);
  const [subjectFormErrors, setSubjectFormErrors] = useState({});
  const [subjectSubmitting, setSubjectSubmitting] = useState(false);
  const [subjectModalError, setSubjectModalError] = useState(null);

  const [deleteSubjectModal, setDeleteSubjectModal] = useState({ open: false, item: null, loading: false, error: null });

  // Schedules
  const [scheduleModal, setScheduleModal] = useState({ open: false, mode: "create", item: null });
  const [scheduleFormData, setScheduleFormData] = useState(initialScheduleForm);
  const [scheduleFormErrors, setScheduleFormErrors] = useState({});
  const [scheduleSubmitting, setScheduleSubmitting] = useState(false);
  const [scheduleModalError, setScheduleModalError] = useState(null);

  const [deleteScheduleModal, setDeleteScheduleModal] = useState({ open: false, item: null, loading: false, error: null });

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
      const [cData, sData, fData] = await Promise.allSettled([
        academicService.getCourses({ size: 100 }),
        academicService.getSubjects({ size: 100 }),
        authService.getFacultyUsers(),
      ]);

      if (cData.status === "fulfilled") setDropdownCourses(cData.value?.content || []);
      if (sData.status === "fulfilled") setDropdownSubjects(sData.value?.content || []);
      if (fData.status === "fulfilled") setDropdownFaculty(fData.value || []);
    } catch {
      // Non-blocking fallback
    }
  };

  useEffect(() => {
    loadDropdownData();
  }, []);

  // ---- Fetch Courses ----
  const fetchCourses = async (page = coursesState.page, size = coursesState.size, isRefresh = false) => {
    setCoursesState((prev) => ({
      ...prev,
      loading: !isRefresh,
      refreshing: isRefresh,
      error: null,
    }));

    try {
      const data = await academicService.getCourses({ page, size });
      setCoursesState((prev) => ({
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
            : "Academic Service unreachable. Please check connection.";
      setCoursesState((prev) => ({
        ...prev,
        loading: false,
        refreshing: false,
        error: msg,
      }));
    }
  };

  // ---- Fetch Subjects ----
  const fetchSubjects = async (page = subjectsState.page, size = subjectsState.size, isRefresh = false) => {
    setSubjectsState((prev) => ({
      ...prev,
      loading: !isRefresh,
      refreshing: isRefresh,
      error: null,
    }));

    try {
      const data = await academicService.getSubjects({ page, size });
      setSubjectsState((prev) => ({
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
            : "Academic Service unreachable. Please check connection.";
      setSubjectsState((prev) => ({
        ...prev,
        loading: false,
        refreshing: false,
        error: msg,
      }));
    }
  };

  // ---- Fetch Schedules ----
  const fetchSchedules = async (page = schedulesState.page, size = schedulesState.size, isRefresh = false) => {
    setSchedulesState((prev) => ({
      ...prev,
      loading: !isRefresh,
      refreshing: isRefresh,
      error: null,
    }));

    try {
      const data = await academicService.getSchedules({ page, size });
      setSchedulesState((prev) => ({
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
            : "Academic Service unreachable. Please check connection.";
      setSchedulesState((prev) => ({
        ...prev,
        loading: false,
        refreshing: false,
        error: msg,
      }));
    }
  };

  useEffect(() => {
    fetchCourses(0, coursesState.size);
    fetchSubjects(0, subjectsState.size);
    fetchSchedules(0, schedulesState.size);
  }, []);

  // Filtered Items (Client-side Search)
  const filteredCourses = useMemo(() => {
    const term = coursesState.search.trim().toLowerCase();
    if (!term) return coursesState.items;
    return coursesState.items.filter((c) => {
      const code = (c.courseCode || "").toLowerCase();
      const name = (c.courseName || "").toLowerCase();
      const dept = (c.department || "").toLowerCase();
      return code.includes(term) || name.includes(term) || dept.includes(term);
    });
  }, [coursesState.items, coursesState.search]);

  const filteredSubjects = useMemo(() => {
    const term = subjectsState.search.trim().toLowerCase();
    if (!term) return subjectsState.items;
    return subjectsState.items.filter((s) => {
      const code = (s.subjectCode || "").toLowerCase();
      const name = (s.subjectName || "").toLowerCase();
      const cCode = (s.courseCode || "").toLowerCase();
      const cName = (s.courseName || "").toLowerCase();
      return code.includes(term) || name.includes(term) || cCode.includes(term) || cName.includes(term);
    });
  }, [subjectsState.items, subjectsState.search]);

  const filteredSchedules = useMemo(() => {
    const term = schedulesState.search.trim().toLowerCase();
    if (!term) return schedulesState.items;
    return schedulesState.items.filter((sch) => {
      const code = (sch.subjectCode || "").toLowerCase();
      const name = (sch.subjectName || "").toLowerCase();
      const day = (sch.dayOfWeek || "").toLowerCase();
      const room = (sch.roomNumber || "").toLowerCase();
      const section = (sch.section || "").toLowerCase();
      return (
        code.includes(term) ||
        name.includes(term) ||
        day.includes(term) ||
        room.includes(term) ||
        section.includes(term)
      );
    });
  }, [schedulesState.items, schedulesState.search]);

  // Current active tab refresh handler
  const handleRefresh = () => {
    if (activeTab === "courses") fetchCourses(coursesState.page, coursesState.size, true);
    else if (activeTab === "subjects") fetchSubjects(subjectsState.page, subjectsState.size, true);
    else if (activeTab === "schedules") fetchSchedules(schedulesState.page, schedulesState.size, true);
  };

  const isCurrentRefreshing =
    (activeTab === "courses" && coursesState.refreshing) ||
    (activeTab === "subjects" && subjectsState.refreshing) ||
    (activeTab === "schedules" && schedulesState.refreshing);

  const isCurrentLoading =
    (activeTab === "courses" && coursesState.loading) ||
    (activeTab === "subjects" && subjectsState.loading) ||
    (activeTab === "schedules" && schedulesState.loading);

  // ==========================================
  // COURSE HANDLERS
  // ==========================================
  const handleOpenAddCourse = () => {
    setCourseFormData(initialCourseForm);
    setCourseFormErrors({});
    setCourseModalError(null);
    setCourseModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditCourse = (course) => {
    setCourseFormData({
      courseCode: course.courseCode || "",
      courseName: course.courseName || "",
      department: course.department || "",
      durationYears: course.durationYears || 4,
      description: course.description || "",
    });
    setCourseFormErrors({});
    setCourseModalError(null);
    setCourseModal({ open: true, mode: "edit", item: course });
  };

  const handleOpenViewCourse = (course) => {
    setCourseModal({ open: true, mode: "view", item: course });
  };

  const validateCourseForm = () => {
    const errs = {};
    if (!courseFormData.courseCode.trim()) errs.courseCode = "Course Code is required";
    else if (courseFormData.courseCode.trim().length < 2) errs.courseCode = "Must be at least 2 characters";
    if (!courseFormData.courseName.trim()) errs.courseName = "Course Name is required";
    if (!courseFormData.durationYears || Number(courseFormData.durationYears) < 1) {
      errs.durationYears = "Duration must be at least 1 year";
    }
    setCourseFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitCourse = async (e) => {
    e.preventDefault();
    if (courseModal.mode === "view") return;
    if (!validateCourseForm()) return;

    setCourseSubmitting(true);
    setCourseModalError(null);

    try {
      if (courseModal.mode === "create") {
        await academicService.createCourse({
          courseCode: courseFormData.courseCode.trim().toUpperCase(),
          courseName: courseFormData.courseName.trim(),
          department: courseFormData.department.trim() || undefined,
          durationYears: Number(courseFormData.durationYears),
          description: courseFormData.description.trim() || undefined,
        });
        setToast({ message: `Course "${courseFormData.courseCode.trim().toUpperCase()}" created successfully!`, type: "success" });
      } else {
        await academicService.updateCourse(courseModal.item.id, {
          courseName: courseFormData.courseName.trim(),
          department: courseFormData.department.trim() || undefined,
          durationYears: Number(courseFormData.durationYears),
          description: courseFormData.description.trim() || undefined,
        });
        setToast({ message: `Course "${courseModal.item.courseCode}" updated successfully!`, type: "success" });
      }

      setCourseModal({ open: false, mode: "create", item: null });
      fetchCourses(coursesState.page, coursesState.size, true);
      loadDropdownData();
    } catch (err) {
      setCourseModalError(err.response?.data?.message || "Failed to save course. Please check inputs.");
    } finally {
      setCourseSubmitting(false);
    }
  };

  const handleDeleteCourse = async () => {
    if (!deleteCourseModal.item) return;
    setDeleteCourseModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await academicService.deleteCourse(deleteCourseModal.item.id);
      setToast({ message: `Course "${deleteCourseModal.item.courseCode}" deleted successfully!`, type: "success" });
      setDeleteCourseModal({ open: false, item: null, loading: false, error: null });
      fetchCourses(coursesState.page, coursesState.size, true);
      loadDropdownData();
    } catch (err) {
      setDeleteCourseModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete course.",
      }));
    }
  };

  // ==========================================
  // SUBJECT HANDLERS
  // ==========================================
  const handleOpenAddSubject = () => {
    setSubjectFormData({
      ...initialSubjectForm,
      courseId: dropdownCourses[0]?.id || "",
      facultyUserId: dropdownFaculty[0]?.id || "",
    });
    setSubjectFormErrors({});
    setSubjectModalError(null);
    setSubjectModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditSubject = (subj) => {
    setSubjectFormData({
      courseId: subj.courseId || "",
      subjectCode: subj.subjectCode || "",
      subjectName: subj.subjectName || "",
      credits: subj.credits || 3,
      semester: subj.semester || 1,
      facultyUserId: subj.facultyUserId || "",
    });
    setSubjectFormErrors({});
    setSubjectModalError(null);
    setSubjectModal({ open: true, mode: "edit", item: subj });
  };

  const handleOpenViewSubject = (subj) => {
    setSubjectModal({ open: true, mode: "view", item: subj });
  };

  const validateSubjectForm = () => {
    const errs = {};
    if (!subjectFormData.courseId) errs.courseId = "Please select a Course";
    if (!subjectFormData.subjectCode.trim()) errs.subjectCode = "Subject Code is required";
    if (!subjectFormData.subjectName.trim()) errs.subjectName = "Subject Name is required";
    if (!subjectFormData.credits || Number(subjectFormData.credits) < 1) errs.credits = "Credits must be at least 1";
    if (!subjectFormData.semester || Number(subjectFormData.semester) < 1) errs.semester = "Semester must be at least 1";
    setSubjectFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitSubject = async (e) => {
    e.preventDefault();
    if (subjectModal.mode === "view") return;
    if (!validateSubjectForm()) return;

    setSubjectSubmitting(true);
    setSubjectModalError(null);

    try {
      if (subjectModal.mode === "create") {
        await academicService.createSubject({
          courseId: Number(subjectFormData.courseId),
          subjectCode: subjectFormData.subjectCode.trim().toUpperCase(),
          subjectName: subjectFormData.subjectName.trim(),
          credits: Number(subjectFormData.credits),
          semester: Number(subjectFormData.semester),
          facultyUserId: subjectFormData.facultyUserId ? Number(subjectFormData.facultyUserId) : undefined,
        });
        setToast({ message: `Subject "${subjectFormData.subjectCode.trim().toUpperCase()}" created successfully!`, type: "success" });
      } else {
        await academicService.updateSubject(subjectModal.item.id, {
          subjectName: subjectFormData.subjectName.trim(),
          credits: Number(subjectFormData.credits),
          semester: Number(subjectFormData.semester),
          facultyUserId: subjectFormData.facultyUserId ? Number(subjectFormData.facultyUserId) : undefined,
        });
        setToast({ message: `Subject "${subjectModal.item.subjectCode}" updated successfully!`, type: "success" });
      }

      setSubjectModal({ open: false, mode: "create", item: null });
      fetchSubjects(subjectsState.page, subjectsState.size, true);
      loadDropdownData();
    } catch (err) {
      setSubjectModalError(err.response?.data?.message || "Failed to save subject. Please check inputs.");
    } finally {
      setSubjectSubmitting(false);
    }
  };

  const handleDeleteSubject = async () => {
    if (!deleteSubjectModal.item) return;
    setDeleteSubjectModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await academicService.deleteSubject(deleteSubjectModal.item.id);
      setToast({ message: `Subject "${deleteSubjectModal.item.subjectCode}" deleted successfully!`, type: "success" });
      setDeleteSubjectModal({ open: false, item: null, loading: false, error: null });
      fetchSubjects(subjectsState.page, subjectsState.size, true);
      loadDropdownData();
    } catch (err) {
      setDeleteSubjectModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete subject.",
      }));
    }
  };

  // ==========================================
  // SCHEDULE HANDLERS
  // ==========================================
  const handleOpenAddSchedule = () => {
    setScheduleFormData({
      ...initialScheduleForm,
      subjectId: dropdownSubjects[0]?.id || "",
      facultyUserId: dropdownFaculty[0]?.id || "",
    });
    setScheduleFormErrors({});
    setScheduleModalError(null);
    setScheduleModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditSchedule = (sch) => {
    setScheduleFormData({
      subjectId: sch.subjectId || "",
      facultyUserId: sch.facultyUserId || "",
      dayOfWeek: sch.dayOfWeek || "MONDAY",
      startTime: sch.startTime ? String(sch.startTime).substring(0, 5) : "09:00",
      endTime: sch.endTime ? String(sch.endTime).substring(0, 5) : "10:30",
      roomNumber: sch.roomNumber || "",
      academicYear: sch.academicYear || "2026-2027",
      semester: sch.semester || 1,
      section: sch.section || "A",
    });
    setScheduleFormErrors({});
    setScheduleModalError(null);
    setScheduleModal({ open: true, mode: "edit", item: sch });
  };

  const handleOpenViewSchedule = (sch) => {
    setScheduleModal({ open: true, mode: "view", item: sch });
  };

  const validateScheduleForm = () => {
    const errs = {};
    if (!scheduleFormData.subjectId) errs.subjectId = "Please select a Subject";
    if (!scheduleFormData.facultyUserId) errs.facultyUserId = "Please select a Faculty member";
    if (!scheduleFormData.dayOfWeek) errs.dayOfWeek = "Day of week is required";
    if (!scheduleFormData.startTime) errs.startTime = "Start time is required";
    if (!scheduleFormData.endTime) errs.endTime = "End time is required";
    if (!scheduleFormData.academicYear.trim()) errs.academicYear = "Academic year is required";
    if (!scheduleFormData.semester || Number(scheduleFormData.semester) < 1) errs.semester = "Semester must be at least 1";
    setScheduleFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitSchedule = async (e) => {
    e.preventDefault();
    if (scheduleModal.mode === "view") return;
    if (!validateScheduleForm()) return;

    setScheduleSubmitting(true);
    setScheduleModalError(null);

    const formatTimeToApi = (t) => (t && t.length === 5 ? `${t}:00` : t);

    try {
      if (scheduleModal.mode === "create") {
        await academicService.createSchedule({
          subjectId: Number(scheduleFormData.subjectId),
          facultyUserId: Number(scheduleFormData.facultyUserId),
          dayOfWeek: scheduleFormData.dayOfWeek,
          startTime: formatTimeToApi(scheduleFormData.startTime),
          endTime: formatTimeToApi(scheduleFormData.endTime),
          roomNumber: scheduleFormData.roomNumber.trim() || undefined,
          academicYear: scheduleFormData.academicYear.trim(),
          semester: Number(scheduleFormData.semester),
          section: scheduleFormData.section.trim() || undefined,
        });
        setToast({ message: "Class schedule created successfully!", type: "success" });
      } else {
        await academicService.updateSchedule(scheduleModal.item.id, {
          facultyUserId: Number(scheduleFormData.facultyUserId),
          dayOfWeek: scheduleFormData.dayOfWeek,
          startTime: formatTimeToApi(scheduleFormData.startTime),
          endTime: formatTimeToApi(scheduleFormData.endTime),
          roomNumber: scheduleFormData.roomNumber.trim() || undefined,
          academicYear: scheduleFormData.academicYear.trim(),
          semester: Number(scheduleFormData.semester),
          section: scheduleFormData.section.trim() || undefined,
        });
        setToast({ message: "Class schedule updated successfully!", type: "success" });
      }

      setScheduleModal({ open: false, mode: "create", item: null });
      fetchSchedules(schedulesState.page, schedulesState.size, true);
    } catch (err) {
      setScheduleModalError(err.response?.data?.message || "Failed to save schedule. Please check inputs.");
    } finally {
      setScheduleSubmitting(false);
    }
  };

  const handleDeleteSchedule = async () => {
    if (!deleteScheduleModal.item) return;
    setDeleteScheduleModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await academicService.deleteSchedule(deleteScheduleModal.item.id);
      setToast({ message: "Class schedule deleted successfully!", type: "success" });
      setDeleteScheduleModal({ open: false, item: null, loading: false, error: null });
      fetchSchedules(schedulesState.page, schedulesState.size, true);
    } catch (err) {
      setDeleteScheduleModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete schedule.",
      }));
    }
  };

  return (
    <div className="academics-page">
      {/* Toast Notification */}
      {toast && (
        <div className={`toast-notification ${toast.type}`}>
          <span>{toast.message}</span>
          <button className="toast-close-btn" onClick={() => setToast(null)}>✕</button>
        </div>
      )}

      {/* Header */}
      <div className="academics-header">
        <div className="academics-header-left">
          <div className="academics-title-row">
            <h1>Academics Management</h1>
          </div>
          <p className="academics-subtitle">
            Explore academic degree programs, curriculum subjects, and published class timetables.
          </p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="academics-tabs" role="tablist">
        <button
          className={`academics-tab-btn ${activeTab === "courses" ? "active" : ""}`}
          onClick={() => setActiveTab("courses")}
          role="tab"
          aria-selected={activeTab === "courses"}
        >
          <span>Courses</span>
          <span className="tab-badge">
            {coursesState.loading ? "…" : coursesState.totalElements}
          </span>
        </button>

        <button
          className={`academics-tab-btn ${activeTab === "subjects" ? "active" : ""}`}
          onClick={() => setActiveTab("subjects")}
          role="tab"
          aria-selected={activeTab === "subjects"}
        >
          <span>Subjects</span>
          <span className="tab-badge">
            {subjectsState.loading ? "…" : subjectsState.totalElements}
          </span>
        </button>

        <button
          className={`academics-tab-btn ${activeTab === "schedules" ? "active" : ""}`}
          onClick={() => setActiveTab("schedules")}
          role="tab"
          aria-selected={activeTab === "schedules"}
        >
          <span>Class Schedules</span>
          <span className="tab-badge">
            {schedulesState.loading ? "…" : schedulesState.totalElements}
          </span>
        </button>
      </div>

      {/* ==============================================================
          TAB 1: COURSES
          ============================================================== */}
      {activeTab === "courses" && (
        <div className="tab-content">
          {coursesState.error && (
            <div className="academics-error-banner" role="alert">
              <div className="error-left">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{coursesState.error}</span>
              </div>
              <button className="retry-btn" onClick={() => fetchCourses(coursesState.page, coursesState.size, true)}>
                Retry
              </button>
            </div>
          )}

          {/* Toolbar */}
          <div className="academics-toolbar">
            <div className="academics-search-wrapper">
              <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                className="academics-search-input"
                placeholder="Search courses by code, name, department..."
                value={coursesState.search}
                onChange={(e) => setCoursesState((prev) => ({ ...prev, search: e.target.value }))}
              />
              {coursesState.search && (
                <button
                  className="search-clear-btn"
                  onClick={() => setCoursesState((prev) => ({ ...prev, search: "" }))}
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="academics-toolbar-actions">
              {isAdmin && (
                <button
                  className="btn-add-entity"
                  onClick={handleOpenAddCourse}
                  id="btn-add-course"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <span>Add Course</span>
                </button>
              )}

              {coursesState.search && (
                <span className="search-feedback">
                  Showing {filteredCourses.length} of {coursesState.items.length}
                </span>
              )}
              <button
                className={`refresh-btn ${isCurrentRefreshing ? "spinning" : ""}`}
                onClick={handleRefresh}
                disabled={isCurrentLoading || isCurrentRefreshing}
                title="Refresh courses"
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
          <div className="academics-table-card">
            <div className="table-responsive">
              <table className="academics-table" id="courses-table">
                <thead>
                  <tr>
                    <th>Course Code</th>
                    <th>Course Name</th>
                    <th>Department</th>
                    <th>Duration</th>
                    <th>Status</th>
                    <th style={{ textAlign: "right" }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {coursesState.loading ? (
                    Array.from({ length: 4 }).map((_, i) => (
                      <tr key={`courses-skel-${i}`} className="skeleton-row">
                        <td><div className="skeleton-pulse" style={{ width: 80 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 220 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 160 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 70 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 65, borderRadius: 12 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80, marginLeft: "auto" }} /></td>
                      </tr>
                    ))
                  ) : filteredCourses.length === 0 ? (
                    <tr>
                      <td colSpan="6">
                        <div className="academics-empty">
                          <div className="empty-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="8" y1="12" x2="16" y2="12" />
                            </svg>
                          </div>
                          <h3>{coursesState.search ? "No matching courses" : "No courses found"}</h3>
                          <p>
                            {coursesState.search
                              ? `No courses matched "${coursesState.search}".`
                              : "No academic degree programs are currently registered."}
                          </p>
                          {isAdmin && !coursesState.search && (
                            <button className="empty-action-btn" onClick={handleOpenAddCourse}>
                              + Add First Course
                            </button>
                          )}
                          {coursesState.search && (
                            <button
                              className="empty-action-btn"
                              onClick={() => setCoursesState((prev) => ({ ...prev, search: "" }))}
                            >
                              Clear Search
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredCourses.map((course) => (
                      <tr key={course.id || course.courseCode}>
                        <td>
                          <span className="code-badge">{course.courseCode}</span>
                        </td>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>{course.courseName}</strong>
                          {course.description && (
                            <div style={{ fontSize: 12, color: "var(--color-text-muted)", marginTop: 2 }}>
                              {course.description}
                            </div>
                          )}
                        </td>
                        <td>{course.department || "—"}</td>
                        <td>
                          <span className="primary-pill">
                            {course.durationYears ? `${course.durationYears} ${course.durationYears === 1 ? "Year" : "Years"}` : "—"}
                          </span>
                        </td>
                        <td>
                          <span className={`status-pill status-${(course.status || "active").toLowerCase()}`}>
                            {course.status || "ACTIVE"}
                          </span>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <div className="action-buttons-cell" style={{ justifyContent: "flex-end" }}>
                            <button
                              className="btn-action-icon view"
                              title="View Details"
                              onClick={() => handleOpenViewCourse(course)}
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                              </svg>
                            </button>
                            {isAdmin && (
                              <>
                                <button
                                  className="btn-action-icon edit"
                                  title="Edit Course"
                                  onClick={() => handleOpenEditCourse(course)}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                                  </svg>
                                </button>
                                <button
                                  className="btn-action-icon delete"
                                  title="Delete Course"
                                  onClick={() => setDeleteCourseModal({ open: true, item: course, loading: false, error: null })}
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
            {!coursesState.loading && coursesState.totalPages > 1 && (
              <div className="academics-pagination">
                <span className="pagination-info">
                  Page {coursesState.page + 1} of {coursesState.totalPages} ({coursesState.totalElements} courses)
                </span>
                <div className="pagination-buttons">
                  <button
                    className="pagination-btn"
                    disabled={coursesState.page === 0}
                    onClick={() => fetchCourses(coursesState.page - 1, coursesState.size)}
                  >
                    Previous
                  </button>
                  <button
                    className="pagination-btn"
                    disabled={coursesState.page >= coursesState.totalPages - 1}
                    onClick={() => fetchCourses(coursesState.page + 1, coursesState.size)}
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
          TAB 2: SUBJECTS
          ============================================================== */}
      {activeTab === "subjects" && (
        <div className="tab-content">
          {subjectsState.error && (
            <div className="academics-error-banner" role="alert">
              <div className="error-left">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{subjectsState.error}</span>
              </div>
              <button className="retry-btn" onClick={() => fetchSubjects(subjectsState.page, subjectsState.size, true)}>
                Retry
              </button>
            </div>
          )}

          {/* Toolbar */}
          <div className="academics-toolbar">
            <div className="academics-search-wrapper">
              <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                className="academics-search-input"
                placeholder="Search subjects by code, name, degree course..."
                value={subjectsState.search}
                onChange={(e) => setSubjectsState((prev) => ({ ...prev, search: e.target.value }))}
              />
              {subjectsState.search && (
                <button
                  className="search-clear-btn"
                  onClick={() => setSubjectsState((prev) => ({ ...prev, search: "" }))}
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="academics-toolbar-actions">
              {isAdminOrFaculty && (
                <button
                  className="btn-add-entity"
                  onClick={handleOpenAddSubject}
                  id="btn-add-subject"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <span>Add Subject</span>
                </button>
              )}

              {subjectsState.search && (
                <span className="search-feedback">
                  Showing {filteredSubjects.length} of {subjectsState.items.length}
                </span>
              )}
              <button
                className={`refresh-btn ${isCurrentRefreshing ? "spinning" : ""}`}
                onClick={handleRefresh}
                disabled={isCurrentLoading || isCurrentRefreshing}
                title="Refresh subjects"
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
          <div className="academics-table-card">
            <div className="table-responsive">
              <table className="academics-table" id="subjects-table">
                <thead>
                  <tr>
                    <th>Subject Code</th>
                    <th>Subject Name</th>
                    <th>Course Program</th>
                    <th>Semester</th>
                    <th>Credits</th>
                    <th style={{ textAlign: "right" }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {subjectsState.loading ? (
                    Array.from({ length: 4 }).map((_, i) => (
                      <tr key={`subjects-skel-${i}`} className="skeleton-row">
                        <td><div className="skeleton-pulse" style={{ width: 80 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 200 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 140 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 60 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 50 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80, marginLeft: "auto" }} /></td>
                      </tr>
                    ))
                  ) : filteredSubjects.length === 0 ? (
                    <tr>
                      <td colSpan="6">
                        <div className="academics-empty">
                          <div className="empty-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="8" y1="12" x2="16" y2="12" />
                            </svg>
                          </div>
                          <h3>{subjectsState.search ? "No matching subjects" : "No subjects found"}</h3>
                          <p>
                            {subjectsState.search
                              ? `No subjects matched "${subjectsState.search}".`
                              : "No academic curriculum subjects have been registered yet."}
                          </p>
                          {isAdminOrFaculty && !subjectsState.search && (
                            <button className="empty-action-btn" onClick={handleOpenAddSubject}>
                              + Add First Subject
                            </button>
                          )}
                          {subjectsState.search && (
                            <button
                              className="empty-action-btn"
                              onClick={() => setSubjectsState((prev) => ({ ...prev, search: "" }))}
                            >
                              Clear Search
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredSubjects.map((subj) => (
                      <tr key={subj.id || subj.subjectCode}>
                        <td>
                          <span className="code-badge">{subj.subjectCode}</span>
                        </td>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>{subj.subjectName}</strong>
                        </td>
                        <td>
                          {subj.courseName ? (
                            <div>
                              <span>{subj.courseName}</span>
                              {subj.courseCode && (
                                <span style={{ fontSize: 12, color: "var(--color-text-muted)", marginLeft: 6 }}>
                                  ({subj.courseCode})
                                </span>
                              )}
                            </div>
                          ) : (
                            <span style={{ color: "var(--color-text-muted)" }}>Course #{subj.courseId}</span>
                          )}
                        </td>
                        <td>
                          <span className="secondary-pill">Sem {subj.semester ?? "—"}</span>
                        </td>
                        <td>
                          <span className="primary-pill">{subj.credits ?? "—"} Cr</span>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <div className="action-buttons-cell" style={{ justifyContent: "flex-end" }}>
                            <button
                              className="btn-action-icon view"
                              title="View Details"
                              onClick={() => handleOpenViewSubject(subj)}
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
                                  title="Edit Subject"
                                  onClick={() => handleOpenEditSubject(subj)}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                                  </svg>
                                </button>
                                <button
                                  className="btn-action-icon delete"
                                  title="Delete Subject"
                                  onClick={() => setDeleteSubjectModal({ open: true, item: subj, loading: false, error: null })}
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
            {!subjectsState.loading && subjectsState.totalPages > 1 && (
              <div className="academics-pagination">
                <span className="pagination-info">
                  Page {subjectsState.page + 1} of {subjectsState.totalPages} ({subjectsState.totalElements} subjects)
                </span>
                <div className="pagination-buttons">
                  <button
                    className="pagination-btn"
                    disabled={subjectsState.page === 0}
                    onClick={() => fetchSubjects(subjectsState.page - 1, subjectsState.size)}
                  >
                    Previous
                  </button>
                  <button
                    className="pagination-btn"
                    disabled={subjectsState.page >= subjectsState.totalPages - 1}
                    onClick={() => fetchSubjects(subjectsState.page + 1, subjectsState.size)}
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
          TAB 3: CLASS SCHEDULES
          ============================================================== */}
      {activeTab === "schedules" && (
        <div className="tab-content">
          {schedulesState.error && (
            <div className="academics-error-banner" role="alert">
              <div className="error-left">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{schedulesState.error}</span>
              </div>
              <button className="retry-btn" onClick={() => fetchSchedules(schedulesState.page, schedulesState.size, true)}>
                Retry
              </button>
            </div>
          )}

          {/* Toolbar */}
          <div className="academics-toolbar">
            <div className="academics-search-wrapper">
              <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                className="academics-search-input"
                placeholder="Search schedules by subject, day, room, section..."
                value={schedulesState.search}
                onChange={(e) => setSchedulesState((prev) => ({ ...prev, search: e.target.value }))}
              />
              {schedulesState.search && (
                <button
                  className="search-clear-btn"
                  onClick={() => setSchedulesState((prev) => ({ ...prev, search: "" }))}
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="academics-toolbar-actions">
              {isAdminOrFaculty && (
                <button
                  className="btn-add-entity"
                  onClick={handleOpenAddSchedule}
                  id="btn-add-schedule"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <span>Add Schedule</span>
                </button>
              )}

              {schedulesState.search && (
                <span className="search-feedback">
                  Showing {filteredSchedules.length} of {schedulesState.items.length}
                </span>
              )}
              <button
                className={`refresh-btn ${isCurrentRefreshing ? "spinning" : ""}`}
                onClick={handleRefresh}
                disabled={isCurrentLoading || isCurrentRefreshing}
                title="Refresh timetables"
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
          <div className="academics-table-card">
            <div className="table-responsive">
              <table className="academics-table" id="schedules-table">
                <thead>
                  <tr>
                    <th>Day</th>
                    <th>Time Slot</th>
                    <th>Subject</th>
                    <th>Room</th>
                    <th>Section</th>
                    <th>Academic Year</th>
                    <th style={{ textAlign: "right" }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {schedulesState.loading ? (
                    Array.from({ length: 4 }).map((_, i) => (
                      <tr key={`schedules-skel-${i}`} className="skeleton-row">
                        <td><div className="skeleton-pulse" style={{ width: 70 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 130 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 180 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 45 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80 }} /></td>
                        <td><div className="skeleton-pulse" style={{ width: 80, marginLeft: "auto" }} /></td>
                      </tr>
                    ))
                  ) : filteredSchedules.length === 0 ? (
                    <tr>
                      <td colSpan="7">
                        <div className="academics-empty">
                          <div className="empty-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="8" y1="12" x2="16" y2="12" />
                            </svg>
                          </div>
                          <h3>{schedulesState.search ? "No matching schedules" : "No timetables found"}</h3>
                          <p>
                            {schedulesState.search
                              ? `No schedules matched "${schedulesState.search}".`
                              : "No class schedules have been published yet."}
                          </p>
                          {isAdminOrFaculty && !schedulesState.search && (
                            <button className="empty-action-btn" onClick={handleOpenAddSchedule}>
                              + Add First Class Schedule
                            </button>
                          )}
                          {schedulesState.search && (
                            <button
                              className="empty-action-btn"
                              onClick={() => setSchedulesState((prev) => ({ ...prev, search: "" }))}
                            >
                              Clear Search
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredSchedules.map((sch) => (
                      <tr key={sch.id}>
                        <td>
                          <span className="day-badge">{formatDay(sch.dayOfWeek)}</span>
                        </td>
                        <td>
                          <span className="time-pill">
                            {formatTime(sch.startTime)} – {formatTime(sch.endTime)}
                          </span>
                        </td>
                        <td>
                          <strong style={{ color: "var(--color-text-heading)" }}>
                            {sch.subjectName || `Subject #${sch.subjectId}`}
                          </strong>
                          {sch.subjectCode && (
                            <span style={{ fontSize: 12, color: "var(--color-text-muted)", marginLeft: 6 }}>
                              ({sch.subjectCode})
                            </span>
                          )}
                        </td>
                        <td>{sch.roomNumber || "—"}</td>
                        <td>
                          <span className="secondary-pill">{sch.section ? `Sec ${sch.section}` : "—"}</span>
                        </td>
                        <td>{sch.academicYear || "—"}</td>
                        <td style={{ textAlign: "right" }}>
                          <div className="action-buttons-cell" style={{ justifyContent: "flex-end" }}>
                            <button
                              className="btn-action-icon view"
                              title="View Details"
                              onClick={() => handleOpenViewSchedule(sch)}
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
                                  title="Edit Schedule"
                                  onClick={() => handleOpenEditSchedule(sch)}
                                >
                                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                                  </svg>
                                </button>
                                <button
                                  className="btn-action-icon delete"
                                  title="Delete Schedule"
                                  onClick={() => setDeleteScheduleModal({ open: true, item: sch, loading: false, error: null })}
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
            {!schedulesState.loading && schedulesState.totalPages > 1 && (
              <div className="academics-pagination">
                <span className="pagination-info">
                  Page {schedulesState.page + 1} of {schedulesState.totalPages} ({schedulesState.totalElements} schedules)
                </span>
                <div className="pagination-buttons">
                  <button
                    className="pagination-btn"
                    disabled={schedulesState.page === 0}
                    onClick={() => fetchSchedules(schedulesState.page - 1, schedulesState.size)}
                  >
                    Previous
                  </button>
                  <button
                    className="pagination-btn"
                    disabled={schedulesState.page >= schedulesState.totalPages - 1}
                    onClick={() => fetchSchedules(schedulesState.page + 1, schedulesState.size)}
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
          COURSE MODALS (ADD / EDIT / VIEW / DELETE)
          ============================================================== */}
      {courseModal.open && (
        <div className="modal-overlay" onClick={() => setCourseModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h2>
                  {courseModal.mode === "create" && "Add New Course"}
                  {courseModal.mode === "edit" && `Edit Course: ${courseModal.item?.courseCode}`}
                  {courseModal.mode === "view" && `Course Details: ${courseModal.item?.courseCode}`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setCourseModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            {courseModal.mode === "view" ? (
              <div className="modal-body">
                <div className="modal-view-row">
                  <span className="modal-view-label">Course Code</span>
                  <span className="modal-view-value"><span className="code-badge">{courseModal.item?.courseCode}</span></span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Course Name</span>
                  <span className="modal-view-value">{courseModal.item?.courseName}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Department</span>
                  <span className="modal-view-value">{courseModal.item?.department || "—"}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Duration</span>
                  <span className="modal-view-value">{courseModal.item?.durationYears} Years</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Status</span>
                  <span className="modal-view-value">
                    <span className={`status-pill status-${(courseModal.item?.status || "active").toLowerCase()}`}>
                      {courseModal.item?.status || "ACTIVE"}
                    </span>
                  </span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Description</span>
                  <span className="modal-view-value">{courseModal.item?.description || "No description provided."}</span>
                </div>
                <div className="modal-footer">
                  <button
                    className="btn-modal-cancel"
                    onClick={() => setCourseModal({ open: false, mode: "create", item: null })}
                  >
                    Close
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmitCourse}>
                <div className="modal-body">
                  {courseModalError && (
                    <div className="modal-alert-error" role="alert">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{courseModalError}</span>
                    </div>
                  )}

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="course-code">
                        Course Code <span className="required-star">*</span>
                      </label>
                      <input
                        id="course-code"
                        type="text"
                        className={`form-input ${courseFormErrors.courseCode ? "input-error" : ""}`}
                        placeholder="e.g. CS-BS"
                        value={courseFormData.courseCode}
                        onChange={(e) => setCourseFormData({ ...courseFormData, courseCode: e.target.value })}
                        disabled={courseModal.mode === "edit" || courseSubmitting}
                      />
                      {courseFormErrors.courseCode && <span className="form-error-msg">{courseFormErrors.courseCode}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="course-duration">
                        Duration (Years) <span className="required-star">*</span>
                      </label>
                      <input
                        id="course-duration"
                        type="number"
                        min="1"
                        max="10"
                        className={`form-input ${courseFormErrors.durationYears ? "input-error" : ""}`}
                        value={courseFormData.durationYears}
                        onChange={(e) => setCourseFormData({ ...courseFormData, durationYears: e.target.value })}
                        disabled={courseSubmitting}
                      />
                      {courseFormErrors.durationYears && <span className="form-error-msg">{courseFormErrors.durationYears}</span>}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="course-name">
                      Course Name <span className="required-star">*</span>
                    </label>
                    <input
                      id="course-name"
                      type="text"
                      className={`form-input ${courseFormErrors.courseName ? "input-error" : ""}`}
                      placeholder="e.g. Bachelor of Science in Computer Science"
                      value={courseFormData.courseName}
                      onChange={(e) => setCourseFormData({ ...courseFormData, courseName: e.target.value })}
                      disabled={courseSubmitting}
                    />
                    {courseFormErrors.courseName && <span className="form-error-msg">{courseFormErrors.courseName}</span>}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="course-dept">
                      Department
                    </label>
                    <input
                      id="course-dept"
                      type="text"
                      className="form-input"
                      placeholder="e.g. Computer Science and Engineering"
                      value={courseFormData.department}
                      onChange={(e) => setCourseFormData({ ...courseFormData, department: e.target.value })}
                      disabled={courseSubmitting}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="course-desc">
                      Description
                    </label>
                    <textarea
                      id="course-desc"
                      className="form-textarea"
                      placeholder="Brief overview of curriculum and learning goals..."
                      value={courseFormData.description}
                      onChange={(e) => setCourseFormData({ ...courseFormData, description: e.target.value })}
                      disabled={courseSubmitting}
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn-modal-cancel"
                    onClick={() => setCourseModal({ open: false, mode: "create", item: null })}
                    disabled={courseSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-modal-submit"
                    disabled={courseSubmitting}
                  >
                    {courseSubmitting ? "Saving..." : courseModal.mode === "create" ? "Create Course" : "Save Changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Delete Course Dialog */}
      {deleteCourseModal.open && (
        <div className="modal-overlay" onClick={() => !deleteCourseModal.loading && setDeleteCourseModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>Confirm Deletion</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteCourseModal.loading}
                onClick={() => setDeleteCourseModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteCourseModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteCourseModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete course <strong>{deleteCourseModal.item?.courseCode} - {deleteCourseModal.item?.courseName}</strong>?
              </p>
              <p style={{ color: "var(--color-text-muted)", fontSize: 13, margin: 0 }}>
                This operation cannot be undone. Courses with associated subjects cannot be deleted until their subjects are removed or reassigned.
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteCourseModal.loading}
                onClick={() => setDeleteCourseModal({ open: false, item: null, loading: false, error: null })}
              >
                Cancel
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteCourseModal.loading}
                onClick={handleDeleteCourse}
              >
                {deleteCourseModal.loading ? "Deleting..." : "Delete Course"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==============================================================
          SUBJECT MODALS (ADD / EDIT / VIEW / DELETE)
          ============================================================== */}
      {subjectModal.open && (
        <div className="modal-overlay" onClick={() => setSubjectModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h2>
                  {subjectModal.mode === "create" && "Add New Subject"}
                  {subjectModal.mode === "edit" && `Edit Subject: ${subjectModal.item?.subjectCode}`}
                  {subjectModal.mode === "view" && `Subject Details: ${subjectModal.item?.subjectCode}`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setSubjectModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            {subjectModal.mode === "view" ? (
              <div className="modal-body">
                <div className="modal-view-row">
                  <span className="modal-view-label">Subject Code</span>
                  <span className="modal-view-value"><span className="code-badge">{subjectModal.item?.subjectCode}</span></span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Subject Name</span>
                  <span className="modal-view-value">{subjectModal.item?.subjectName}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Course Degree</span>
                  <span className="modal-view-value">{subjectModal.item?.courseName || `Course #${subjectModal.item?.courseId}`}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Semester</span>
                  <span className="modal-view-value">Semester {subjectModal.item?.semester}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Credits</span>
                  <span className="modal-view-value">{subjectModal.item?.credits} Credits</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Faculty User</span>
                  <span className="modal-view-value">
                    {subjectModal.item?.facultyUserId ? `Faculty Member #${subjectModal.item.facultyUserId}` : "Unassigned"}
                  </span>
                </div>
                <div className="modal-footer">
                  <button
                    className="btn-modal-cancel"
                    onClick={() => setSubjectModal({ open: false, mode: "create", item: null })}
                  >
                    Close
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmitSubject}>
                <div className="modal-body">
                  {subjectModalError && (
                    <div className="modal-alert-error" role="alert">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{subjectModalError}</span>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label" htmlFor="subj-course">
                      Associated Course <span className="required-star">*</span>
                    </label>
                    <select
                      id="subj-course"
                      className={`form-select ${subjectFormErrors.courseId ? "input-error" : ""}`}
                      value={subjectFormData.courseId}
                      onChange={(e) => setSubjectFormData({ ...subjectFormData, courseId: e.target.value })}
                      disabled={subjectModal.mode === "edit" || subjectSubmitting}
                    >
                      <option value="">Select a Course...</option>
                      {dropdownCourses.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.courseCode} - {c.courseName}
                        </option>
                      ))}
                    </select>
                    {subjectFormErrors.courseId && <span className="form-error-msg">{subjectFormErrors.courseId}</span>}
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="subj-code">
                        Subject Code <span className="required-star">*</span>
                      </label>
                      <input
                        id="subj-code"
                        type="text"
                        className={`form-input ${subjectFormErrors.subjectCode ? "input-error" : ""}`}
                        placeholder="e.g. CS101"
                        value={subjectFormData.subjectCode}
                        onChange={(e) => setSubjectFormData({ ...subjectFormData, subjectCode: e.target.value })}
                        disabled={subjectModal.mode === "edit" || subjectSubmitting}
                      />
                      {subjectFormErrors.subjectCode && <span className="form-error-msg">{subjectFormErrors.subjectCode}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="subj-name">
                        Subject Name <span className="required-star">*</span>
                      </label>
                      <input
                        id="subj-name"
                        type="text"
                        className={`form-input ${subjectFormErrors.subjectName ? "input-error" : ""}`}
                        placeholder="e.g. Data Structures & Algorithms"
                        value={subjectFormData.subjectName}
                        onChange={(e) => setSubjectFormData({ ...subjectFormData, subjectName: e.target.value })}
                        disabled={subjectSubmitting}
                      />
                      {subjectFormErrors.subjectName && <span className="form-error-msg">{subjectFormErrors.subjectName}</span>}
                    </div>
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="subj-credits">
                        Credits <span className="required-star">*</span>
                      </label>
                      <input
                        id="subj-credits"
                        type="number"
                        min="1"
                        max="30"
                        className={`form-input ${subjectFormErrors.credits ? "input-error" : ""}`}
                        value={subjectFormData.credits}
                        onChange={(e) => setSubjectFormData({ ...subjectFormData, credits: e.target.value })}
                        disabled={subjectSubmitting}
                      />
                      {subjectFormErrors.credits && <span className="form-error-msg">{subjectFormErrors.credits}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="subj-sem">
                        Semester <span className="required-star">*</span>
                      </label>
                      <input
                        id="subj-sem"
                        type="number"
                        min="1"
                        max="12"
                        className={`form-input ${subjectFormErrors.semester ? "input-error" : ""}`}
                        value={subjectFormData.semester}
                        onChange={(e) => setSubjectFormData({ ...subjectFormData, semester: e.target.value })}
                        disabled={subjectSubmitting}
                      />
                      {subjectFormErrors.semester && <span className="form-error-msg">{subjectFormErrors.semester}</span>}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="subj-faculty">
                      Assigned Faculty Member
                    </label>
                    <select
                      id="subj-faculty"
                      className="form-select"
                      value={subjectFormData.facultyUserId}
                      onChange={(e) => setSubjectFormData({ ...subjectFormData, facultyUserId: e.target.value })}
                      disabled={subjectSubmitting}
                    >
                      <option value="">Unassigned</option>
                      {dropdownFaculty.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.firstName} {f.lastName} ({f.username})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn-modal-cancel"
                    onClick={() => setSubjectModal({ open: false, mode: "create", item: null })}
                    disabled={subjectSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-modal-submit"
                    disabled={subjectSubmitting}
                  >
                    {subjectSubmitting ? "Saving..." : subjectModal.mode === "create" ? "Create Subject" : "Save Changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Delete Subject Dialog */}
      {deleteSubjectModal.open && (
        <div className="modal-overlay" onClick={() => !deleteSubjectModal.loading && setDeleteSubjectModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>Confirm Deletion</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteSubjectModal.loading}
                onClick={() => setDeleteSubjectModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteSubjectModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteSubjectModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete subject <strong>{deleteSubjectModal.item?.subjectCode} - {deleteSubjectModal.item?.subjectName}</strong>?
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteSubjectModal.loading}
                onClick={() => setDeleteSubjectModal({ open: false, item: null, loading: false, error: null })}
              >
                Cancel
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteSubjectModal.loading}
                onClick={handleDeleteSubject}
              >
                {deleteSubjectModal.loading ? "Deleting..." : "Delete Subject"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==============================================================
          SCHEDULE MODALS (ADD / EDIT / VIEW / DELETE)
          ============================================================== */}
      {scheduleModal.open && (
        <div className="modal-overlay" onClick={() => setScheduleModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h2>
                  {scheduleModal.mode === "create" && "Add Class Schedule"}
                  {scheduleModal.mode === "edit" && `Edit Class Schedule: ${scheduleModal.item?.subjectCode || ""}`}
                  {scheduleModal.mode === "view" && `Class Schedule Details`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setScheduleModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            {scheduleModal.mode === "view" ? (
              <div className="modal-body">
                <div className="modal-view-row">
                  <span className="modal-view-label">Subject</span>
                  <span className="modal-view-value">
                    {scheduleModal.item?.subjectName} ({scheduleModal.item?.subjectCode})
                  </span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Day of Week</span>
                  <span className="modal-view-value"><span className="day-badge">{formatDay(scheduleModal.item?.dayOfWeek)}</span></span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Time Slot</span>
                  <span className="modal-view-value">{formatTime(scheduleModal.item?.startTime)} – {formatTime(scheduleModal.item?.endTime)}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Room & Building</span>
                  <span className="modal-view-value">{scheduleModal.item?.roomNumber || "—"}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Section</span>
                  <span className="modal-view-value">{scheduleModal.item?.section || "—"}</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Academic Year</span>
                  <span className="modal-view-value">{scheduleModal.item?.academicYear} (Semester {scheduleModal.item?.semester})</span>
                </div>
                <div className="modal-view-row">
                  <span className="modal-view-label">Faculty User</span>
                  <span className="modal-view-value">Faculty ID #{scheduleModal.item?.facultyUserId}</span>
                </div>
                <div className="modal-footer">
                  <button
                    className="btn-modal-cancel"
                    onClick={() => setScheduleModal({ open: false, mode: "create", item: null })}
                  >
                    Close
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmitSchedule}>
                <div className="modal-body">
                  {scheduleModalError && (
                    <div className="modal-alert-error" role="alert">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{scheduleModalError}</span>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label" htmlFor="sch-subject">
                      Subject <span className="required-star">*</span>
                    </label>
                    <select
                      id="sch-subject"
                      className={`form-select ${scheduleFormErrors.subjectId ? "input-error" : ""}`}
                      value={scheduleFormData.subjectId}
                      onChange={(e) => setScheduleFormData({ ...scheduleFormData, subjectId: e.target.value })}
                      disabled={scheduleModal.mode === "edit" || scheduleSubmitting}
                    >
                      <option value="">Select a Subject...</option>
                      {dropdownSubjects.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.subjectCode} - {s.subjectName}
                        </option>
                      ))}
                    </select>
                    {scheduleFormErrors.subjectId && <span className="form-error-msg">{scheduleFormErrors.subjectId}</span>}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="sch-faculty">
                      Faculty Member <span className="required-star">*</span>
                    </label>
                    <select
                      id="sch-faculty"
                      className={`form-select ${scheduleFormErrors.facultyUserId ? "input-error" : ""}`}
                      value={scheduleFormData.facultyUserId}
                      onChange={(e) => setScheduleFormData({ ...scheduleFormData, facultyUserId: e.target.value })}
                      disabled={scheduleSubmitting}
                    >
                      <option value="">Select Faculty...</option>
                      {dropdownFaculty.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.firstName} {f.lastName} ({f.username})
                        </option>
                      ))}
                    </select>
                    {scheduleFormErrors.facultyUserId && <span className="form-error-msg">{scheduleFormErrors.facultyUserId}</span>}
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-day">
                        Day of Week <span className="required-star">*</span>
                      </label>
                      <select
                        id="sch-day"
                        className="form-select"
                        value={scheduleFormData.dayOfWeek}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, dayOfWeek: e.target.value })}
                        disabled={scheduleSubmitting}
                      >
                        {DAYS_OF_WEEK.map((d) => (
                          <option key={d} value={d}>
                            {formatDay(d)}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-room">
                        Room Number
                      </label>
                      <input
                        id="sch-room"
                        type="text"
                        className="form-input"
                        placeholder="e.g. Hall-101"
                        value={scheduleFormData.roomNumber}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, roomNumber: e.target.value })}
                        disabled={scheduleSubmitting}
                      />
                    </div>
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-start">
                        Start Time <span className="required-star">*</span>
                      </label>
                      <input
                        id="sch-start"
                        type="time"
                        className={`form-input ${scheduleFormErrors.startTime ? "input-error" : ""}`}
                        value={scheduleFormData.startTime}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, startTime: e.target.value })}
                        disabled={scheduleSubmitting}
                      />
                      {scheduleFormErrors.startTime && <span className="form-error-msg">{scheduleFormErrors.startTime}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-end">
                        End Time <span className="required-star">*</span>
                      </label>
                      <input
                        id="sch-end"
                        type="time"
                        className={`form-input ${scheduleFormErrors.endTime ? "input-error" : ""}`}
                        value={scheduleFormData.endTime}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, endTime: e.target.value })}
                        disabled={scheduleSubmitting}
                      />
                      {scheduleFormErrors.endTime && <span className="form-error-msg">{scheduleFormErrors.endTime}</span>}
                    </div>
                  </div>

                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-ay">
                        Academic Year <span className="required-star">*</span>
                      </label>
                      <input
                        id="sch-ay"
                        type="text"
                        className={`form-input ${scheduleFormErrors.academicYear ? "input-error" : ""}`}
                        placeholder="e.g. 2026-2027"
                        value={scheduleFormData.academicYear}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, academicYear: e.target.value })}
                        disabled={scheduleSubmitting}
                      />
                      {scheduleFormErrors.academicYear && <span className="form-error-msg">{scheduleFormErrors.academicYear}</span>}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="sch-sem">
                        Semester <span className="required-star">*</span>
                      </label>
                      <input
                        id="sch-sem"
                        type="number"
                        min="1"
                        max="12"
                        className={`form-input ${scheduleFormErrors.semester ? "input-error" : ""}`}
                        value={scheduleFormData.semester}
                        onChange={(e) => setScheduleFormData({ ...scheduleFormData, semester: e.target.value })}
                        disabled={scheduleSubmitting}
                      />
                      {scheduleFormErrors.semester && <span className="form-error-msg">{scheduleFormErrors.semester}</span>}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="sch-section">
                      Section
                    </label>
                    <input
                      id="sch-section"
                      type="text"
                      className="form-input"
                      placeholder="e.g. A"
                      value={scheduleFormData.section}
                      onChange={(e) => setScheduleFormData({ ...scheduleFormData, section: e.target.value })}
                      disabled={scheduleSubmitting}
                    />
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn-modal-cancel"
                    onClick={() => setScheduleModal({ open: false, mode: "create", item: null })}
                    disabled={scheduleSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-modal-submit"
                    disabled={scheduleSubmitting}
                  >
                    {scheduleSubmitting ? "Saving..." : scheduleModal.mode === "create" ? "Create Schedule" : "Save Changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Delete Schedule Dialog */}
      {deleteScheduleModal.open && (
        <div className="modal-overlay" onClick={() => !deleteScheduleModal.loading && setDeleteScheduleModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>Confirm Deletion</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteScheduleModal.loading}
                onClick={() => setDeleteScheduleModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteScheduleModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteScheduleModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete the schedule for <strong>{deleteScheduleModal.item?.subjectName}</strong> on <strong>{formatDay(deleteScheduleModal.item?.dayOfWeek)}</strong>?
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteScheduleModal.loading}
                onClick={() => setDeleteScheduleModal({ open: false, item: null, loading: false, error: null })}
              >
                Cancel
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteScheduleModal.loading}
                onClick={handleDeleteSchedule}
              >
                {deleteScheduleModal.loading ? "Deleting..." : "Delete Schedule"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AcademicsPage;
