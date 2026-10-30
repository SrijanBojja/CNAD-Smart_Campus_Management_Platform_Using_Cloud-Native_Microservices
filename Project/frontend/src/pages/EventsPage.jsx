import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import eventService from "../services/eventService";
import "./Events.css";

/**
 * Format ISO DateTime string to readable format
 * e.g. "2026-10-01T09:00:00" -> "Oct 1, 2026, 9:00 AM"
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

/**
 * Format date range between start and end date-time
 */
const formatDateRange = (startStr, endStr) => {
  if (!startStr) return "—";
  const start = formatDateTime(startStr);
  if (!endStr) return start;
  const end = formatDateTime(endStr);
  return `${start} – ${end}`;
};

/**
 * Map event type to CSS badge class
 */
const getTypeBadgeClass = (type) => {
  const norm = (type || "").toUpperCase();
  switch (norm) {
    case "TECHNICAL":
      return "badge-type-technical";
    case "WORKSHOP":
      return "badge-type-workshop";
    case "SEMINAR":
      return "badge-type-seminar";
    case "ACADEMIC":
      return "badge-type-academic";
    case "CULTURAL":
      return "badge-type-cultural";
    case "SPORTS":
      return "badge-type-sports";
    default:
      return "badge-type-other";
  }
};

/**
 * Map event status to CSS badge class
 */
const getStatusBadgeClass = (status) => {
  const norm = (status || "").toUpperCase();
  switch (norm) {
    case "UPCOMING":
      return "badge-status-upcoming";
    case "ONGOING":
      return "badge-status-ongoing";
    case "COMPLETED":
      return "badge-status-completed";
    case "CANCELLED":
      return "badge-status-cancelled";
    default:
      return "badge-status-upcoming";
  }
};

const EVENT_TYPES = [
  { value: "ALL", label: "All Categories" },
  { value: "TECHNICAL", label: "Technical" },
  { value: "WORKSHOP", label: "Workshop" },
  { value: "SEMINAR", label: "Seminar" },
  { value: "ACADEMIC", label: "Academic" },
  { value: "CULTURAL", label: "Cultural" },
  { value: "SPORTS", label: "Sports" },
  { value: "OTHER", label: "Other" },
];

