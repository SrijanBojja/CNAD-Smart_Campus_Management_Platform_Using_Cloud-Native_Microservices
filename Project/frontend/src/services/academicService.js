import api from "../api/axios";

/**
 * Academic Service — API client for academic management endpoints.
 * Calls /api/v1/courses, /api/v1/subjects, and /api/v1/schedules
 * through the API Gateway at http://localhost:8080.
 */
const academicService = {
  // ==========================================
  // COURSES
  // ==========================================
  getCourses: async ({ page = 0, size = 100, department, status } = {}) => {
    const params = { page, size };
    if (department) params.department = department;
    if (status) params.status = status;

    const response = await api.get("/api/v1/courses", { params });
    return response.data;
  },

  getCourseById: async (id) => {
    const response = await api.get(`/api/v1/courses/${id}`);
    return response.data;
  },

  createCourse: async (data) => {
    const response = await api.post("/api/v1/courses", data);
    return response.data;
  },

  updateCourse: async (id, data) => {
    const response = await api.put(`/api/v1/courses/${id}`, data);
    return response.data;
  },

  updateCourseStatus: async (id, status) => {
    const response = await api.patch(`/api/v1/courses/${id}/status`, { status });
    return response.data;
  },

  deleteCourse: async (id) => {
    const response = await api.delete(`/api/v1/courses/${id}`);
    return response.data;
  },

  // ==========================================
  // SUBJECTS
  // ==========================================
  getSubjects: async ({ page = 0, size = 100, courseId, semester, facultyUserId } = {}) => {
    const params = { page, size };
    if (courseId) params.courseId = courseId;
    if (semester) params.semester = semester;
    if (facultyUserId) params.facultyUserId = facultyUserId;

    const response = await api.get("/api/v1/subjects", { params });
    return response.data;
  },

  getSubjectById: async (id) => {
    const response = await api.get(`/api/v1/subjects/${id}`);
    return response.data;
  },

  createSubject: async (data) => {
    const response = await api.post("/api/v1/subjects", data);
    return response.data;
  },

  updateSubject: async (id, data) => {
    const response = await api.put(`/api/v1/subjects/${id}`, data);
    return response.data;
  },

  deleteSubject: async (id) => {
    const response = await api.delete(`/api/v1/subjects/${id}`);
    return response.data;
  },

  // ==========================================
  // SCHEDULES
  // ==========================================
  getSchedules: async ({
    page = 0,
    size = 100,
    subjectId,
    facultyUserId,
    dayOfWeek,
    academicYear,
    semester,
  } = {}) => {
    const params = { page, size };
    if (subjectId) params.subjectId = subjectId;
    if (facultyUserId) params.facultyUserId = facultyUserId;
    if (dayOfWeek) params.dayOfWeek = dayOfWeek;
    if (academicYear) params.academicYear = academicYear;
    if (semester) params.semester = semester;

    const response = await api.get("/api/v1/schedules", { params });
    return response.data;
  },

  getScheduleById: async (id) => {
    const response = await api.get(`/api/v1/schedules/${id}`);
    return response.data;
  },

  createSchedule: async (data) => {
    const response = await api.post("/api/v1/schedules", data);
    return response.data;
  },

  updateSchedule: async (id, data) => {
    const response = await api.put(`/api/v1/schedules/${id}`, data);
    return response.data;
  },

  deleteSchedule: async (id) => {
    const response = await api.delete(`/api/v1/schedules/${id}`);
    return response.data;
  },
};

export default academicService;
