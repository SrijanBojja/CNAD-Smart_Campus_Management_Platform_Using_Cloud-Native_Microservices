import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import authService from "../services/authService";
import studentService from "../services/studentService";
import "./Students.css";

/**
 * Status badge styling helper
 */
const getStatusClass = (status) => {
  const norm = (status || "").toUpperCase();
  switch (norm) {
    case "ACTIVE":
      return "status-active";
    case "INACTIVE":
      return "status-inactive";
    case "SUSPENDED":
      return "status-suspended";
    case "GRADUATED":
      return "status-graduated";
    default:
      return "status-active";
  }
};

/**
 * StudentsPage — Student management directory for ADMIN and FACULTY.
 * Displays paginated student list, client-side search, real backend data,
 * and comprehensive loading, empty, error, and unauthorized states.
 */
const initialFormData = {
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  phone: "",
  studentNumber: "",
  department: "",
  program: "",
  yearOfStudy: "",
  section: "",
  status: "ACTIVE",
};

/**
 * StudentsPage — Student management directory for ADMIN and FACULTY.
 * Displays paginated student list, client-side search, real backend data,
 * Add Student modal for ADMINs, and comprehensive loading, empty, error, and unauthorized states.
 */
const StudentsPage = ({ onNavigate }) => {
  const { user } = useAuth();
  const role = user?.role;
  const isAuthorized = role === "ADMIN" || role === "FACULTY";
  const isAdmin = role === "ADMIN";

  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Client-side search query
  const [searchTerm, setSearchTerm] = useState("");

  // Add Student modal & form states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [createdAuthUserId, setCreatedAuthUserId] = useState(null);
  const [toast, setToast] = useState(null);

  const fetchStudents = async (targetPage = page, targetSize = size, isManualRefresh = false) => {
    if (!isAuthorized) {
      setLoading(false);
      return;
    }

    if (isManualRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      const data = await studentService.getStudents({
        page: targetPage,
        size: targetSize,
        sort: "createdAt,desc",
      });

      setStudents(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
      setPage(data.number ?? targetPage);
      setSize(data.size ?? targetSize);
    } catch (err) {
      const status = err.response?.status;
      if (status === 403) {
        setError("Access restricted. You lack permission to view the student registry.");
      } else if (status === 401) {
        setError("Session expired. Please sign in again.");
      } else if (err.code === "ERR_NETWORK") {
        setError("Unable to connect to Student Service. Please ensure the microservice is running.");
      } else {
        setError(
          err.response?.data?.message ||
          err.response?.data?.error ||
          "Failed to load students. Please try again later."
        );
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchStudents(page, size);
  }, [page, size, isAuthorized]);

  // Auto-dismiss toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => {
        setToast(null);
      }, 6000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Modal open/close handlers
  const handleOpenModal = () => {
    setFormData(initialFormData);
    setFormErrors({});
    setSubmitError(null);
    setCreatedAuthUserId(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (submitting) return;
    setIsModalOpen(false);
    setFormData(initialFormData);
    setFormErrors({});
    setSubmitError(null);
    setCreatedAuthUserId(null);
  };

  // Close modal on Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape" && isModalOpen && !submitting) {
        handleCloseModal();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isModalOpen, submitting]);

  const handleRefresh = () => {
    fetchStudents(page, size, true);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
    }
  };

  const handleSizeChange = (e) => {
    const newSize = Number(e.target.value);
    setSize(newSize);
    setPage(0);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear field-specific validation error on user input
    if (formErrors[name]) {
      setFormErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  // Client-side validation logic
  const validateForm = () => {
    const errors = {};

    // Account validations (only required if auth account has not yet been registered)
    if (!createdAuthUserId) {
      if (!formData.username || !formData.username.trim()) {
        errors.username = "Username is required.";
      } else if (formData.username.trim().length < 3) {
        errors.username = "Username must be at least 3 characters.";
      } else if (formData.username.trim().length > 50) {
        errors.username = "Username cannot exceed 50 characters.";
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!formData.email || !formData.email.trim()) {
        errors.email = "Email address is required.";
      } else if (!emailRegex.test(formData.email.trim())) {
        errors.email = "Please enter a valid email address.";
      } else if (formData.email.trim().length > 150) {
        errors.email = "Email cannot exceed 150 characters.";
      }

      if (!formData.password) {
        errors.password = "Password is required.";
      } else if (formData.password.length < 6) {
        errors.password = "Password must be at least 6 characters.";
      } else if (formData.password.length > 100) {
        errors.password = "Password cannot exceed 100 characters.";
      }

      if (!formData.confirmPassword) {
        errors.confirmPassword = "Confirm password is required.";
      } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = "Passwords do not match.";
      }
    }

    // Personal validations
    if (!formData.firstName || !formData.firstName.trim()) {
      errors.firstName = "First name is required.";
    } else if (formData.firstName.trim().length > 100) {
      errors.firstName = "First name cannot exceed 100 characters.";
    }

    if (!formData.lastName || !formData.lastName.trim()) {
      errors.lastName = "Last name is required.";
    } else if (formData.lastName.trim().length > 100) {
      errors.lastName = "Last name cannot exceed 100 characters.";
    }

    if (formData.dateOfBirth) {
      const dob = new Date(formData.dateOfBirth);
      const today = new Date();
      if (dob >= today) {
        errors.dateOfBirth = "Date of birth must be in the past.";
      }
    }

    if (formData.phone && formData.phone.trim().length > 30) {
      errors.phone = "Phone number cannot exceed 30 characters.";
    }

    // Academic validations
    if (!formData.studentNumber || !formData.studentNumber.trim()) {
      errors.studentNumber = "Student number is required.";
    } else if (formData.studentNumber.trim().length > 30) {
      errors.studentNumber = "Student number cannot exceed 30 characters.";
    }

    if (formData.department && formData.department.trim().length > 100) {
      errors.department = "Department cannot exceed 100 characters.";
    }

    if (formData.program && formData.program.trim().length > 100) {
      errors.program = "Program cannot exceed 100 characters.";
    }

    if (formData.yearOfStudy) {
      const year = Number(formData.yearOfStudy);
      if (isNaN(year) || year < 1 || year > 10) {
        errors.yearOfStudy = "Year of study must be a number between 1 and 10.";
      }
    }

    if (formData.section && formData.section.trim().length > 50) {
      errors.section = "Section cannot exceed 50 characters.";
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      setSubmitError("Please resolve the highlighted validation errors before submitting.");
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    let authUserId = createdAuthUserId;

    // Step 1: Create the authentication account via Auth Service if not already created
    if (!authUserId) {
      const authPayload = {
        username: formData.username.trim(),
        email: formData.email.trim(),
        password: formData.password,
        firstName: formData.firstName.trim() || undefined,
        lastName: formData.lastName.trim() || undefined,
      };

      try {
        const authUser = await authService.registerStudentAccount(authPayload);
        authUserId = authUser?.id ?? authUser?.data?.id ?? authUser?.userId;
        if (!authUserId) {
          setSubmitError(
            "The authentication account was created, but no valid User ID was returned by the Auth Service. Please contact an administrator."
          );
          setSubmitting(false);
          return;
        }
        setCreatedAuthUserId(authUserId);
      } catch (authErr) {
        let authErrorMessage = "Failed to create authentication account. Please verify credentials and try again.";
        if (authErr.response?.status === 409) {
          authErrorMessage = authErr.response?.data?.message || "Username or email is already registered in the Authentication Service.";
        } else if (authErr.response?.status === 400) {
          authErrorMessage = authErr.response?.data?.message || authErr.response?.data?.error || "Invalid registration details. Please verify your input.";
        } else if (authErr.response?.status === 403) {
          authErrorMessage = "Access denied. Only administrators can register new student accounts.";
        } else if (authErr.response?.data?.message) {
          authErrorMessage = authErr.response.data.message;
        } else if (authErr.message) {
          authErrorMessage = authErr.message;
        }

        setSubmitError(authErrorMessage);
        setSubmitting(false);
        return;
      }
    }

    // Step 2: Create the student profile in Student Service using that user ID
    // Exclude password and confirm-password fields from the student profile request
    const studentPayload = {
      userId: Number(authUserId),
      studentNumber: formData.studentNumber.trim(),
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      dateOfBirth: formData.dateOfBirth || null,
      phone: formData.phone.trim() || null,
      department: formData.department.trim() || null,
      program: formData.program.trim() || null,
      yearOfStudy: formData.yearOfStudy ? Number(formData.yearOfStudy) : null,
      section: formData.section.trim() || null,
    };

    try {
      const createdStudent = await studentService.createStudent(studentPayload);

      setToast({
        type: "success",
        message: `Student ${createdStudent.studentNumber || studentPayload.studentNumber} (${createdStudent.firstName} ${createdStudent.lastName}) registered successfully!`,
      });

      // Close modal and reset form
      setIsModalOpen(false);
      setFormData(initialFormData);
      setFormErrors({});
      setCreatedAuthUserId(null);

      // Refresh student directory to page 0 to show the newly added student at top
      setPage(0);
      fetchStudents(0, size, true);
    } catch (profileErr) {
      // Step 3: Handle partial failure clearly
      let profileErrMsg = "Failed to create student profile.";
      if (profileErr.response?.status === 409) {
        profileErrMsg = profileErr.response?.data?.message || "A student with this Student Number or User ID already exists in Student Service.";
      } else if (profileErr.response?.status === 400) {
        profileErrMsg = profileErr.response?.data?.message || profileErr.response?.data?.error || "Validation failed in Student Service.";
      } else if (profileErr.response?.status === 403) {
        profileErrMsg = "Access denied by Student Service.";
      } else if (profileErr.response?.data?.message) {
        profileErrMsg = profileErr.response.data.message;
      } else if (profileErr.message) {
        profileErrMsg = profileErr.message;
      }

      setSubmitError(
        `Partial Failure: The authentication account for "${formData.username.trim()}" was created successfully (User ID: ${authUserId}), but creating the student profile failed: ${profileErrMsg}. The authentication account is already registered; do not re-register the account. You may correct the student details below and click Create Student to retry profile creation.`
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Client-side search across student number, name, department, program
  const filteredStudents = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return students;

    return students.filter((s) => {
      const sNumber = (s.studentNumber || "").toLowerCase();
      const fullName = `${s.firstName || ""} ${s.lastName || ""}`.toLowerCase();
      const dept = (s.department || "").toLowerCase();
      const prog = (s.program || "").toLowerCase();

      return (
        sNumber.includes(term) ||
        fullName.includes(term) ||
        dept.includes(term) ||
        prog.includes(term)
      );
    });
  }, [students, searchTerm]);

  // If unauthorized role (e.g. STUDENT)
  if (!isAuthorized) {
    return (
      <div className="students-unauthorized-card" role="alert">
        <div className="unauthorized-icon" aria-hidden="true">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h2>Access Restricted</h2>
        <p>
          Student management and profile access is restricted to <strong>ADMIN</strong> and <strong>FACULTY</strong> roles. Your account ({role || "Student"}) does not have permission to view this directory.
        </p>
        <button
          className="unauthorized-back-btn"
          onClick={() => onNavigate && onNavigate("dashboard")}
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const startRecord = totalElements === 0 ? 0 : page * size + 1;
  const endRecord = Math.min((page + 1) * size, totalElements);

  return (
    <div className="students-page">
      {/* Page Header */}
      <div className="students-header">
        <div className="students-header-left">
          <div className="students-title-row">
            <h1>Students Management</h1>
            <span className="students-count-badge" title="Total registered students">
              {loading ? "Loading..." : `${totalElements} ${totalElements === 1 ? "Student" : "Students"}`}
            </span>
          </div>
          <p className="students-subtitle">
            View and manage registered campus students, academic programs, and enrollment status.
          </p>
        </div>

        {/* Add Student Button (ADMIN only) */}
        {isAdmin && (
          <div className="students-header-actions">
            <button
              id="btn-add-student"
              className="btn-add-student"
              onClick={handleOpenModal}
              title="Add a new student"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>+ Add Student</span>
            </button>
          </div>
        )}
      </div>

      {/* Success Toast */}
      {toast && (
        <div className={`student-toast ${toast.type}`} role="status">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{toast.message}</span>
          </div>
          <button
            className="student-toast-close"
            onClick={() => setToast(null)}
            aria-label="Dismiss message"
          >
            ✕
          </button>
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div className="students-error-banner" role="alert">
          <div className="error-left">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{error}</span>
          </div>
          <button className="retry-btn" onClick={() => fetchStudents(page, size, true)}>
            Retry
          </button>
        </div>
      )}

      {/* Toolbar: Search & Refresh */}
      <div className="students-toolbar">
        <div className="students-search-wrapper">
          <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            id="student-search-input"
            type="text"
            className="students-search-input"
            placeholder="Search by student number, name, department, program..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            disabled={loading}
          />
          {searchTerm && (
            <button
              className="clear-search-btn"
              onClick={() => setSearchTerm("")}
              aria-label="Clear search query"
              title="Clear search"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>

        <div className="students-toolbar-actions">
          {searchTerm && (
            <span className="search-feedback">
              Showing {filteredStudents.length} of {students.length} on this page
            </span>
          )}
          <button
            className={`refresh-btn ${refreshing ? "spinning" : ""}`}
            onClick={handleRefresh}
            disabled={loading || refreshing}
            title="Refresh student list"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>{refreshing ? "Refreshing..." : "Refresh"}</span>
          </button>
        </div>
      </div>

      {/* Students Table Card */}
      <div className="students-table-card">
        <div className="table-responsive">
          <table className="students-table" id="students-table">
            <thead>
              <tr>
                <th>Student Number</th>
                <th>Name</th>
                <th>Department</th>
                <th>Program</th>
                <th>Year</th>
                <th>Section</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                // Skeleton loading state
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={`skeleton-${i}`} className="skeleton-row">
                    <td><div className="skeleton-pulse" style={{ width: 100 }} /></td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div className="skeleton-pulse" style={{ width: 30, height: 30, borderRadius: "50%" }} />
                        <div className="skeleton-pulse" style={{ width: 140 }} />
                      </div>
                    </td>
                    <td><div className="skeleton-pulse" style={{ width: 180 }} /></td>
                    <td><div className="skeleton-pulse" style={{ width: 90 }} /></td>
                    <td><div className="skeleton-pulse" style={{ width: 50 }} /></td>
                    <td><div className="skeleton-pulse" style={{ width: 45 }} /></td>
                    <td><div className="skeleton-pulse" style={{ width: 70, borderRadius: 12 }} /></td>
                  </tr>
                ))
              ) : filteredStudents.length === 0 ? (
                // Empty state
                <tr>
                  <td colSpan="7">
                    <div className="students-empty">
                      <div className="empty-icon" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <circle cx="12" cy="12" r="10" />
                          <line x1="8" y1="12" x2="16" y2="12" />
                        </svg>
                      </div>
                      <h3>{searchTerm ? "No matching students found" : "No student records found"}</h3>
                      <p>
                        {searchTerm
                          ? `No student records matched "${searchTerm}". Try a different keyword or clear the search.`
                          : "There are currently no students registered in the system."}
                      </p>
                      {searchTerm && (
                        <button className="empty-action-btn" onClick={() => setSearchTerm("")}>
                          Clear Search
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                // Render students
                filteredStudents.map((s) => {
                  const fullName = `${s.firstName || ""} ${s.lastName || ""}`.trim() || "Unnamed Student";
                  const initial = (s.firstName ? s.firstName[0] : s.lastName ? s.lastName[0] : "S").toUpperCase();

                  return (
                    <tr key={s.id || s.studentNumber}>
                      <td>
                        <span className="student-num-badge">{s.studentNumber}</span>
                      </td>
                      <td>
                        <div className="student-name-cell">
                          <div className="student-avatar-mini" aria-hidden="true">
                            {initial}
                          </div>
                          <span className="student-fullname">{fullName}</span>
                        </div>
                      </td>
                      <td>
                        <span className="student-dept">{s.department || "—"}</span>
                      </td>
                      <td>
                        <span className="student-program-badge">{s.program || "—"}</span>
                      </td>
                      <td>
                        <span className="student-year-pill">
                          {s.yearOfStudy ? `Year ${s.yearOfStudy}` : "—"}
                        </span>
                      </td>
                      <td>
                        <span className="student-section-pill">
                          {s.section ? `Sec ${s.section}` : "—"}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge ${getStatusClass(s.status)}`}>
                          {s.status || "ACTIVE"}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {!loading && totalElements > 0 && (
          <div className="students-pagination">
            <span className="pagination-info">
              Showing {startRecord}–{endRecord} of {totalElements} students
            </span>

            <div className="pagination-controls">
              <label htmlFor="student-page-size" style={{ fontSize: 12.5, color: "var(--color-text-muted)" }}>
                Per page:
              </label>
              <select
                id="student-page-size"
                className="pagination-size-select"
                value={size}
                onChange={handleSizeChange}
                aria-label="Items per page"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>

              <div className="pagination-buttons">
                <button
                  className="pagination-btn"
                  onClick={() => handlePageChange(page - 1)}
                  disabled={page === 0}
                  aria-label="Previous page"
                  title="Previous page"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="15 18 9 12 15 6" />
                  </svg>
                </button>

                <span className="pagination-page-indicator">
                  Page {page + 1} of {Math.max(1, totalPages)}
                </span>

                <button
                  className="pagination-btn"
                  onClick={() => handlePageChange(page + 1)}
                  disabled={page >= totalPages - 1}
                  aria-label="Next page"
                  title="Next page"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Add Student Modal */}
      {isModalOpen && (
        <div className="student-modal-overlay" onClick={handleCloseModal}>
          <div
            className="student-modal-content"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-student-modal-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="student-modal-header">
              <div className="student-modal-title">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8.5" cy="7" r="4" />
                  <line x1="20" y1="8" x2="20" y2="14" />
                  <line x1="23" y1="11" x2="17" y2="11" />
                </svg>
                <h2 id="add-student-modal-title">Add New Student</h2>
              </div>
              <button
                type="button"
                className="student-modal-close-btn"
                onClick={handleCloseModal}
                disabled={submitting}
                aria-label="Close dialog"
                title="Close"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} noValidate>
              <div className="student-modal-body">
                {submitError && (
                  <div className="modal-error-alert" role="alert">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span>{submitError}</span>
                  </div>
                )}

                {createdAuthUserId && (
                  <div
                    className="modal-info-alert"
                    role="status"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.625rem",
                      padding: "0.75rem 1rem",
                      marginBottom: "1rem",
                      backgroundColor: "#eff6ff",
                      border: "1px solid #bfdbfe",
                      borderRadius: "8px",
                      color: "#1e40af",
                      fontSize: "0.875rem",
                      lineHeight: "1.4",
                    }}
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="16" x2="12" y2="12" />
                      <line x1="12" y1="8" x2="12.01" y2="8" />
                    </svg>
                    <span>
                      Authentication account created with User ID: <strong>{createdAuthUserId}</strong>. Complete student profile details below and submit to retry profile creation.
                    </span>
                  </div>
                )}

                {/* Section 1: Account Information */}
                <div className="form-section">
                  <h3 className="form-section-title">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    Account Information
                  </h3>
                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="student-username">
                        Username <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-username"
                        name="username"
                        type="text"
                        className={`form-input ${formErrors.username ? "input-error" : ""}`}
                        placeholder="e.g. jdoe_student"
                        value={formData.username}
                        onChange={handleInputChange}
                        disabled={submitting || !!createdAuthUserId}
                      />
                      {formErrors.username && (
                        <span className="form-error-msg">{formErrors.username}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-email">
                        Email Address <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-email"
                        name="email"
                        type="email"
                        className={`form-input ${formErrors.email ? "input-error" : ""}`}
                        placeholder="e.g. student@campus.edu"
                        value={formData.email}
                        onChange={handleInputChange}
                        disabled={submitting || !!createdAuthUserId}
                      />
                      {formErrors.email && (
                        <span className="form-error-msg">{formErrors.email}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-password">
                        Password <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-password"
                        name="password"
                        type="password"
                        className={`form-input ${formErrors.password ? "input-error" : ""}`}
                        placeholder="••••••••"
                        value={formData.password}
                        onChange={handleInputChange}
                        disabled={submitting || !!createdAuthUserId}
                      />
                      {formErrors.password && (
                        <span className="form-error-msg">{formErrors.password}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-confirmPassword">
                        Confirm Password <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-confirmPassword"
                        name="confirmPassword"
                        type="password"
                        className={`form-input ${formErrors.confirmPassword ? "input-error" : ""}`}
                        placeholder="••••••••"
                        value={formData.confirmPassword}
                        onChange={handleInputChange}
                        disabled={submitting || !!createdAuthUserId}
                      />
                      {formErrors.confirmPassword && (
                        <span className="form-error-msg">{formErrors.confirmPassword}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Section 2: Personal Information */}
                <div className="form-section">
                  <h3 className="form-section-title">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                    Personal Information
                  </h3>
                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="student-firstName">
                        First Name <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-firstName"
                        name="firstName"
                        type="text"
                        className={`form-input ${formErrors.firstName ? "input-error" : ""}`}
                        placeholder="e.g. John"
                        value={formData.firstName}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.firstName && (
                        <span className="form-error-msg">{formErrors.firstName}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-lastName">
                        Last Name <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-lastName"
                        name="lastName"
                        type="text"
                        className={`form-input ${formErrors.lastName ? "input-error" : ""}`}
                        placeholder="e.g. Doe"
                        value={formData.lastName}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.lastName && (
                        <span className="form-error-msg">{formErrors.lastName}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-dateOfBirth">
                        Date of Birth
                      </label>
                      <input
                        id="student-dateOfBirth"
                        name="dateOfBirth"
                        type="date"
                        className={`form-input ${formErrors.dateOfBirth ? "input-error" : ""}`}
                        value={formData.dateOfBirth}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.dateOfBirth && (
                        <span className="form-error-msg">{formErrors.dateOfBirth}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-phone">
                        Phone Number
                      </label>
                      <input
                        id="student-phone"
                        name="phone"
                        type="tel"
                        className={`form-input ${formErrors.phone ? "input-error" : ""}`}
                        placeholder="e.g. +1 555 123 4567"
                        value={formData.phone}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.phone && (
                        <span className="form-error-msg">{formErrors.phone}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Section 3: Academic Information */}
                <div className="form-section">
                  <h3 className="form-section-title">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                      <path d="M6 12v5c3 3 9 3 12 0v-5" />
                    </svg>
                    Academic Information
                  </h3>
                  <div className="form-grid-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="student-studentNumber">
                        Student Number <span className="required-star">*</span>
                      </label>
                      <input
                        id="student-studentNumber"
                        name="studentNumber"
                        type="text"
                        className={`form-input ${formErrors.studentNumber ? "input-error" : ""}`}
                        placeholder="e.g. STU-2024-001"
                        value={formData.studentNumber}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.studentNumber && (
                        <span className="form-error-msg">{formErrors.studentNumber}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-department">
                        Department
                      </label>
                      <input
                        id="student-department"
                        name="department"
                        type="text"
                        className="form-input"
                        placeholder="e.g. Computer Science"
                        value={formData.department}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-program">
                        Program
                      </label>
                      <input
                        id="student-program"
                        name="program"
                        type="text"
                        className="form-input"
                        placeholder="e.g. B.Tech Computer Engineering"
                        value={formData.program}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-yearOfStudy">
                        Year of Study
                      </label>
                      <input
                        id="student-yearOfStudy"
                        name="yearOfStudy"
                        type="number"
                        min="1"
                        max="10"
                        className={`form-input ${formErrors.yearOfStudy ? "input-error" : ""}`}
                        placeholder="e.g. 1"
                        value={formData.yearOfStudy}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.yearOfStudy && (
                        <span className="form-error-msg">{formErrors.yearOfStudy}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-section">
                        Section
                      </label>
                      <input
                        id="student-section"
                        name="section"
                        type="text"
                        className="form-input"
                        placeholder="e.g. A"
                        value={formData.section}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="student-status">
                        Student Status
                      </label>
                      <input
                        id="student-status"
                        name="status"
                        type="text"
                        className="form-input"
                        value={formData.status}
                        disabled
                        readOnly
                      />
                      <span className="form-helper-text">
                        New student profiles are activated with ACTIVE status by default
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="student-modal-footer">
                <button
                  type="button"
                  className="btn-modal-cancel"
                  onClick={handleCloseModal}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="btn-submit-student"
                  className="btn-modal-submit"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <svg className="spinning" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" strokeDasharray="32" strokeDashoffset="12" />
                      </svg>
                      <span>Creating Student...</span>
                    </>
                  ) : (
                    <>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                      <span>Create Student</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudentsPage;