const EVENT_STATUSES = [
  { value: "ALL", label: "All Statuses" },
  { value: "UPCOMING", label: "Upcoming" },
  { value: "ONGOING", label: "Ongoing" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
];

const initialEventForm = {
  title: "",
  description: "",
  eventType: "TECHNICAL",
  startDateTime: "",
  endDateTime: "",
  location: "",
  capacity: 100,
  status: "UPCOMING",
};

/**
 * EventsPage — Events Management module.
 * Provides real-time event discovery, category/status filtering, search,
 * event creation, editing, deletion, event registration, and attendee inspection.
 */
const EventsPage = () => {
  const { user } = useAuth();
  const userRole = user?.role;
  const isAdminOrFaculty = userRole === "ADMIN" || userRole === "FACULTY";

  // Data & Pagination state
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [unauthorized, setUnauthorized] = useState(false);

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters state
  const [eventTypeFilter, setEventTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [viewMode, setViewMode] = useState("grid"); // "grid" | "table"

  // Registration & Toast feedback
  const [registeringId, setRegisteringId] = useState(null);
  const [toast, setToast] = useState(null); // { message, type: 'success'|'error'|'info' }

  // Modal states
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [modalTab, setModalTab] = useState("details"); // "details" | "registrations"
  const [registrations, setRegistrations] = useState([]);
  const [loadingRegistrations, setLoadingRegistrations] = useState(false);
  const [registrationsError, setRegistrationsError] = useState(null);
  const [cancellingRegId, setCancellingRegId] = useState(null);

  // Add/Edit Event Modal state
  const [eventFormModal, setEventFormModal] = useState({ open: false, mode: "create", item: null });
  const [eventFormData, setEventFormData] = useState(initialEventForm);
  const [eventFormErrors, setEventFormErrors] = useState({});
  const [eventSubmitting, setEventSubmitting] = useState(false);
  const [eventModalError, setEventModalError] = useState(null);

  // Delete Confirmation Modal state
  const [deleteEventModal, setDeleteEventModal] = useState({ open: false, item: null, loading: false, error: null });

  // Auto-dismiss toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Fetch events from backend
  const fetchEvents = useCallback(
    async (
      pageNum = page,
      pageSize = size,
      eType = eventTypeFilter,
      stat = statusFilter,
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
        const data = await eventService.getEvents({
          page: pageNum,
          size: pageSize,
          eventType: eType !== "ALL" ? eType : undefined,
          status: stat !== "ALL" ? stat : undefined,
        });

        setEvents(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
        setPage(data.number ?? pageNum);
      } catch (err) {
        if (err.response?.status === 401 || err.response?.status === 403) {
          setUnauthorized(true);
          setError("You do not have authorization to view campus events.");
        } else if (
          err.code === "ERR_NETWORK" ||
          err.response?.status === 500 ||
          err.response?.status === 503
        ) {
          setError(
            "Event Service is currently unreachable. Please ensure the event-service is running on port 8085."
          );
        } else {
          setError("Failed to load events. Please try again.");
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [page, size, eventTypeFilter, statusFilter]
  );

  useEffect(() => {
    fetchEvents(page, size, eventTypeFilter, statusFilter);
  }, [fetchEvents, page, size, eventTypeFilter, statusFilter]);

  // Client-side search filtering
  const filteredEvents = useMemo(() => {
    if (!searchTerm.trim()) return events;
    const term = searchTerm.toLowerCase();
    return events.filter(
      (ev) =>
        (ev.title && ev.title.toLowerCase().includes(term)) ||
        (ev.location && ev.location.toLowerCase().includes(term)) ||
        (ev.description && ev.description.toLowerCase().includes(term)) ||
        (ev.eventType && ev.eventType.toLowerCase().includes(term))
    );
  }, [events, searchTerm]);

  // Metric counts
  const metrics = useMemo(() => {
    const total = totalElements;
    const upcoming = events.filter((e) => e.status === "UPCOMING").length;
    const ongoing = events.filter((e) => e.status === "ONGOING").length;
    const completed = events.filter((e) => e.status === "COMPLETED").length;
    return { total, upcoming, ongoing, completed };
  }, [events, totalElements]);

  // Handle Event Registration (Students & General Users)
  const handleRegister = async (eventId, eventTitle) => {
    setRegisteringId(eventId);
    try {
      await eventService.registerForEvent(eventId);
      setToast({
        type: "success",
        message: `Successfully registered for "${eventTitle}"!`,
      });
      fetchEvents(page, size, eventTypeFilter, statusFilter, true);
    } catch (err) {
      const errMsg =
        err.response?.status === 409
          ? "You are already registered for this event."
          : err.response?.data?.message || "Failed to complete registration.";
      setToast({ type: "error", message: errMsg });
    } finally {
      setRegisteringId(null);
    }
  };

  // Inspect Event Attendees (Admin & Faculty)
  const fetchRegistrations = async (eventId) => {
    setLoadingRegistrations(true);
    setRegistrationsError(null);
    try {
      const data = await eventService.getEventRegistrations(eventId);
      setRegistrations(data.content || []);
    } catch (err) {
      setRegistrationsError(
        err.response?.data?.message || "Failed to load attendees."
      );
    } finally {
      setLoadingRegistrations(false);
    }
  };

  // Cancel Registration
  const handleCancelRegistration = async (eventId, regId) => {
    setCancellingRegId(regId);
    try {
      await eventService.cancelRegistration(eventId, regId);
      setToast({
        type: "info",
        message: "Registration cancelled successfully.",
      });
      fetchRegistrations(eventId);
      fetchEvents(page, size, eventTypeFilter, statusFilter, true);
    } catch (err) {
      setToast({
        type: "error",
        message:
          err.response?.data?.message || "Failed to cancel registration.",
      });
    } finally {
      setCancellingRegId(null);
    }
  };

  // Open modal for details
  const openDetailsModal = (event, initialTab = "details") => {
    setSelectedEvent(event);
    setModalTab(initialTab);
    if (initialTab === "registrations" && isAdminOrFaculty) {
      fetchRegistrations(event.id);
    }
  };

  const closeModal = () => {
    setSelectedEvent(null);
    setRegistrations([]);
    setRegistrationsError(null);
  };

  // Switch modal tab
  const handleModalTabChange = (tab) => {
    setModalTab(tab);
    if (tab === "registrations" && selectedEvent && registrations.length === 0) {
      fetchRegistrations(selectedEvent.id);
    }
  };

  // ==========================================
  // EVENT FORM (CREATE / EDIT)
  // ==========================================
  const handleOpenAddEvent = () => {
    // Default dates: tomorrow at 09:00 and 17:00
    const now = new Date();
    const tomorrow = new Date(now.setDate(now.getDate() + 1));
    const pad = (n) => String(n).padStart(2, "0");
    const yyyy = tomorrow.getFullYear();
    const mm = pad(tomorrow.getMonth() + 1);
    const dd = pad(tomorrow.getDate());

    setEventFormData({
      ...initialEventForm,
      startDateTime: `${yyyy}-${mm}-${dd}T09:00`,
      endDateTime: `${yyyy}-${mm}-${dd}T17:00`,
    });
    setEventFormErrors({});
    setEventModalError(null);
    setEventFormModal({ open: true, mode: "create", item: null });
  };

  const handleOpenEditEvent = (event) => {
    // Format startDateTime and endDateTime for datetime-local input
    const toInputVal = (dtStr) => {
      if (!dtStr) return "";
      return dtStr.length > 16 ? dtStr.substring(0, 16) : dtStr;
    };

    setEventFormData({
      title: event.title || "",
      description: event.description || "",
      eventType: event.eventType || "TECHNICAL",
      startDateTime: toInputVal(event.startDateTime),
      endDateTime: toInputVal(event.endDateTime),
      location: event.location || "",
      capacity: event.capacity ?? 100,
      status: event.status || "UPCOMING",
    });
    setEventFormErrors({});
    setEventModalError(null);
    setEventFormModal({ open: true, mode: "edit", item: event });
  };

  const validateEventForm = () => {
    const errs = {};
    if (!eventFormData.title.trim()) errs.title = "Event Title is required";
    if (!eventFormData.eventType) errs.eventType = "Event Category is required";
    if (!eventFormData.startDateTime) errs.startDateTime = "Start date & time is required";
    if (!eventFormData.endDateTime) errs.endDateTime = "End date & time is required";

    if (eventFormData.startDateTime && eventFormData.endDateTime) {
      if (new Date(eventFormData.endDateTime) <= new Date(eventFormData.startDateTime)) {
        errs.endDateTime = "End time must be strictly after start time";
      }
    }

    if (eventFormData.capacity && Number(eventFormData.capacity) < 1) {
      errs.capacity = "Capacity must be at least 1";
    }

    setEventFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitEventForm = async (e) => {
    e.preventDefault();
    if (!validateEventForm()) return;

    setEventSubmitting(true);
    setEventModalError(null);

    const formatToIso = (dt) => (dt && dt.length === 16 ? `${dt}:00` : dt);

    try {
      if (eventFormModal.mode === "create") {
        await eventService.createEvent({
          title: eventFormData.title.trim(),
          description: eventFormData.description.trim() || undefined,
          eventType: eventFormData.eventType,
          startDateTime: formatToIso(eventFormData.startDateTime),
          endDateTime: formatToIso(eventFormData.endDateTime),
          location: eventFormData.location.trim() || undefined,
          capacity: eventFormData.capacity ? Number(eventFormData.capacity) : undefined,
        });
        setToast({ type: "success", message: `Event "${eventFormData.title.trim()}" created successfully!` });
      } else {
        await eventService.updateEvent(eventFormModal.item.id, {
          title: eventFormData.title.trim(),
          description: eventFormData.description.trim() || undefined,
          eventType: eventFormData.eventType,
          startDateTime: formatToIso(eventFormData.startDateTime),
          endDateTime: formatToIso(eventFormData.endDateTime),
          location: eventFormData.location.trim() || undefined,
          capacity: eventFormData.capacity ? Number(eventFormData.capacity) : undefined,
          status: eventFormData.status,
        });
        setToast({ type: "success", message: `Event "${eventFormData.title.trim()}" updated successfully!` });
      }

      setEventFormModal({ open: false, mode: "create", item: null });
      if (selectedEvent && selectedEvent.id === eventFormModal.item?.id) {
        closeModal();
      }
      fetchEvents(page, size, eventTypeFilter, statusFilter, true);
    } catch (err) {
      setEventModalError(err.response?.data?.message || "Failed to save campus event. Please check inputs.");
    } finally {
      setEventSubmitting(false);
    }
  };

  const handleDeleteEvent = async () => {
    if (!deleteEventModal.item) return;
    setDeleteEventModal((prev) => ({ ...prev, loading: true, error: null }));

    try {
      await eventService.deleteEvent(deleteEventModal.item.id);
      setToast({ type: "success", message: `Event "${deleteEventModal.item.title}" deleted or cancelled successfully!` });
      setDeleteEventModal({ open: false, item: null, loading: false, error: null });
      if (selectedEvent && selectedEvent.id === deleteEventModal.item.id) {
        closeModal();
      }
      fetchEvents(page, size, eventTypeFilter, statusFilter, true);
    } catch (err) {
      setDeleteEventModal((prev) => ({
        ...prev,
        loading: false,
        error: err.response?.data?.message || "Failed to delete event.",
      }));
    }
  };

  return (
    <div className="events-page">
      {/* Page Header */}
      <div className="events-header">
        <div className="events-header-left">
          <div className="events-title-row">
            <div className="events-title-icon" aria-hidden="true">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
                <circle cx="12" cy="16" r="1" />
              </svg>
            </div>
            <h1>Events Management</h1>
          </div>
          <p className="events-subtitle">
            Browse campus schedules, technical symposiums, workshops, and register for campus activities in real-time.
          </p>
        </div>

        <div className="events-header-actions">
          {isAdminOrFaculty && (
            <button
              className="btn-add-event"
              onClick={handleOpenAddEvent}
              id="btn-add-event"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>Add Event</span>
            </button>
          )}

          <button
            className="btn-refresh"
            onClick={() => fetchEvents(page, size, eventTypeFilter, statusFilter, true)}
            disabled={loading || refreshing}
            title="Refresh event listings"
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
      <div className="events-stats-strip">
        <div className="event-stat-card">
          <div className="event-stat-icon total">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </div>
          <div className="event-stat-info">
            <span className="event-stat-label">Total Events</span>
            <span className="event-stat-value">{metrics.total}</span>
          </div>
        </div>

        <div className="event-stat-card">
          <div className="event-stat-icon upcoming">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </div>
          <div className="event-stat-info">
            <span className="event-stat-label">Upcoming</span>
            <span className="event-stat-value">{metrics.upcoming}</span>
          </div>
        </div>

        <div className="event-stat-card">
          <div className="event-stat-icon ongoing">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
          </div>
          <div className="event-stat-info">
            <span className="event-stat-label">Ongoing</span>
            <span className="event-stat-value">{metrics.ongoing}</span>
          </div>
        </div>

        <div className="event-stat-card">
          <div className="event-stat-icon completed">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <div className="event-stat-info">
            <span className="event-stat-label">Completed</span>
            <span className="event-stat-value">{metrics.completed}</span>
          </div>
        </div>
      </div>

      {/* Toast Feedback */}
      {toast && (
        <div className={`events-toast ${toast.type}`}>
          <span>{toast.message}</span>
          <button
            className="events-toast-close"
            onClick={() => setToast(null)}
            aria-label="Dismiss notification"
          >
            ×
          </button>
        </div>
      )}

      {/* Controls & Filter Bar */}
      <div className="events-controls">
        <div className="events-controls-left">
          <div className="events-search-box">
            <svg
              className="events-search-icon"
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
              className="events-search-input"
              placeholder="Search by event title, location, or keyword..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="events-filter-group">
            <select
              className="events-select"
              value={eventTypeFilter}
              onChange={(e) => {
                setEventTypeFilter(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by event category"
            >
              {EVENT_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>

            <select
              className="events-select"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by event status"
            >
              {EVENT_STATUSES.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="events-controls-right">
          {/* View Mode Toggle */}
          <div className="view-switch" role="group" aria-label="View mode">
            <button
              className={`view-btn ${viewMode === "grid" ? "active" : ""}`}
              onClick={() => setViewMode("grid")}
              title="Card grid view"
              aria-label="Card grid view"
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

      {/* Main Content Area */}
      {loading ? (
        <div className="events-skeleton-grid">
          {[1, 2, 3, 4, 5, 6].map((idx) => (
            <div key={idx} className="events-skeleton-card">
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <div className="skeleton-shimmer" style={{ width: 80, height: 22 }} />
                <div className="skeleton-shimmer" style={{ width: 70, height: 22 }} />
              </div>
              <div className="skeleton-shimmer" style={{ width: "85%", height: 20 }} />
              <div className="skeleton-shimmer" style={{ width: "100%", height: 36 }} />
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <div className="skeleton-shimmer" style={{ width: "60%", height: 14 }} />
                <div className="skeleton-shimmer" style={{ width: "50%", height: 14 }} />
              </div>
              <div className="skeleton-shimmer" style={{ width: "100%", height: 36, marginTop: "auto" }} />
            </div>
          ))}
        </div>
      ) : unauthorized ? (
        <div className="events-error-state">
          <div className="events-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0110 0v4" />
            </svg>
          </div>
          <h2 className="events-state-title">Access Restricted</h2>
          <p className="events-state-desc">{error}</p>
        </div>
      ) : error ? (
        <div className="events-error-state">
          <div className="events-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h2 className="events-state-title">Unable to Load Events</h2>
          <p className="events-state-desc">{error}</p>
          <button
            className="btn-action-primary"
            style={{ maxWidth: 160 }}
            onClick={() => fetchEvents(page, size, eventTypeFilter, statusFilter, true)}
          >
            Try Again
          </button>
        </div>
      ) : filteredEvents.length === 0 ? (
        <div className="events-empty-state">
          <div className="events-state-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
              <line x1="10" y1="14" x2="14" y2="18" />
              <line x1="14" y1="14" x2="10" y2="18" />
            </svg>
          </div>
          <h2 className="events-state-title">No Events Found</h2>
          <p className="events-state-desc">
            {searchTerm || eventTypeFilter !== "ALL" || statusFilter !== "ALL"
              ? "No events match your current filter and search criteria. Try modifying your filters."
              : "There are currently no events scheduled on the campus portal."}
          </p>
          {isAdminOrFaculty && !searchTerm && eventTypeFilter === "ALL" && statusFilter === "ALL" && (
            <button className="btn-add-event" onClick={handleOpenAddEvent}>
              + Add First Campus Event
            </button>
          )}
          {(searchTerm || eventTypeFilter !== "ALL" || statusFilter !== "ALL") && (
            <button
              className="btn-action-secondary"
              onClick={() => {
                setSearchTerm("");
                setEventTypeFilter("ALL");
                setStatusFilter("ALL");
              }}
            >
              Clear All Filters
            </button>
          )}
        </div>
      ) : viewMode === "grid" ? (
        /* Card Grid View */
        <div className="events-grid">
          {filteredEvents.map((event) => {
            const isCancelled = event.status === "CANCELLED";
            const isCompleted = event.status === "COMPLETED";
            const canRegister = !isCancelled && !isCompleted;
            const isRegistering = registeringId === event.id;

            return (
              <div key={event.id} className="event-card">
                <div className="event-card-header">
                  <span className={`event-type-badge ${getTypeBadgeClass(event.eventType)}`}>
                    {event.eventType || "EVENT"}
                  </span>
                  <span className={`event-status-badge ${getStatusBadgeClass(event.status)}`}>
                    <span className="event-status-dot" />
                    {event.status || "UPCOMING"}
                  </span>
                </div>

                <div className="event-card-body">
                  <h3 className="event-card-title">{event.title}</h3>
                  <p className="event-card-desc">
                    {event.description || "No description provided."}
                  </p>

                  <div className="event-card-meta">
                    <div className="event-meta-item">
                      <svg className="event-meta-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10" />
                        <polyline points="12 6 12 12 16 14" />
                      </svg>
                      <span>{formatDateTime(event.startDateTime)}</span>
                    </div>

                    <div className="event-meta-item">
                      <svg className="event-meta-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                        <circle cx="12" cy="10" r="3" />
                      </svg>
                      <span>{event.location || "Online / TBD"}</span>
                    </div>

                    {event.capacity != null && (
                      <div className="event-meta-item">
                        <svg className="event-meta-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                          <circle cx="9" cy="7" r="4" />
                        </svg>
                        <span>Capacity: {event.capacity} seats</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="event-card-actions">
                  <button
                    className="btn-action-secondary"
                    onClick={() => openDetailsModal(event, "details")}
                  >
                    Details
                  </button>

                  {isAdminOrFaculty && (
                    <>
                      <button
                        className="btn-action-secondary"
                        onClick={() => openDetailsModal(event, "registrations")}
                        title="Inspect event attendees"
                      >
                        Attendees
                      </button>
                      <button
                        className="btn-action-icon edit"
                        onClick={() => handleOpenEditEvent(event)}
                        title="Edit Event"
                      >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                        </svg>
                      </button>
                      <button
                        className="btn-action-icon delete"
                        onClick={() => setDeleteEventModal({ open: true, item: event, loading: false, error: null })}
                        title="Delete Event"
                      >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </>
                  )}

                  <button
                    className="btn-action-primary"
                    onClick={() => handleRegister(event.id, event.title)}
                    disabled={!canRegister || isRegistering}
                    title={
                      !canRegister
                        ? `Event is ${event.status.toLowerCase()}`
                        : "Register for this event"
                    }
                  >
                    {isRegistering ? (
                      "Registering..."
                    ) : (
                      <>
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                          <circle cx="9" cy="7" r="4" />
                          <line x1="19" y1="8" x2="19" y2="14" />
                          <line x1="22" y1="11" x2="16" y2="11" />
                        </svg>
                        <span>Register</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <div className="events-table-wrapper">
          <table className="events-table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Category</th>
                <th>Schedule</th>
                <th>Location</th>
                <th>Capacity</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredEvents.map((event) => {
                const isCancelled = event.status === "CANCELLED";
                const isCompleted = event.status === "COMPLETED";
                const canRegister = !isCancelled && !isCompleted;
                const isRegistering = registeringId === event.id;

                return (
                  <tr key={event.id}>
                    <td>
                      <div className="event-cell-title">
                        <span className="event-cell-name">{event.title}</span>
                        <span className="event-cell-desc">
                          {event.description || "No description"}
                        </span>
                      </div>
                    </td>
                    <td>
                      <span className={`event-type-badge ${getTypeBadgeClass(event.eventType)}`}>
                        {event.eventType}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: "12.5px", whiteSpace: "nowrap" }}>
                        <div>{formatDateTime(event.startDateTime)}</div>
                        <div style={{ color: "var(--color-text-muted)" }}>
                          to {formatDateTime(event.endDateTime)}
                        </div>
                      </div>
                    </td>
                    <td>{event.location || "TBD"}</td>
                    <td>{event.capacity != null ? `${event.capacity} seats` : "Unlimited"}</td>
                    <td>
                      <span className={`event-status-badge ${getStatusBadgeClass(event.status)}`}>
                        <span className="event-status-dot" />
                        {event.status}
                      </span>
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          className="btn-action-secondary"
                          style={{ padding: "5px 10px", fontSize: "12px" }}
                          onClick={() => openDetailsModal(event, "details")}
                        >
                          Details
                        </button>
                        {isAdminOrFaculty && (
                          <>
                            <button
                              className="btn-action-secondary"
                              style={{ padding: "5px 10px", fontSize: "12px" }}
                              onClick={() => openDetailsModal(event, "registrations")}
                            >
                              Attendees
                            </button>
                            <button
                              className="btn-action-icon edit"
                              style={{ width: 28, height: 28 }}
                              onClick={() => handleOpenEditEvent(event)}
                              title="Edit Event"
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                              </svg>
                            </button>
                            <button
                              className="btn-action-icon delete"
                              style={{ width: 28, height: 28 }}
                              onClick={() => setDeleteEventModal({ open: true, item: event, loading: false, error: null })}
                              title="Delete Event"
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                              </svg>
                            </button>
                          </>
                        )}
                        <button
                          className="btn-action-primary"
                          style={{ padding: "5px 10px", fontSize: "12px", minWidth: 80 }}
                          onClick={() => handleRegister(event.id, event.title)}
                          disabled={!canRegister || isRegistering}
                        >
                          {isRegistering ? "..." : "Register"}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Controls */}
      {!loading && totalPages > 1 && (
        <div className="events-pagination">
          <div className="pagination-info">
            Showing Page {page + 1} of {totalPages} (Total {totalElements} events)
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

      {/* ==============================================================
          EVENT DETAILS & ATTENDEES MODAL
          ============================================================== */}
      {selectedEvent && (
        <div className="modal-overlay" onClick={closeModal} aria-modal="true" role="dialog">
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-header-left">
                <span className={`event-type-badge ${getTypeBadgeClass(selectedEvent.eventType)}`}>
                  {selectedEvent.eventType}
                </span>
                <span className={`event-status-badge ${getStatusBadgeClass(selectedEvent.status)}`}>
                  <span className="event-status-dot" />
                  {selectedEvent.status}
                </span>
              </div>
              <button
                className="modal-close-btn"
                onClick={closeModal}
                aria-label="Close dialog"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            {isAdminOrFaculty && (
              <div className="modal-tabs">
                <button
                  className={`modal-tab-btn ${modalTab === "details" ? "active" : ""}`}
                  onClick={() => handleModalTabChange("details")}
                >
                  Event Details
                </button>
                <button
                  className={`modal-tab-btn ${modalTab === "registrations" ? "active" : ""}`}
                  onClick={() => handleModalTabChange("registrations")}
                >
                  Registrations & Attendees
                </button>
              </div>
            )}

            <div className="modal-body">
              {modalTab === "details" ? (
                <>
                  <h2 style={{ fontSize: "20px", color: "var(--color-text-heading)", margin: 0 }}>
                    {selectedEvent.title}
                  </h2>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Description:</span>
                    <span className="modal-detail-val">
                      {selectedEvent.description || "No detailed description provided."}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Schedule:</span>
                    <span className="modal-detail-val">
                      {formatDateRange(selectedEvent.startDateTime, selectedEvent.endDateTime)}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Location / Venue:</span>
                    <span className="modal-detail-val">
                      {selectedEvent.location || "Campus Venue / Online"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Capacity:</span>
                    <span className="modal-detail-val">
                      {selectedEvent.capacity ? `${selectedEvent.capacity} attendees max` : "Open"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Organizer ID:</span>
                    <span className="modal-detail-val">
                      {selectedEvent.organizerUserId ? `User #${selectedEvent.organizerUserId}` : "Staff / Faculty"}
                    </span>
                  </div>

                  <div className="modal-detail-row">
                    <span className="modal-detail-label">Created At:</span>
                    <span className="modal-detail-val">
                      {formatDateTime(selectedEvent.createdAt)}
                    </span>
                  </div>
                </>
              ) : (
                /* Registrations Tab */
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h3 style={{ fontSize: "16px", color: "var(--color-text-heading)", margin: 0 }}>
                      Registered Attendees ({registrations.length})
                    </h3>
                    <button
                      className="btn-refresh"
                      onClick={() => fetchRegistrations(selectedEvent.id)}
                      disabled={loadingRegistrations}
                    >
                      <span>{loadingRegistrations ? "Loading..." : "Refresh"}</span>
                    </button>
                  </div>

                  {loadingRegistrations ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      Loading registered attendees...
                    </div>
                  ) : registrationsError ? (
                    <div style={{ color: "var(--color-danger)", padding: "12px 0" }}>
                      {registrationsError}
                    </div>
                  ) : registrations.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "30px 0", color: "var(--color-text-muted)" }}>
                      No users have registered for this event yet.
                    </div>
                  ) : (
                    <div className="events-table-wrapper">
                      <table className="registrations-table">
                        <thead>
                          <tr>
                            <th>Registration ID</th>
                            <th>User ID</th>
                            <th>Registered Time</th>
                            <th>Status</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {registrations.map((reg) => (
                            <tr key={reg.id}>
                              <td>#{reg.id}</td>
                              <td>User #{reg.userId}</td>
                              <td>{formatDateTime(reg.registeredAt)}</td>
                              <td>
                                <span
                                  className={`event-status-badge ${
                                    reg.status === "REGISTERED"
                                      ? "badge-status-ongoing"
                                      : "badge-status-cancelled"
                                  }`}
                                  style={{ padding: "2px 8px", fontSize: "11px" }}
                                >
                                  {reg.status}
                                </span>
                              </td>
                              <td>
                                {reg.status === "REGISTERED" && (
                                  <button
                                    className="btn-action-secondary"
                                    style={{
                                      padding: "4px 8px",
                                      fontSize: "11px",
                                      color: "var(--color-danger)",
                                      borderColor: "var(--color-danger-border)",
                                    }}
                                    onClick={() => handleCancelRegistration(selectedEvent.id, reg.id)}
                                    disabled={cancellingRegId === reg.id}
                                  >
                                    {cancellingRegId === reg.id ? "Cancelling..." : "Cancel"}
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
              {isAdminOrFaculty && (
                <>
                  <button
                    className="btn-action-secondary"
                    onClick={() => {
                      const ev = selectedEvent;
                      closeModal();
                      handleOpenEditEvent(ev);
                    }}
                  >
                    Edit Event
                  </button>
                  <button
                    className="btn-action-secondary"
                    style={{ color: "var(--color-danger)", borderColor: "var(--color-danger-border)" }}
                    onClick={() => {
                      const ev = selectedEvent;
                      closeModal();
                      setDeleteEventModal({ open: true, item: ev, loading: false, error: null });
                    }}
                  >
                    Delete Event
                  </button>
                </>
              )}

              <button className="btn-action-secondary" onClick={closeModal}>
                Close
              </button>

              {modalTab === "details" &&
                selectedEvent.status !== "CANCELLED" &&
                selectedEvent.status !== "COMPLETED" && (
                  <button
                    className="btn-action-primary"
                    onClick={() => handleRegister(selectedEvent.id, selectedEvent.title)}
                    disabled={registeringId === selectedEvent.id}
                  >
                    {registeringId === selectedEvent.id ? "Registering..." : "Register for Event"}
                  </button>
                )}
            </div>
          </div>
        </div>
      )}

      {/* ==============================================================
          ADD / EDIT EVENT MODAL
          ============================================================== */}
      {eventFormModal.open && (
        <div className="modal-overlay" onClick={() => setEventFormModal({ open: false, mode: "create", item: null })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-header-left">
                <h2 style={{ fontSize: "18px", color: "var(--color-text-heading)", margin: 0 }}>
                  {eventFormModal.mode === "create" ? "Add Campus Event" : `Edit Event: ${eventFormModal.item?.title}`}
                </h2>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setEventFormModal({ open: false, mode: "create", item: null })}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitEventForm}>
              <div className="modal-body">
                {eventModalError && (
                  <div className="modal-alert-error" role="alert">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span>{eventModalError}</span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label" htmlFor="ev-title">
                    Event Title <span className="required-star">*</span>
                  </label>
                  <input
                    id="ev-title"
                    type="text"
                    className={`form-input ${eventFormErrors.title ? "input-error" : ""}`}
                    placeholder="e.g. Cloud-Native Microservices Hackathon"
                    value={eventFormData.title}
                    onChange={(e) => setEventFormData({ ...eventFormData, title: e.target.value })}
                    disabled={eventSubmitting}
                  />
                  {eventFormErrors.title && <span className="form-error-msg">{eventFormErrors.title}</span>}
                </div>

                <div className="form-grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="ev-type">
                      Event Category <span className="required-star">*</span>
                    </label>
                    <select
                      id="ev-type"
                      className="form-select"
                      value={eventFormData.eventType}
                      onChange={(e) => setEventFormData({ ...eventFormData, eventType: e.target.value })}
                      disabled={eventSubmitting}
                    >
                      <option value="TECHNICAL">Technical</option>
                      <option value="WORKSHOP">Workshop</option>
                      <option value="SEMINAR">Seminar</option>
                      <option value="ACADEMIC">Academic</option>
                      <option value="CULTURAL">Cultural</option>
                      <option value="SPORTS">Sports</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>

                  {eventFormModal.mode === "edit" ? (
                    <div className="form-group">
                      <label className="form-label" htmlFor="ev-status">
                        Status <span className="required-star">*</span>
                      </label>
                      <select
                        id="ev-status"
                        className="form-select"
                        value={eventFormData.status}
                        onChange={(e) => setEventFormData({ ...eventFormData, status: e.target.value })}
                        disabled={eventSubmitting}
                      >
                        <option value="UPCOMING">Upcoming</option>
                        <option value="ONGOING">Ongoing</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="CANCELLED">Cancelled</option>
                      </select>
                    </div>
                  ) : (
                    <div className="form-group">
                      <label className="form-label" htmlFor="ev-capacity">
                        Attendee Capacity (Seats)
                      </label>
                      <input
                        id="ev-capacity"
                        type="number"
                        min="1"
                        className="form-input"
                        placeholder="e.g. 100"
                        value={eventFormData.capacity}
                        onChange={(e) => setEventFormData({ ...eventFormData, capacity: e.target.value })}
                        disabled={eventSubmitting}
                      />
                    </div>
                  )}
                </div>

                <div className="form-grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="ev-start">
                      Start Date & Time <span className="required-star">*</span>
                    </label>
                    <input
                      id="ev-start"
                      type="datetime-local"
                      className={`form-input ${eventFormErrors.startDateTime ? "input-error" : ""}`}
                      value={eventFormData.startDateTime}
                      onChange={(e) => setEventFormData({ ...eventFormData, startDateTime: e.target.value })}
                      disabled={eventSubmitting}
                    />
                    {eventFormErrors.startDateTime && <span className="form-error-msg">{eventFormErrors.startDateTime}</span>}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="ev-end">
                      End Date & Time <span className="required-star">*</span>
                    </label>
                    <input
                      id="ev-end"
                      type="datetime-local"
                      className={`form-input ${eventFormErrors.endDateTime ? "input-error" : ""}`}
                      value={eventFormData.endDateTime}
                      onChange={(e) => setEventFormData({ ...eventFormData, endDateTime: e.target.value })}
                      disabled={eventSubmitting}
                    />
                    {eventFormErrors.endDateTime && <span className="form-error-msg">{eventFormErrors.endDateTime}</span>}
                  </div>
                </div>

                <div className="form-grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="ev-location">
                      Location / Venue
                    </label>
                    <input
                      id="ev-location"
                      type="text"
                      className="form-input"
                      placeholder="e.g. Auditorium Hall A"
                      value={eventFormData.location}
                      onChange={(e) => setEventFormData({ ...eventFormData, location: e.target.value })}
                      disabled={eventSubmitting}
                    />
                  </div>

                  {eventFormModal.mode === "edit" && (
                    <div className="form-group">
                      <label className="form-label" htmlFor="ev-capacity-edit">
                        Attendee Capacity (Seats)
                      </label>
                      <input
                        id="ev-capacity-edit"
                        type="number"
                        min="1"
                        className="form-input"
                        placeholder="e.g. 100"
                        value={eventFormData.capacity}
                        onChange={(e) => setEventFormData({ ...eventFormData, capacity: e.target.value })}
                        disabled={eventSubmitting}
                      />
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="ev-desc">
                    Description
                  </label>
                  <textarea
                    id="ev-desc"
                    className="form-textarea"
                    placeholder="Event overview, agenda, prerequisites, etc."
                    value={eventFormData.description}
                    onChange={(e) => setEventFormData({ ...eventFormData, description: e.target.value })}
                    disabled={eventSubmitting}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn-modal-cancel"
                  onClick={() => setEventFormModal({ open: false, mode: "create", item: null })}
                  disabled={eventSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-modal-submit"
                  disabled={eventSubmitting}
                >
                  {eventSubmitting ? "Saving..." : eventFormModal.mode === "create" ? "Create Event" : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==============================================================
          DELETE EVENT CONFIRMATION DIALOG
          ============================================================== */}
      {deleteEventModal.open && (
        <div className="modal-overlay" onClick={() => !deleteEventModal.loading && setDeleteEventModal({ open: false, item: null, loading: false, error: null })}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3 style={{ fontSize: "17px", color: "var(--color-text-heading)", margin: 0 }}>Confirm Cancellation</h3>
              </div>
              <button
                className="modal-close-btn"
                disabled={deleteEventModal.loading}
                onClick={() => setDeleteEventModal({ open: false, item: null, loading: false, error: null })}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {deleteEventModal.error && (
                <div className="modal-alert-error" role="alert">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{deleteEventModal.error}</span>
                </div>
              )}
              <p style={{ color: "var(--color-text-heading)", margin: 0, lineHeight: 1.5 }}>
                Are you sure you want to delete or cancel <strong>{deleteEventModal.item?.title}</strong>?
              </p>
              <p style={{ color: "var(--color-text-muted)", fontSize: 13, margin: 0 }}>
                This event will be marked as cancelled or removed from active discovery.
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                disabled={deleteEventModal.loading}
                onClick={() => setDeleteEventModal({ open: false, item: null, loading: false, error: null })}
              >
                Keep Event
              </button>
              <button
                className="btn-modal-delete"
                disabled={deleteEventModal.loading}
                onClick={handleDeleteEvent}
              >
                {deleteEventModal.loading ? "Deleting..." : "Delete Event"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EventsPage;
