import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import facilityService from "../services/facilityService";
import "./Facilities.css";

/**
 * Format ISO Date string to human readable format
 */
const formatDate = (dateStr) => {
  if (!dateStr) return "—";
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
};

/**
 * Format time ("09:00:00" -> "9:00 AM")
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
 * Format ISO DateTime string
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

const FACILITY_TYPES = [
  { value: "ALL", label: "All Space Types" },
  { value: "CLASSROOM", label: "Classroom" },
  { value: "LAB", label: "Laboratory" },
  { value: "AUDITORIUM", label: "Auditorium" },
  { value: "SEMINAR_HALL", label: "Seminar Hall" },
  { value: "SPORTS", label: "Sports Complex" },
  { value: "LIBRARY", label: "Library" },
  { value: "OTHER", label: "Other" },
];

const FACILITY_STATUSES = [
  { value: "ALL", label: "All Statuses" },
  { value: "AVAILABLE", label: "Available" },
  { value: "UNAVAILABLE", label: "Unavailable" },
  { value: "MAINTENANCE", label: "Under Maintenance" },
];

const getTypeBadgeClass = (type) => {
  const norm = (type || "").toUpperCase();
  switch (norm) {
    case "CLASSROOM":
      return "badge-type-classroom";
    case "LAB":
      return "badge-type-lab";
    case "AUDITORIUM":
      return "badge-type-auditorium";
    case "SEMINAR_HALL":
      return "badge-type-seminar_hall";
    case "SPORTS":
      return "badge-type-sports";
    case "LIBRARY":
      return "badge-type-library";
    default:
      return "badge-type-other";
  }
};

const getStatusBadgeClass = (status) => {
  const norm = (status || "").toUpperCase();
  switch (norm) {
    case "AVAILABLE":
      return "badge-status-available";
    case "UNAVAILABLE":
      return "badge-status-unavailable";
    case "MAINTENANCE":
      return "badge-status-maintenance";
    default:
      return "badge-status-available";
  }
};

/**
 * FacilitiesPage — Facilities Management module.
 * Provides physical space discovery, room booking, status management,
 * and facility maintenance work orders.
 */
