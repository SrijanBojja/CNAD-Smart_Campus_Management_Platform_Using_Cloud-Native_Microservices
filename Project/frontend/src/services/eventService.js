import api from "../api/axios";

/**
 * Event Service — API client for campus events management endpoints.
 * Calls /api/v1/events and /api/v1/events/{eventId}/registrations
 * through the API Gateway at http://localhost:8080.
 */
const eventService = {
  /**
   * Retrieve a paginated list of campus events with optional filters.
   * Accessible by ADMIN, FACULTY, and STUDENT.
   *
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-indexed page number
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.eventType] - ACADEMIC | WORKSHOP | SEMINAR | CULTURAL | SPORTS | TECHNICAL | OTHER
   * @param {string} [params.status] - UPCOMING | ONGOING | COMPLETED | CANCELLED
   * @param {number} [params.organizerUserId] - Optional organizer user ID
   * @param {string} [params.fromDate] - ISO LocalDateTime (e.g. 2026-10-01T00:00:00)
   * @param {string} [params.toDate] - ISO LocalDateTime (e.g. 2026-10-31T23:59:59)
   * @returns {Promise<Object>} Spring Data Page<EventResponse>
   */
  getEvents: async ({
    page = 0,
    size = 20,
    eventType,
    status,
    organizerUserId,
    fromDate,
    toDate,
  } = {}) => {
    const params = { page, size };
    if (eventType && eventType !== "ALL") params.eventType = eventType;
    if (status && status !== "ALL") params.status = status;
    if (organizerUserId) params.organizerUserId = organizerUserId;
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;

    const response = await api.get("/api/v1/events", { params });
    return response.data;
  },

  /**
   * Retrieve a single campus event by its ID.
   * Accessible by ADMIN, FACULTY, and STUDENT.
   *
   * @param {number|string} id - Event ID
   * @returns {Promise<Object>} EventResponse
   */
  getEventById: async (id) => {
    const response = await api.get(`/api/v1/events/${id}`);
    return response.data;
  },

  /**
   * Create a new campus event. Restricted to ADMIN and FACULTY.
   *
   * @param {Object} eventData
   * @returns {Promise<Object>} EventResponse
   */
  createEvent: async (eventData) => {
    const response = await api.post("/api/v1/events", eventData);
    return response.data;
  },

  /**
   * Update an existing event. Restricted to ADMIN and organizer FACULTY.
   *
   * @param {number|string} id
   * @param {Object} eventData
   * @returns {Promise<Object>} EventResponse
   */
  updateEvent: async (id, eventData) => {
    const response = await api.put(`/api/v1/events/${id}`, eventData);
    return response.data;
  },

  /**
   * Delete or cancel an event. Restricted to ADMIN and organizer FACULTY.
   *
   * @param {number|string} id
   * @returns {Promise<void>}
   */
  deleteEvent: async (id) => {
    const response = await api.delete(`/api/v1/events/${id}`);
    return response.data;
  },

  /**
   * Register the currently authenticated user for an event.
   * Accessible by ADMIN, FACULTY, and STUDENT.
   *
   * @param {number|string} eventId - Event ID to register for
   * @returns {Promise<Object>} EventRegistrationResponse
   */
  registerForEvent: async (eventId) => {
    const response = await api.post(`/api/v1/events/${eventId}/registrations`);
    return response.data;
  },

  /**
   * Retrieve paginated registrations for a specific event.
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} eventId - Event ID
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-indexed page number
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.status] - Optional status filter (REGISTERED | CANCELLED)
   * @returns {Promise<Object>} Spring Data Page<EventRegistrationResponse>
   */
  getEventRegistrations: async (
    eventId,
    { page = 0, size = 20, status } = {}
  ) => {
    const params = { page, size };
    if (status && status !== "ALL") params.status = status;

    const response = await api.get(
      `/api/v1/events/${eventId}/registrations`,
      { params }
    );
    return response.data;
  },

  /**
   * Retrieve specific registration details.
   * Accessible by ADMIN, FACULTY, or the registered user.
   *
   * @param {number|string} eventId - Event ID
   * @param {number|string} registrationId - Registration ID
   * @returns {Promise<Object>} EventRegistrationResponse
   */
  getRegistrationById: async (eventId, registrationId) => {
    const response = await api.get(
      `/api/v1/events/${eventId}/registrations/${registrationId}`
    );
    return response.data;
  },

  /**
   * Cancel an event registration.
   * Accessible by ADMIN, organizer FACULTY, or the registered user.
   *
   * @param {number|string} eventId - Event ID
   * @param {number|string} registrationId - Registration ID
   * @returns {Promise<Object>} EventRegistrationResponse
   */
  cancelRegistration: async (eventId, registrationId) => {
    const response = await api.delete(
      `/api/v1/events/${eventId}/registrations/${registrationId}`
    );
    return response.data;
  },
};

export default eventService;
