import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import authService from "../services/authService";
import "./Faculty.css";

const initialFormData = {
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  firstName: "",
  lastName: "",
  employeeId: "",
  department: "",
  phoneNumber: "",
};

const FacultyPage = ({ onNavigate }) => {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [facultyList, setFacultyList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Search filter
  const [searchTerm, setSearchTerm] = useState("");

  // Modal & Form state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [toast, setToast] = useState(null);

  // Auto-dismiss toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  const fetchFaculty = async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      const data = await authService.getFacultyUsers();
      setFacultyList(Array.isArray(data) ? data : []);
    } catch (err) {
      const status = err.response?.status;
      if (status === 403) {
        setError("Access restricted: Administrator role required to view faculty.");
      } else if (status === 401) {
        setError("Your session has expired. Please log in again.");
      } else {
        setError("Unable to connect to the authentication service. Please check if services are running.");
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    if (isAdmin) {
      fetchFaculty();
    } else {
      setLoading(false);
    }
  }, [isAdmin]);

  const handleOpenModal = () => {
    setFormData(initialFormData);
    setFormErrors({});
    setSubmitError(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (submitting) return;
    setIsModalOpen(false);
    setFormData(initialFormData);
    setFormErrors({});
    setSubmitError(null);
  };

  // Close modal on Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape" && isModalOpen && !submitting) {
        handleCloseModal();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isModalOpen, submitting]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  const validateForm = () => {
    const errors = {};

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

    if (formData.employeeId && formData.employeeId.trim().length > 50) {
      errors.employeeId = "Employee ID cannot exceed 50 characters.";
    }

    if (formData.department && formData.department.trim().length > 100) {
      errors.department = "Department cannot exceed 100 characters.";
    }

    if (formData.phoneNumber && formData.phoneNumber.trim().length > 50) {
      errors.phoneNumber = "Phone number cannot exceed 50 characters.";
    }

    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    const payload = {
      username: formData.username.trim(),
      email: formData.email.trim().toLowerCase(),
      password: formData.password,
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      employeeId: formData.employeeId?.trim() || null,
      department: formData.department?.trim() || null,
      phoneNumber: formData.phoneNumber?.trim() || null,
    };

    try {
      const created = await authService.registerFacultyAccount(payload);
      const name = `${created.firstName || payload.firstName} ${created.lastName || payload.lastName}`;
      setToast({
        type: "success",
        message: `Faculty member "${name}" registered successfully.`,
      });
      handleCloseModal();
      fetchFaculty(true);
    } catch (err) {
      const status = err.response?.status;
      const respData = err.response?.data;
      if (status === 409) {
        setSubmitError(respData?.message || "Username or email is already registered.");
      } else if (status === 400) {
        setSubmitError(respData?.message || "Invalid input data. Please check all fields.");
      } else if (status === 403) {
        setSubmitError("Access forbidden: Only administrators can register faculty accounts.");
      } else {
        setSubmitError(respData?.message || "Failed to create faculty account. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Filtered faculty list
  const filteredFaculty = useMemo(() => {
    if (!searchTerm.trim()) return facultyList;
    const term = searchTerm.toLowerCase().trim();
    return facultyList.filter((f) => {
      const name = `${f.firstName || ""} ${f.lastName || ""}`.toLowerCase();
      const username = (f.username || "").toLowerCase();
      const email = (f.email || "").toLowerCase();
      const dept = (f.department || "").toLowerCase();
      const empId = (f.employeeId || "").toLowerCase();
      return (
        name.includes(term) ||
        username.includes(term) ||
        email.includes(term) ||
        dept.includes(term) ||
        empId.includes(term)
      );
    });
  }, [facultyList, searchTerm]);

  // Non-admin view
  if (!isAdmin) {
    return (
      <div className="faculty-page">
        <div className="faculty-error-state">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <h3>Access Restricted</h3>
          <p>Faculty Management is only accessible to users with the Administrator role.</p>
          {onNavigate && (
            <button className="btn-faculty-primary" onClick={() => onNavigate("dashboard")}>
              Back to Dashboard
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="faculty-page">
      {/* Toast Alert */}
      {toast && (
        <div className={`faculty-toast ${toast.type === "error" ? "toast-error" : ""}`} role="status">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <span>{toast.message}</span>
        </div>
      )}

      {/* Header */}
      <div className="faculty-header">
        <div className="faculty-header-left">
          <div className="faculty-title-row">
            <h1>Faculty Management</h1>
            <span className="faculty-count-badge">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <polyline points="16 11 18 13 22 9" />
              </svg>
              {facultyList.length} Active Faculty
            </span>
          </div>
          <p className="faculty-subtitle">
            Manage academic instructors, lecturers, and faculty accounts across all campus departments.
          </p>
        </div>

        <div className="faculty-actions">
          <button
            className="btn-faculty-secondary"
            onClick={() => fetchFaculty(true)}
            disabled={loading || refreshing}
            title="Refresh faculty directory"
          >
            <svg
              className={refreshing ? "refresh-spin" : ""}
              xmlns="http://www.w3.org/2000/svg"
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
            </svg>
            Refresh
          </button>

          <button
            className="btn-faculty-primary"
            onClick={handleOpenModal}
            id="add-faculty-btn"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            Add Faculty
          </button>
        </div>
      </div>

      {/* Toolbar / Search */}
      <div className="faculty-toolbar">
        <div className="faculty-search-wrapper">
          <svg className="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="faculty-search-input"
            placeholder="Search by name, username, email, department, or employee ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button className="faculty-search-clear" onClick={() => setSearchTerm("")} aria-label="Clear search">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* Main Table Card */}
      <div className="faculty-table-card">
        {loading ? (
          <div className="faculty-loading-state">
            <div className="spinner" />
            <p>Loading faculty directory...</p>
          </div>
        ) : error ? (
          <div className="faculty-error-state">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <h3>Service Unavailable</h3>
            <p>{error}</p>
            <button className="btn-faculty-secondary" onClick={() => fetchFaculty(true)}>
              Try Again
            </button>
          </div>
        ) : filteredFaculty.length === 0 ? (
          <div className="faculty-empty-state">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 00-3-3.87" />
              <path d="M16 3.13a4 4 0 010 7.75" />
            </svg>
            <h3>No Faculty Found</h3>
            <p>
              {searchTerm
                ? `No faculty members matched "${searchTerm}". Try a different keyword.`
                : "No faculty members have been registered yet. Click 'Add Faculty' to register one."}
            </p>
          </div>
        ) : (
          <div className="faculty-table-wrapper">
            <table className="faculty-table">
              <thead>
                <tr>
                  <th>Faculty Member</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Employee ID</th>
                  <th>Department</th>
                  <th>Phone</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredFaculty.map((fac) => {
                  const initial = (fac.firstName ? fac.firstName[0] : fac.username ? fac.username[0] : "F").toUpperCase();
                  return (
                    <tr key={fac.id}>
                      <td>
                        <div className="faculty-user-cell">
                          <div className="faculty-avatar">{initial}</div>
                          <div className="faculty-user-info">
                            <span className="faculty-user-name">
                              {fac.firstName} {fac.lastName}
                            </span>
                            <span className="faculty-user-sub">ID: #{fac.id}</span>
                          </div>
                        </div>
                      </td>
                      <td>
                        <code>{fac.username}</code>
                      </td>
                      <td>{fac.email}</td>
                      <td>
                        {fac.employeeId ? (
                          <code>{fac.employeeId}</code>
                        ) : (
                          <span style={{ color: "var(--color-text-muted)" }}>—</span>
                        )}
                      </td>
                      <td>
                        {fac.department ? (
                          <span className="dept-badge">{fac.department}</span>
                        ) : (
                          <span style={{ color: "var(--color-text-muted)" }}>General</span>
                        )}
                      </td>
                      <td>
                        {fac.phone || fac.phoneNumber || (
                          <span style={{ color: "var(--color-text-muted)" }}>—</span>
                        )}
                      </td>
                      <td>
                        <span className="faculty-badge">ACTIVE</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Faculty Modal */}
      {isModalOpen && (
        <div className="faculty-modal-overlay" onClick={handleCloseModal}>
          <div
            className="faculty-modal-container"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
          >
            <div className="faculty-modal-header">
              <h2 id="modal-title">Register New Faculty Account</h2>
              <button
                className="faculty-modal-close-btn"
                onClick={handleCloseModal}
                disabled={submitting}
                aria-label="Close modal"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="faculty-modal-body">
                {submitError && (
                  <div className="faculty-alert-error" role="alert">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span>{submitError}</span>
                  </div>
                )}

                {/* Section 1: Account Information */}
                <div>
                  <div className="faculty-form-section-title">1. Authentication Credentials</div>
                  <div className="faculty-form-grid">
                    <div className="faculty-form-group">
                      <label htmlFor="fac-username">
                        Username <span className="required">*</span>
                      </label>
                      <input
                        id="fac-username"
                        type="text"
                        name="username"
                        className={`faculty-input ${formErrors.username ? "has-error" : ""}`}
                        placeholder="e.g. prof_smith"
                        value={formData.username}
                        onChange={handleInputChange}
                        disabled={submitting}
                        autoComplete="off"
                      />
                      {formErrors.username && <span className="faculty-field-error">{formErrors.username}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-email">
                        Email Address <span className="required">*</span>
                      </label>
                      <input
                        id="fac-email"
                        type="email"
                        name="email"
                        className={`faculty-input ${formErrors.email ? "has-error" : ""}`}
                        placeholder="e.g. smith@smartcampus.edu"
                        value={formData.email}
                        onChange={handleInputChange}
                        disabled={submitting}
                        autoComplete="off"
                      />
                      {formErrors.email && <span className="faculty-field-error">{formErrors.email}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-password">
                        Password <span className="required">*</span>
                      </label>
                      <input
                        id="fac-password"
                        type="password"
                        name="password"
                        className={`faculty-input ${formErrors.password ? "has-error" : ""}`}
                        placeholder="At least 6 characters"
                        value={formData.password}
                        onChange={handleInputChange}
                        disabled={submitting}
                        autoComplete="new-password"
                      />
                      {formErrors.password && <span className="faculty-field-error">{formErrors.password}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-confirm-password">
                        Confirm Password <span className="required">*</span>
                      </label>
                      <input
                        id="fac-confirm-password"
                        type="password"
                        name="confirmPassword"
                        className={`faculty-input ${formErrors.confirmPassword ? "has-error" : ""}`}
                        placeholder="Re-enter password"
                        value={formData.confirmPassword}
                        onChange={handleInputChange}
                        disabled={submitting}
                        autoComplete="new-password"
                      />
                      {formErrors.confirmPassword && (
                        <span className="faculty-field-error">{formErrors.confirmPassword}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Section 2: Faculty Profile Details */}
                <div>
                  <div className="faculty-form-section-title">2. Faculty Profile & Department</div>
                  <div className="faculty-form-grid">
                    <div className="faculty-form-group">
                      <label htmlFor="fac-firstName">
                        First Name <span className="required">*</span>
                      </label>
                      <input
                        id="fac-firstName"
                        type="text"
                        name="firstName"
                        className={`faculty-input ${formErrors.firstName ? "has-error" : ""}`}
                        placeholder="e.g. Robert"
                        value={formData.firstName}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.firstName && <span className="faculty-field-error">{formErrors.firstName}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-lastName">
                        Last Name <span className="required">*</span>
                      </label>
                      <input
                        id="fac-lastName"
                        type="text"
                        name="lastName"
                        className={`faculty-input ${formErrors.lastName ? "has-error" : ""}`}
                        placeholder="e.g. Smith"
                        value={formData.lastName}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.lastName && <span className="faculty-field-error">{formErrors.lastName}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-employeeId">Employee ID</label>
                      <input
                        id="fac-employeeId"
                        type="text"
                        name="employeeId"
                        className={`faculty-input ${formErrors.employeeId ? "has-error" : ""}`}
                        placeholder="e.g. FAC-2024-001"
                        value={formData.employeeId}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.employeeId && <span className="faculty-field-error">{formErrors.employeeId}</span>}
                    </div>

                    <div className="faculty-form-group">
                      <label htmlFor="fac-department">Department</label>
                      <input
                        id="fac-department"
                        type="text"
                        name="department"
                        className={`faculty-input ${formErrors.department ? "has-error" : ""}`}
                        placeholder="e.g. Computer Science"
                        value={formData.department}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.department && <span className="faculty-field-error">{formErrors.department}</span>}
                    </div>

                    <div className="faculty-form-group full-width">
                      <label htmlFor="fac-phoneNumber">Phone Number</label>
                      <input
                        id="fac-phoneNumber"
                        type="text"
                        name="phoneNumber"
                        className={`faculty-input ${formErrors.phoneNumber ? "has-error" : ""}`}
                        placeholder="e.g. +1 (555) 234-5678"
                        value={formData.phoneNumber}
                        onChange={handleInputChange}
                        disabled={submitting}
                      />
                      {formErrors.phoneNumber && (
                        <span className="faculty-field-error">{formErrors.phoneNumber}</span>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              <div className="faculty-modal-footer">
                <button
                  type="button"
                  className="btn-faculty-secondary"
                  onClick={handleCloseModal}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-faculty-primary"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                      Registering...
                    </>
                  ) : (
                    "Register Faculty"
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

export default FacultyPage;

