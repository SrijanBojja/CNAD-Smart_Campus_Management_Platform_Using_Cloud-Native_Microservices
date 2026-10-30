import api from "../api/axios";

/**
 * Attendance Service — API client for attendance management endpoints.
 * Calls /api/v1/attendance/sessions and /api/v1/attendance/records
 * through the API Gateway at http://localhost:8080.
 */
const attendanceService = {
  // ==========================================
  // SESSIONS
  // ==========================================
  getSessions: async ({
    page = 0,
    size = 100,
    subjectId,
    facultyUserId,
    sessionDate,
    academicYear,
    semester,
    section,
  } = {}) => {
    const params = { page, size };
    if (subjectId) params.subjectId = subjectId;
    if (facultyUserId) params.facultyUserId = facultyUserId;
    if (sessionDate) params.sessionDate = sessionDate;
    if (academicYear) params.academicYear = academicYear;
    if (semester) params.semester = semester;
    if (section) params.section = section;

    const response = await api.get("/api/v1/attendance/sessions", { params });
    return response.data;
  },

  getSessionById: async (id) => {
    const response = await api.get(`/api/v1/attendance/sessions/${id}`);
    return response.data;
  },

  createSession: async (data) => {
    const response = await api.post("/api/v1/attendance/sessions", data);
    return response.data;
  },

  updateSession: async (id, data) => {
    const response = await api.put(`/api/v1/attendance/sessions/${id}`, data);
    return response.data;
  },

  deleteSession: async (id) => {
    const response = await api.delete(`/api/v1/attendance/sessions/${id}`);
    return response.data;
  },

  // ==========================================
  // RECORDS
  // ==========================================
  getRecords: async ({
    page = 0,
    size = 100,
    sessionId,
    studentId,
    status,
  } = {}) => {
    const params = { page, size };
    if (sessionId) params.sessionId = sessionId;
    if (studentId) params.studentId = studentId;
    if (status && status !== "ALL") params.status = status;

    const response = await api.get("/api/v1/attendance/records", { params });
    return response.data;
  },

  getRecordById: async (id) => {
    const response = await api.get(`/api/v1/attendance/records/${id}`);
    return response.data;
  },

  createRecord: async (data) => {
    const response = await api.post("/api/v1/attendance/records", data);
    return response.data;
  },

  updateRecord: async (id, data) => {
    const response = await api.put(`/api/v1/attendance/records/${id}`, data);
    return response.data;
  },

  deleteRecord: async (id) => {
    const response = await api.delete(`/api/v1/attendance/records/${id}`);
    return response.data;
  },
};

export default attendanceService;
