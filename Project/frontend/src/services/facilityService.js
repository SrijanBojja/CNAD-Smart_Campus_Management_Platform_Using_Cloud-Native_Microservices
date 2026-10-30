import api from "../api/axios";

/**
 * Facility Service — API client for campus facilities, reservations, and maintenance.
 * Calls /api/v1/facilities/** through the API Gateway at http://localhost:8080.
 */
const facilityService = {
  // ==========================================
  // Facilities Catalog
  // ==========================================

  /**
   * Retrieve a paginated list of facilities with optional filters.
   * Accessible by ADMIN, FACULTY, and STUDENT.
   *
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-indexed page number
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.facilityType] - CLASSROOM | LAB | AUDITORIUM | SEMINAR_HALL | SPORTS | LIBRARY | OTHER
   * @param {string} [params.building] - Building name filter
   * @param {string} [params.status] - AVAILABLE | UNAVAILABLE | MAINTENANCE
   * @returns {Promise<Object>} Spring Data Page<FacilityResponse>
   */
  getFacilities: async ({
    page = 0,
    size = 20,
    facilityType,
    building,
    status,
  } = {}) => {
    const params = { page, size };
    if (facilityType && facilityType !== "ALL") params.facilityType = facilityType;
    if (building && building.trim()) params.building = building.trim();
    if (status && status !== "ALL") params.status = status;

    const response = await api.get("/api/v1/facilities", { params });
    return response.data;
  },

  /**
   * Retrieve details for a specific facility by ID.
   * Accessible by ADMIN, FACULTY, and STUDENT.
   *
   * @param {number|string} id - Facility ID
   * @returns {Promise<Object>} FacilityResponse
   */
  getFacilityById: async (id) => {
    const response = await api.get(`/api/v1/facilities/${id}`);
    return response.data;
  },

  /**
   * Create a new campus facility.
   * Restricted to ADMIN.
   *
   * @param {Object} facilityData - { name, facilityType, description, building, roomNumber, capacity, status }
   * @returns {Promise<Object>} FacilityResponse
   */
  createFacility: async (facilityData) => {
    const response = await api.post("/api/v1/facilities", facilityData);
    return response.data;
  },

  /**
   * Update details of an existing facility.
   * Restricted to ADMIN.
   *
   * @param {number|string} id - Facility ID
   * @param {Object} facilityData - { name, facilityType, description, building, roomNumber, capacity, status }
   * @returns {Promise<Object>} FacilityResponse
   */
  updateFacility: async (id, facilityData) => {
    const response = await api.put(`/api/v1/facilities/${id}`, facilityData);
    return response.data;
  },

  /**
   * Update the availability or maintenance status of a facility.
   * Restricted to ADMIN.
   *
   * @param {number|string} id - Facility ID
   * @param {string} status - AVAILABLE | UNAVAILABLE | MAINTENANCE
   * @returns {Promise<Object>} FacilityResponse
   */
  updateFacilityStatus: async (id, status) => {
    const response = await api.patch(`/api/v1/facilities/${id}/status`, { status });
    return response.data;
  },

  /**
   * Delete or retire a facility.
   * Restricted to ADMIN.
   *
   * @param {number|string} id - Facility ID
   * @returns {Promise<void>}
   */
  deleteFacility: async (id) => {
    await api.delete(`/api/v1/facilities/${id}`);
  },

  // ==========================================
  // Facility Booking Requests
  // ==========================================

  /**
   * Submit a booking request for a facility.
   * Accessible by ADMIN, FACULTY, and STUDENT. Requester ID is derived from JWT.
   *
   * @param {number|string} facilityId - Facility ID
   * @param {Object} requestData - { requestDate (YYYY-MM-DD), startTime (HH:mm:ss), endTime (HH:mm:ss), purpose }
   * @returns {Promise<Object>} FacilityRequestResponse
   */
  createBookingRequest: async (facilityId, requestData) => {
    const response = await api.post(
      `/api/v1/facilities/${facilityId}/requests`,
      requestData
    );
    return response.data;
  },

  /**
   * List all booking requests for a facility.
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId - Facility ID
   * @param {Object} [params]
   * @param {number} [params.page=0]
   * @param {number} [params.size=20]
   * @returns {Promise<Object>} Spring Data Page<FacilityRequestResponse>
   */
  getFacilityRequests: async (facilityId, { page = 0, size = 20 } = {}) => {
    const params = { page, size };
    const response = await api.get(
      `/api/v1/facilities/${facilityId}/requests`,
      { params }
    );
    return response.data;
  },

  /**
   * Get single facility booking request by ID.
   * ADMIN, FACULTY, or the owner STUDENT.
   *
   * @param {number|string} facilityId
   * @param {number|string} requestId
   * @returns {Promise<Object>} FacilityRequestResponse
   */
  getRequestById: async (facilityId, requestId) => {
    const response = await api.get(
      `/api/v1/facilities/${facilityId}/requests/${requestId}`
    );
    return response.data;
  },

  /**
   * Update request status (Approve, Reject, or Cancel).
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId
   * @param {number|string} requestId
   * @param {Object} statusData - { status: 'APPROVED'|'REJECTED'|'CANCELLED'|'COMPLETED', remarks }
   * @returns {Promise<Object>} FacilityRequestResponse
   */
  updateRequestStatus: async (facilityId, requestId, { status, remarks } = {}) => {
    const response = await api.patch(
      `/api/v1/facilities/${facilityId}/requests/${requestId}/status`,
      { status, remarks }
    );
    return response.data;
  },

  /**
   * Cancel or delete a booking request.
   * Accessible by ADMIN, FACULTY, or the owner STUDENT.
   *
   * @param {number|string} facilityId
   * @param {number|string} requestId
   * @returns {Promise<void>}
   */
  cancelBookingRequest: async (facilityId, requestId) => {
    await api.delete(
      `/api/v1/facilities/${facilityId}/requests/${requestId}`
    );
  },

  // ==========================================
  // Facility Maintenance
  // ==========================================

  /**
   * Report a maintenance issue for a facility.
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId
   * @param {Object} recordData - { issueDescription, priority, assignedToUserId }
   * @returns {Promise<Object>} MaintenanceRecordResponse
   */
  createMaintenanceRecord: async (facilityId, recordData) => {
    const response = await api.post(
      `/api/v1/facilities/${facilityId}/maintenance`,
      recordData
    );
    return response.data;
  },

  /**
   * Retrieve paginated maintenance records for a facility.
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId
   * @param {Object} [params]
   * @param {number} [params.page=0]
   * @param {number} [params.size=20]
   * @returns {Promise<Object>} Spring Data Page<MaintenanceRecordResponse>
   */
  getMaintenanceRecords: async (facilityId, { page = 0, size = 20 } = {}) => {
    const params = { page, size };
    const response = await api.get(
      `/api/v1/facilities/${facilityId}/maintenance`,
      { params }
    );
    return response.data;
  },

  /**
   * Advance or update maintenance status (OPEN, IN_PROGRESS, RESOLVED, CANCELLED).
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId
   * @param {number|string} maintenanceId
   * @param {Object} statusData - { status, assignedToUserId, resolutionNotes }
   * @returns {Promise<Object>} MaintenanceRecordResponse
   */
  updateMaintenanceStatus: async (
    facilityId,
    maintenanceId,
    { status, assignedToUserId, resolutionNotes } = {}
  ) => {
    const response = await api.patch(
      `/api/v1/facilities/${facilityId}/maintenance/${maintenanceId}/status`,
      { status, assignedToUserId, resolutionNotes }
    );
    return response.data;
  },

  /**
   * Cancel or delete a maintenance work order.
   * Restricted to ADMIN and FACULTY.
   *
   * @param {number|string} facilityId
   * @param {number|string} maintenanceId
   * @returns {Promise<void>}
   */
  cancelMaintenanceRecord: async (facilityId, maintenanceId) => {
    await api.delete(
      `/api/v1/facilities/${facilityId}/maintenance/${maintenanceId}`
    );
  },
};

export default facilityService;