const FacilitiesPage = () => {
  const { user } = useAuth();
  const role = user?.role;
  const isAdmin = role === "ADMIN";
  const isAdminOrFaculty = role === "ADMIN" || role === "FACULTY";

  // Data & Pagination state
  const [facilities, setFacilities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [unauthorized, setUnauthorized] = useState(false);

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters state
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [viewMode, setViewMode] = useState("grid"); // "grid" | "table"

  // Toast feedback
  const [toast, setToast] = useState(null); // { message, type: 'success'|'error'|'info' }

  // Modals state
  // 1. Booking Request Modal
  const [bookingModalFacility, setBookingModalFacility] = useState(null);
  const [bookingForm, setBookingForm] = useState({
    requestDate: new Date().toISOString().split("T")[0],
    startTime: "09:00",
    endTime: "11:00",
    purpose: "",
  });
  const [bookingSubmitting, setBookingSubmitting] = useState(false);
  const [bookingError, setBookingError] = useState(null);

  // 2. Facility Details & Workflows Modal
  const [detailsFacility, setDetailsFacility] = useState(null);
  const [modalTab, setModalTab] = useState("overview"); // "overview" | "requests" | "maintenance"
  const [facilityRequests, setFacilityRequests] = useState([]);
  const [loadingRequests, setLoadingRequests] = useState(false);
  const [maintenanceRecords, setMaintenanceRecords] = useState([]);
  const [loadingMaintenance, setLoadingMaintenance] = useState(false);

  // Maintenance form in modal
  const [showAddMaintenance, setShowAddMaintenance] = useState(false);
  const [maintenanceForm, setMaintenanceForm] = useState({
    issueDescription: "",
    priority: "MEDIUM",
    assignedToUserId: "",
  });
  const [submittingMaintenance, setSubmittingMaintenance] = useState(false);

  // 3. Create Facility Modal (Admin only)
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    name: "",
    facilityType: "CLASSROOM",
    description: "",
    building: "",
    roomNumber: "",
    capacity: 40,
    status: "AVAILABLE",
  });
  const [createSubmitting, setCreateSubmitting] = useState(false);

  // Auto-dismiss toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Fetch facilities list
  const fetchFacilities = useCallback(
    async (
      pageNum = page,
      pageSize = size,
      fType = typeFilter,
      fStatus = statusFilter,
      isRefresh = false
    ) => {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      setUnauthorized(false);

      try {
        const data = await facilityService.getFacilities({
          page: pageNum,
          size: pageSize,
          facilityType: fType !== "ALL" ? fType : undefined,
          status: fStatus !== "ALL" ? fStatus : undefined,
        });

        setFacilities(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
        setPage(data.number ?? pageNum);
      } catch (err) {
        if (err.response?.status === 401 || err.response?.status === 403) {
          setUnauthorized(true);
          setError("You do not have authorization to view campus facilities.");
        } else if (
          err.code === "ERR_NETWORK" ||
          err.response?.status === 500 ||
          err.response?.status === 503
        ) {
          setError(
            "Facility Service is currently unreachable. Please ensure the facility-service is running on port 8086."
          );
        } else {
          setError("Failed to load facilities. Please try again.");
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [page, size, typeFilter, statusFilter]
  );

  useEffect(() => {
    fetchFacilities(page, size, typeFilter, statusFilter);
  }, [page, size, typeFilter, statusFilter, fetchFacilities]);

  // Client-side search filtering
  const filteredFacilities = useMemo(() => {
    if (!searchTerm.trim()) return facilities;
    const term = searchTerm.toLowerCase();
    return facilities.filter(
      (f) =>
        f.name?.toLowerCase().includes(term) ||
        f.building?.toLowerCase().includes(term) ||
        f.roomNumber?.toLowerCase().includes(term) ||
        f.description?.toLowerCase().includes(term) ||
        f.facilityType?.toLowerCase().includes(term)
    );
  }, [facilities, searchTerm]);

  // Metric stats
  const metrics = useMemo(() => {
    const availableCount = facilities.filter((f) => f.status === "AVAILABLE").length;
    const maintenanceCount = facilities.filter((f) => f.status === "MAINTENANCE").length;
    const totalCapacity = facilities.reduce((sum, f) => sum + (f.capacity || 0), 0);
    return {
      total: totalElements || facilities.length,
      available: availableCount,
      maintenance: maintenanceCount,
      totalCapacity,
    };
  }, [facilities, totalElements]);

  // Handle Booking Request Submission
  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    if (!bookingModalFacility) return;

    if (!bookingForm.purpose.trim()) {
      setBookingError("Please state the purpose of the booking reservation.");
      return;
    }

    setBookingSubmitting(true);
    setBookingError(null);

    try {
      // Ensure time string has seconds formatted for Spring LocalTime
      const formattedStartTime =
        bookingForm.startTime.length === 5 ? `${bookingForm.startTime}:00` : bookingForm.startTime;
      const formattedEndTime =
        bookingForm.endTime.length === 5 ? `${bookingForm.endTime}:00` : bookingForm.endTime;

      await facilityService.createBookingRequest(bookingModalFacility.id, {
        requestDate: bookingForm.requestDate,
        startTime: formattedStartTime,
        endTime: formattedEndTime,
        purpose: bookingForm.purpose.trim(),
      });

      setToast({
        type: "success",
        message: `Booking request submitted for "${bookingModalFacility.name}"! Status is PENDING approval.`,
      });
      setBookingModalFacility(null);
      setBookingForm({
        requestDate: new Date().toISOString().split("T")[0],
        startTime: "09:00",
        endTime: "11:00",
        purpose: "",
      });
    } catch (err) {
      if (err.response?.status === 409) {
        setBookingError("Schedule Conflict: Facility is already booked for this requested time slot.");
      } else if (err.response?.data?.message) {
        setBookingError(err.response.data.message);
      } else {
        setBookingError("Failed to submit booking request. Please check inputs and try again.");
      }
    } finally {
      setBookingSubmitting(false);
    }
  };

  // Fetch requests for facility (Admin / Faculty)
  const fetchFacilityRequests = async (facilityId) => {
    if (!isAdminOrFaculty) return;
    setLoadingRequests(true);
    try {
      const data = await facilityService.getFacilityRequests(facilityId);
      setFacilityRequests(data.content || []);
    } catch {
      setFacilityRequests([]);
    } finally {
      setLoadingRequests(false);
    }
  };

  // Update request status (Approve / Reject)
  const handleUpdateRequestStatus = async (facilityId, requestId, status) => {
    try {
      await facilityService.updateRequestStatus(facilityId, requestId, {
        status,
        remarks: `Updated by ${role}`,
      });
      setToast({
        type: "success",
        message: `Booking request marked as ${status}.`,
      });
      fetchFacilityRequests(facilityId);
    } catch (err) {
      setToast({
        type: "error",
        message: err.response?.data?.message || "Failed to update request status.",
      });
    }
  };

  // Fetch maintenance for facility (Admin / Faculty)
  const fetchMaintenance = async (facilityId) => {
    if (!isAdminOrFaculty) return;
    setLoadingMaintenance(true);
    try {
      const data = await facilityService.getMaintenanceRecords(facilityId);
      setMaintenanceRecords(data.content || []);
    } catch {
      setMaintenanceRecords([]);
    } finally {
      setLoadingMaintenance(false);
    }
  };

  // Report maintenance
  const handleMaintenanceSubmit = async (e) => {
    e.preventDefault();
    if (!detailsFacility) return;

    if (!maintenanceForm.issueDescription.trim()) {
      alert("Please enter issue description");
      return;
    }

    setSubmittingMaintenance(true);
    try {
      await facilityService.createMaintenanceRecord(detailsFacility.id, {
        issueDescription: maintenanceForm.issueDescription.trim(),
        priority: maintenanceForm.priority,
        assignedToUserId: maintenanceForm.assignedToUserId ? Number(maintenanceForm.assignedToUserId) : null,
      });

      setToast({
        type: "success",
        message: "Maintenance work order reported successfully.",
      });
      setShowAddMaintenance(false);
      setMaintenanceForm({ issueDescription: "", priority: "MEDIUM", assignedToUserId: "" });
      fetchMaintenance(detailsFacility.id);
    } catch (err) {
      alert(err.response?.data?.message || "Failed to report maintenance issue.");
    } finally {
      setSubmittingMaintenance(false);
    }
  };

  // Update maintenance status
  const handleUpdateMaintenanceStatus = async (facilityId, recordId, status) => {
    try {
      await facilityService.updateMaintenanceStatus(facilityId, recordId, {
        status,
        resolutionNotes: status === "RESOLVED" ? "Issue resolved by campus technician" : undefined,
      });
      setToast({
        type: "success",
        message: `Maintenance status updated to ${status}.`,
      });
      fetchMaintenance(facilityId);
    } catch (err) {
      setToast({
        type: "error",
        message: err.response?.data?.message || "Failed to update maintenance status.",
      });
    }
  };

  // Create facility (Admin)
  const handleCreateFacilitySubmit = async (e) => {
    e.preventDefault();
    if (!createForm.name.trim()) return;

    setCreateSubmitting(true);
    try {
      await facilityService.createFacility({
        name: createForm.name.trim(),
        facilityType: createForm.facilityType,
        description: createForm.description.trim(),
        building: createForm.building.trim(),
        roomNumber: createForm.roomNumber.trim(),
        capacity: Number(createForm.capacity) || 0,
        status: createForm.status,
      });

      setToast({
        type: "success",
        message: `Facility "${createForm.name}" created successfully!`,
      });
      setCreateModalOpen(false);
      setCreateForm({
        name: "",
        facilityType: "CLASSROOM",
        description: "",
        building: "",
        roomNumber: "",
        capacity: 40,
        status: "AVAILABLE",
      });
      fetchFacilities(0, size, typeFilter, statusFilter, true);
    } catch (err) {
      alert(err.response?.data?.message || "Failed to create facility.");
    } finally {
      setCreateSubmitting(false);
    }
  };

  // Admin Quick status toggle
  const handleToggleStatus = async (facilityId, currentStatus) => {
    const nextStatus =
      currentStatus === "AVAILABLE"
        ? "MAINTENANCE"
        : currentStatus === "MAINTENANCE"
          ? "UNAVAILABLE"
          : "AVAILABLE";

    try {
      await facilityService.updateFacilityStatus(facilityId, nextStatus);
      setToast({
        type: "info",
        message: `Facility status updated to ${nextStatus}.`,
      });
      fetchFacilities(page, size, typeFilter, statusFilter, true);
      if (detailsFacility && detailsFacility.id === facilityId) {
        setDetailsFacility((prev) => ({ ...prev, status: nextStatus }));
      }
    } catch (err) {
      alert(err.response?.data?.message || "Failed to update status.");
    }
  };

  // Open details modal
  const openDetailsModal = (facility, tab = "overview") => {
    setDetailsFacility(facility);
    setModalTab(tab);
    if (tab === "requests" && isAdminOrFaculty) {
      fetchFacilityRequests(facility.id);
    } else if (tab === "maintenance" && isAdminOrFaculty) {
      fetchMaintenance(facility.id);
    }
  };

  const handleModalTabChange = (tab) => {
    setModalTab(tab);
    if (!detailsFacility) return;
    if (tab === "requests" && facilityRequests.length === 0) {
      fetchFacilityRequests(detailsFacility.id);
    } else if (tab === "maintenance" && maintenanceRecords.length === 0) {
      fetchMaintenance(detailsFacility.id);
    }
  };

  return (
    <div className="facilities-page">
      {/* Header */}
      <div className="facilities-header">
        <div className="facilities-header-left">
          <div className="facilities-title-row">
            <div className="facilities-title-icon" aria-hidden="true">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 21h18" />
                <path d="M5 21V7l8-4v18" />
                <path d="M19 21V11l-6-4" />
                <line x1="9" y1="9" x2="9" y2="9.01" />
                <line x1="9" y1="13" x2="9" y2="13.01" />
                <line x1="9" y1="17" x2="9" y2="17.01" />
              </svg>
            </div>
            <h1>Facilities Management</h1>
          </div>
          <p className="facilities-subtitle">
            Browse campus auditoriums, seminar halls, lecture rooms, laboratories, and reserve facility spaces in real-time.
          </p>
        </div>

        <div className="facilities-header-actions">
          {isAdmin && (
            <button
              className="btn-primary"
              onClick={() => setCreateModalOpen(true)}
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>Add Facility</span>
            </button>
          )}

          <button
            className="btn-secondary"
            onClick={() => fetchFacilities(page, size, typeFilter, statusFilter, true)}
            disabled={loading || refreshing}
            title="Refresh facility listings"
          >
            <svg
              className={refreshing ? "spinning" : ""}
              xmlns="http://www.w3.org/2000/svg"
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
            <span>{refreshing ? "Refreshing..." : "Refresh"}</span>
          </button>
        </div>
      </div>

      {/* Metrics Strip */}
      <div className="facilities-stats-strip">
        <div className="facility-stat-card">
          <div className="facility-stat-icon total">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 21h18" />
              <path d="M5 21V7l8-4v18" />
              <path d="M19 21V11l-6-4" />
            </svg>
          </div>
          <div className="facility-stat-info">
            <span className="facility-stat-label">Total Facilities</span>
            <span className="facility-stat-value">{metrics.total}</span>
          </div>
        </div>

        <div className="facility-stat-card">
          <div className="facility-stat-icon available">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <div className="facility-stat-info">
            <span className="facility-stat-label">Available Spaces</span>
            <span className="facility-stat-value">{metrics.available}</span>
          </div>
        </div>

        <div className="facility-stat-card">
          <div className="facility-stat-icon maintenance">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z" />
            </svg>
          </div>
          <div className="facility-stat-info">
            <span className="facility-stat-label">Under Maintenance</span>
            <span className="facility-stat-value">{metrics.maintenance}</span>
          </div>
        </div>

        <div className="facility-stat-card">
          <div className="facility-stat-icon capacity">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
              <circle cx="9" cy="7" r="4" />
            </svg>
          </div>
          <div className="facility-stat-info">
            <span className="facility-stat-label">Total Seating</span>
            <span className="facility-stat-value">{metrics.totalCapacity}</span>
          </div>
        </div>
      </div>

      {/* Toast Feedback */}
      {toast && (
        <div className={`facilities-toast ${toast.type}`}>
          <span>{toast.message}</span>
          <button
            className="facilities-toast-close"
            onClick={() => setToast(null)}
            aria-label="Dismiss message"
          >
            ×
          </button>
        </div>
      )}

      {/* Controls / Filter Bar */}
      <div className="facilities-controls">
        <div className="facilities-controls-left">
          <div className="facilities-search-box">
            <svg
              className="facilities-search-icon"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              className="facilities-search-input"
              placeholder="Search by facility, building, room, or keyword..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="facilities-filter-group">
            <select
              className="facilities-select"
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by facility type"
            >
              {FACILITY_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>

            <select
              className="facilities-select"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by facility availability status"
            >
              {FACILITY_STATUSES.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="facilities-controls-right">
          <div className="view-switch" role="group" aria-label="View mode">
            <button
              className={`view-btn ${viewMode === "grid" ? "active" : ""}`}
              onClick={() => setViewMode("grid")}
              title="Grid view"
              aria-label="Grid view"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
              </svg>
            </button>
            <button
              className={`view-btn ${viewMode === "table" ? "active" : ""}`}
              onClick={() => setViewMode("table")}
              title="Table view"
              aria-label="Table view"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="8" y1="6" x2="21" y2="6" />
                <line x1="8" y1="12" x2="21" y2="12" />
                <line x1="8" y1="18" x2="21" y2="18" />
                <line x1="3" y1="6" x2="3.01" y2="6" />
                <line x1="3" y1="12" x2="3.01" y2="12" />
                <line x1="3" y1="18" x2="3.01" y2="18" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      {loading ? (
        <div className="facilities-skeleton-grid">
          {[1, 2, 3, 4, 5, 6].map((idx) => (
            <div key={idx} className="facilities-skeleton-card">
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <div className="skeleton-shimmer" style={{ width: 90, height: 22 }} />
                <div className="skeleton-shimmer" style={{ width: 80, height: 22 }} />
              </div>
              <div className="skeleton-shimmer" style={{ width: "80%", height: 20 }} />
              <div className="skeleton-shimmer" style={{ width: "100%", height: 32 }} />
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <div className="skeleton-shimmer" style={{ width: "65%", height: 14 }} />
                <div className="skeleton-shimmer" style={{ width: "50%", height: 14 }} />
              </div>
              <div className="skeleton-shimmer" style={{ width: "100%", height: 36, marginTop: "auto" }} />
            </div>
          ))}
        </div>
      ) : unauthorized ? (
        <div className="facilities-error-state">
          <div className="facilities-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0110 0v4" />
            </svg>
          </div>
          <h2 className="facilities-state-title">Access Restricted</h2>
          <p className="facilities-state-desc">{error}</p>
        </div>
      ) : error ? (
        <div className="facilities-error-state">
          <div className="facilities-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h2 className="facilities-state-title">Unable to Load Facilities</h2>
          <p className="facilities-state-desc">{error}</p>
          <button
            className="btn-primary"
            style={{ maxWidth: 160 }}
            onClick={() => fetchFacilities(page, size, typeFilter, statusFilter, true)}
          >
            Try Again
          </button>
        </div>
      ) : filteredFacilities.length === 0 ? (
        <div className="facilities-empty-state">
          <div className="facilities-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 21h18" />
              <path d="M5 21V7l8-4v18" />
              <line x1="9" y1="9" x2="9" y2="9.01" />
              <line x1="9" y1="13" x2="9" y2="13.01" />
            </svg>
          </div>
          <h2 className="facilities-state-title">No Facilities Found</h2>
          <p className="facilities-state-desc">
            {searchTerm || typeFilter !== "ALL" || statusFilter !== "ALL"
              ? "No facilities match your search criteria. Try modifying your filters."
              : "There are currently no facilities listed on the campus portal."}
          </p>
          {(searchTerm || typeFilter !== "ALL" || statusFilter !== "ALL") && (
            <button
              className="btn-secondary"
              onClick={() => {
                setSearchTerm("");
                setTypeFilter("ALL");
                setStatusFilter("ALL");
              }}
            >
              Clear All Filters
            </button>
          )}
        </div>
      ) : viewMode === "grid" ? (
        /* Card Grid View */
        <div className="facilities-grid">
          {filteredFacilities.map((facility) => {
            const isAvailable = facility.status === "AVAILABLE";

            return (
              <div key={facility.id} className="facility-card">
                <div className="facility-card-header">
                  <span className={`facility-type-badge ${getTypeBadgeClass(facility.facilityType)}`}>
                    {facility.facilityType || "SPACE"}
                  </span>
                  <span className={`facility-status-badge ${getStatusBadgeClass(facility.status)}`}>
                    <span className="facility-status-dot" />
                    {facility.status}
                  </span>
                </div>

                <div className="facility-card-body">
                  <h3 className="facility-card-title">{facility.name}</h3>
                  <p className="facility-card-desc">
                    {facility.description || "No description provided."}
                  </p>

                  <div className="facility-card-meta">
                    <div className="facility-meta-item">
                      <svg className="facility-meta-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M3 21h18" />
                        <path d="M5 21V7l8-4v18" />
                      </svg>
                      <span>
                        {facility.building || "Main Campus"} {facility.roomNumber ? `• Room ${facility.roomNumber}` : ""}
                      </span>
                    </div>

                    <div className="facility-meta-item">
                      <svg className="facility-meta-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                        <circle cx="9" cy="7" r="4" />
                      </svg>
                      <span>Capacity: {facility.capacity ? `${facility.capacity} seats` : "Flexible"}</span>
                    </div>
                  </div>
                </div>

                <div className="facility-card-actions">
                  <button
                    className="btn-secondary"
                    style={{ flex: 1 }}
                    onClick={() => openDetailsModal(facility, "overview")}
                  >
                    Details
                  </button>

                  <button
                    className="btn-primary"
                    style={{ flex: 1.2 }}
                    onClick={() => {
                      setBookingModalFacility(facility);
                      setBookingError(null);
                    }}
                    disabled={!isAvailable}
                    title={!isAvailable ? `Facility is ${facility.status.toLowerCase()}` : "Reserve space"}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="4" width="18" height="18" rx="2" />
                      <line x1="16" y1="2" x2="16" y2="6" />
                      <line x1="8" y1="2" x2="8" y2="6" />
                      <line x1="3" y1="10" x2="21" y2="10" />
                    </svg>
                    <span>Book</span>
                  </button>

                  {isAdminOrFaculty && (
                    <button
                      className="btn-secondary"
                      onClick={() => openDetailsModal(facility, "requests")}
                      title="Inspect bookings and maintenance"
                    >
                      Manage
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <div className="facilities-table-wrapper">
          <table className="facilities-table">
            <thead>
              <tr>
                <th>Facility Name</th>
                <th>Type</th>
                <th>Location</th>
                <th>Capacity</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredFacilities.map((facility) => {
                const isAvailable = facility.status === "AVAILABLE";

                return (
                  <tr key={facility.id}>
                    <td>
                      <div className="facility-cell-title">
                        <span className="facility-cell-name">{facility.name}</span>
                        <span className="facility-cell-desc">
                          {facility.description || "No description"}
                        </span>
                      </div>
                    </td>
                    <td>
                      <span className={`facility-type-badge ${getTypeBadgeClass(facility.facilityType)}`}>
                        {facility.facilityType}
                      </span>
                    </td>
                    <td>
                      <div>{facility.building || "Main Block"}</div>
                      <div style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>
                        {facility.roomNumber ? `Room ${facility.roomNumber}` : ""}
                      </div>
                    </td>
                    <td>{facility.capacity ? `${facility.capacity} seats` : "—"}</td>
                    <td>
                      <span className={`facility-status-badge ${getStatusBadgeClass(facility.status)}`}>
                        <span className="facility-status-dot" />
                        {facility.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: 6 }}>
                        <button
                          className="btn-secondary"
                          style={{ padding: "5px 10px", fontSize: "12px" }}
                          onClick={() => openDetailsModal(facility, "overview")}
                        >
                          Details
                        </button>
                        <button
                          className="btn-primary"
                          style={{ padding: "5px 10px", fontSize: "12px" }}
                          onClick={() => {
                            setBookingModalFacility(facility);
                            setBookingError(null);
                          }}
                          disabled={!isAvailable}
                        >
                          Book
                        </button>
                        {isAdminOrFaculty && (
                          <button
                            className="btn-secondary"
                            style={{ padding: "5px 10px", fontSize: "12px" }}
                            onClick={() => openDetailsModal(facility, "requests")}
                          >
                            Manage
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {!loading && totalPages > 1 && (
        <div className="facilities-pagination">
          <div className="pagination-info">
            Showing Page {page + 1} of {totalPages} (Total {totalElements} facilities)
          </div>

          <div className="pagination-controls">
            <div className="page-size-selector">
              <span>Show</span>
              <select
                className="page-size-select"
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
              <span>per page</span>
            </div>

            <button
              className="page-btn"
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              disabled={page === 0}
            >
              Previous
            </button>
            <button
              className="page-btn"
              onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
              disabled={page >= totalPages - 1}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* 1. Booking Request Modal */}
      {bookingModalFacility && (
        <div className="modal-overlay" onClick={() => setBookingModalFacility(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 540 }}>
            <div className="modal-header">
              <div className="modal-header-left">
                <h2 style={{ fontSize: "18px", margin: 0, color: "var(--color-text-heading)" }}>
                  Book {bookingModalFacility.name}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setBookingModalFacility(null)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleBookingSubmit}>
              <div className="modal-body">
                {bookingError && (
                  <div style={{ padding: "10px 14px", background: "rgba(239, 68, 68, 0.15)", border: "1px solid rgba(239, 68, 68, 0.3)", borderRadius: 6, color: "#f87171", fontSize: "13px" }}>
                    {bookingError}
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Reservation Date *</label>
                  <input
                    type="date"
                    className="form-input"
                    value={bookingForm.requestDate}
                    min={new Date().toISOString().split("T")[0]}
                    onChange={(e) => setBookingForm((p) => ({ ...p, requestDate: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Start Time *</label>
                    <input
                      type="time"
                      className="form-input"
                      value={bookingForm.startTime}
                      onChange={(e) => setBookingForm((p) => ({ ...p, startTime: e.target.value }))}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">End Time *</label>
                    <input
                      type="time"
                      className="form-input"
                      value={bookingForm.endTime}
                      onChange={(e) => setBookingForm((p) => ({ ...p, endTime: e.target.value }))}
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Purpose / Event Description *</label>
                  <textarea
                    className="form-textarea"
                    placeholder="Specify the reason or academic purpose for this reservation..."
                    value={bookingForm.purpose}
                    maxLength={300}
                    onChange={(e) => setBookingForm((p) => ({ ...p, purpose: e.target.value }))}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setBookingModalFacility(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={bookingSubmitting}
                >
                  {bookingSubmitting ? "Submitting..." : "Submit Reservation"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Facility Details & Workflows Modal */}
      {detailsFacility && (
        <div className="modal-overlay" onClick={() => setDetailsFacility(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-header-left">
                <span className={`facility-type-badge ${getTypeBadgeClass(detailsFacility.facilityType)}`}>
                  {detailsFacility.facilityType}
                </span>
                <span className={`facility-status-badge ${getStatusBadgeClass(detailsFacility.status)}`}>
                  <span className="facility-status-dot" />
                  {detailsFacility.status}
                </span>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setDetailsFacility(null)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            {isAdminOrFaculty && (
              <div className="modal-tabs">
                <button
                  className={`modal-tab-btn ${modalTab === "overview" ? "active" : ""}`}
                  onClick={() => handleModalTabChange("overview")}
                >
                  Overview & Specs
                </button>
                <button
                  className={`modal-tab-btn ${modalTab === "requests" ? "active" : ""}`}
                  onClick={() => handleModalTabChange("requests")}
                >
                  Booking Requests
                </button>
                <button
                  className={`modal-tab-btn ${modalTab === "maintenance" ? "active" : ""}`}
                  onClick={() => handleModalTabChange("maintenance")}
                >
                  Maintenance Work Orders
                </button>
              </div>
            )}

            <div className="modal-body">
              {modalTab === "overview" ? (
                <>
                  <h2 style={{ fontSize: "20px", color: "var(--color-text-heading)", margin: 0 }}>
                    {detailsFacility.name}
                  </h2>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Description:</span>
                    <span className="modal-detail-val">
                      {detailsFacility.description || "No description specified."}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Building / Block:</span>
                    <span className="modal-detail-val">
                      {detailsFacility.building || "Campus Main Complex"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Room Number:</span>
                    <span className="modal-detail-val">
                      {detailsFacility.roomNumber || "Not designated"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Capacity:</span>
                    <span className="modal-detail-val">
                      {detailsFacility.capacity ? `${detailsFacility.capacity} occupants` : "Open space"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Current Status:</span>
                    <span className="modal-detail-val">
                      <strong>{detailsFacility.status}</strong>
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Registered On:</span>
                    <span className="modal-detail-val">
                      {formatDateTime(detailsFacility.createdAt)}
                    </span>
                  </div>
                </>
              ) : modalTab === "requests" ? (
                /* Bookings Tab */
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h3 style={{ fontSize: "16px", color: "var(--color-text-heading)", margin: 0 }}>
                      Booking Reservations ({facilityRequests.length})
                    </h3>
                    <button
                      className="btn-secondary"
                      onClick={() => fetchFacilityRequests(detailsFacility.id)}
                      disabled={loadingRequests}
                    >
                      <span>{loadingRequests ? "Loading..." : "Refresh"}</span>
                    </button>
                  </div>

                  {loadingRequests ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      Loading reservation requests...
                    </div>
                  ) : facilityRequests.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      No booking requests found for this facility.
                    </div>
                  ) : (
                    <div className="facilities-table-wrapper">
                      <table className="sub-table">
                        <thead>
                          <tr>
                            <th>Requester</th>
                            <th>Date</th>
                            <th>Time</th>
                            <th>Purpose</th>
                            <th>Status</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {facilityRequests.map((req) => (
                            <tr key={req.id}>
                              <td>User #{req.requestedByUserId}</td>
                              <td>{formatDate(req.requestDate)}</td>
                              <td>{formatTime(req.startTime)} - {formatTime(req.endTime)}</td>
                              <td style={{ maxWidth: 160, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                {req.purpose}
                              </td>
                              <td>
                                <span className={`facility-status-badge ${req.status === "APPROVED" ? "badge-status-available" : req.status === "PENDING" ? "badge-status-maintenance" : "badge-status-unavailable"}`}>
                                  {req.status}
                                </span>
                              </td>
                              <td>
                                {req.status === "PENDING" && isAdminOrFaculty && (
                                  <div style={{ display: "flex", gap: 4 }}>
                                    <button
                                      className="btn-primary"
                                      style={{ padding: "3px 8px", fontSize: "11px" }}
                                      onClick={() => handleUpdateRequestStatus(detailsFacility.id, req.id, "APPROVED")}
                                    >
                                      Approve
                                    </button>
                                    <button
                                      className="btn-secondary"
                                      style={{ padding: "3px 8px", fontSize: "11px", color: "var(--color-danger)" }}
                                      onClick={() => handleUpdateRequestStatus(detailsFacility.id, req.id, "REJECTED")}
                                    >
                                      Reject
                                    </button>
                                  </div>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              ) : (
                /* Maintenance Tab */
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h3 style={{ fontSize: "16px", color: "var(--color-text-heading)", margin: 0 }}>
                      Maintenance Work Orders ({maintenanceRecords.length})
                    </h3>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        className="btn-primary"
                        style={{ padding: "6px 12px", fontSize: "12px" }}
                        onClick={() => setShowAddMaintenance((prev) => !prev)}
                      >
                        {showAddMaintenance ? "Cancel Form" : "+ Report Issue"}
                      </button>
                      <button
                        className="btn-secondary"
                        onClick={() => fetchMaintenance(detailsFacility.id)}
                        disabled={loadingMaintenance}
                      >
                        <span>{loadingMaintenance ? "..." : "Refresh"}</span>
                      </button>
                    </div>
                  </div>

                  {showAddMaintenance && (
                    <form onSubmit={handleMaintenanceSubmit} style={{ background: "var(--color-bg-card)", padding: 16, borderRadius: 8, border: "1px solid var(--color-border)", display: "flex", flexDirection: "column", gap: 12 }}>
                      <h4 style={{ margin: 0, fontSize: "14px", color: "var(--color-text-heading)" }}>
                        Log New Maintenance Issue
                      </h4>
                      <div className="form-group">
                        <label className="form-label">Issue Description *</label>
                        <textarea
                          className="form-textarea"
                          placeholder="Describe the physical or technical issue in this facility..."
                          value={maintenanceForm.issueDescription}
                          onChange={(e) => setMaintenanceForm((p) => ({ ...p, issueDescription: e.target.value }))}
                          required
                        />
                      </div>
                      <div className="form-row">
                        <div className="form-group">
                          <label className="form-label">Priority</label>
                          <select
                            className="form-select"
                            value={maintenanceForm.priority}
                            onChange={(e) => setMaintenanceForm((p) => ({ ...p, priority: e.target.value }))}
                          >
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                            <option value="CRITICAL">Critical</option>
                          </select>
                        </div>
                        <div className="form-group">
                          <label className="form-label">Assignee User ID (Optional)</label>
                          <input
                            type="number"
                            className="form-input"
                            placeholder="e.g. 15"
                            value={maintenanceForm.assignedToUserId}
                            onChange={(e) => setMaintenanceForm((p) => ({ ...p, assignedToUserId: e.target.value }))}
                          />
                        </div>
                      </div>
                      <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
                        <button
                          type="submit"
                          className="btn-primary"
                          disabled={submittingMaintenance}
                        >
                          {submittingMaintenance ? "Logging..." : "Create Work Order"}
                        </button>
                      </div>
                    </form>
                  )}

                  {loadingMaintenance ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      Loading maintenance records...
                    </div>
                  ) : maintenanceRecords.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      No maintenance work orders logged for this space.
                    </div>
                  ) : (
                    <div className="facilities-table-wrapper">
                      <table className="sub-table">
                        <thead>
                          <tr>
                            <th>Order ID</th>
                            <th>Description</th>
                            <th>Priority</th>
                            <th>Reported</th>
                            <th>Status</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {maintenanceRecords.map((m) => (
                            <tr key={m.id}>
                              <td>#{m.id}</td>
                              <td style={{ maxWidth: 180, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                {m.issueDescription}
                              </td>
                              <td>
                                <span style={{ fontWeight: 600, fontSize: "11.5px", color: m.priority === "CRITICAL" ? "var(--color-danger)" : m.priority === "HIGH" ? "var(--color-warning)" : "inherit" }}>
                                  {m.priority}
                                </span>
                              </td>
                              <td>{formatDate(m.reportedAt)}</td>
                              <td>
                                <span className={`facility-status-badge ${m.status === "RESOLVED" ? "badge-status-available" : m.status === "IN_PROGRESS" ? "badge-status-maintenance" : "badge-status-unavailable"}`}>
                                  {m.status}
                                </span>
                              </td>
                              <td>
                                {m.status === "OPEN" && (
                                  <button
                                    className="btn-secondary"
                                    style={{ padding: "3px 8px", fontSize: "11px" }}
                                    onClick={() => handleUpdateMaintenanceStatus(detailsFacility.id, m.id, "IN_PROGRESS")}
                                  >
                                    Start
                                  </button>
                                )}
                                {m.status === "IN_PROGRESS" && (
                                  <button
                                    className="btn-primary"
                                    style={{ padding: "3px 8px", fontSize: "11px" }}
                                    onClick={() => handleUpdateMaintenanceStatus(detailsFacility.id, m.id, "RESOLVED")}
                                  >
                                    Resolve
                                  </button>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="modal-footer">
              {isAdmin && modalTab === "overview" && (
                <button
                  className="btn-secondary"
                  onClick={() => handleToggleStatus(detailsFacility.id, detailsFacility.status)}
                  title="Cycle status: AVAILABLE -> MAINTENANCE -> UNAVAILABLE"
                >
                  Cycle Status
                </button>
              )}

              {detailsFacility.status === "AVAILABLE" && (
                <button
                  className="btn-primary"
                  onClick={() => {
                    setBookingModalFacility(detailsFacility);
                    setDetailsFacility(null);
                  }}
                >
                  Book this Facility
                </button>
              )}

              <button className="btn-secondary" onClick={() => setDetailsFacility(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 3. Create Facility Modal (Admin Only) */}
      {createModalOpen && (
        <div className="modal-overlay" onClick={() => setCreateModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 580 }}>
            <div className="modal-header">
              <div className="modal-header-left">
                <h2 style={{ fontSize: "18px", margin: 0, color: "var(--color-text-heading)" }}>
                  Create New Campus Facility
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setCreateModalOpen(false)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateFacilitySubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Facility Name *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Science Block Auditorium 1"
                    value={createForm.name}
                    onChange={(e) => setCreateForm((p) => ({ ...p, name: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Facility Type *</label>
                    <select
                      className="form-select"
                      value={createForm.facilityType}
                      onChange={(e) => setCreateForm((p) => ({ ...p, facilityType: e.target.value }))}
                    >
                      <option value="CLASSROOM">Classroom</option>
                      <option value="LAB">Laboratory</option>
                      <option value="AUDITORIUM">Auditorium</option>
                      <option value="SEMINAR_HALL">Seminar Hall</option>
                      <option value="SPORTS">Sports Complex</option>
                      <option value="LIBRARY">Library</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Initial Status</label>
                    <select
                      className="form-select"
                      value={createForm.status}
                      onChange={(e) => setCreateForm((p) => ({ ...p, status: e.target.value }))}
                    >
                      <option value="AVAILABLE">Available</option>
                      <option value="UNAVAILABLE">Unavailable</option>
                      <option value="MAINTENANCE">Maintenance</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Building Name</label>
                    <input
                      type="text"
                      className="form-input"
                      placeholder="e.g. Main Academic Block"
                      value={createForm.building}
                      onChange={(e) => setCreateForm((p) => ({ ...p, building: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Room Number</label>
                    <input
                      type="text"
                      className="form-input"
                      placeholder="e.g. MB-204"
                      value={createForm.roomNumber}
                      onChange={(e) => setCreateForm((p) => ({ ...p, roomNumber: e.target.value }))}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Occupant Capacity</label>
                  <input
                    type="number"
                    className="form-input"
                    value={createForm.capacity}
                    min={1}
                    onChange={(e) => setCreateForm((p) => ({ ...p, capacity: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Description / Equipment Details</label>
                  <textarea
                    className="form-textarea"
                    placeholder="Details about A/V equipment, projectors, seating setup, etc."
                    value={createForm.description}
                    onChange={(e) => setCreateForm((p) => ({ ...p, description: e.target.value }))}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setCreateModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={createSubmitting}
                >
                  {createSubmitting ? "Creating..." : "Save Facility"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default FacilitiesPage;
