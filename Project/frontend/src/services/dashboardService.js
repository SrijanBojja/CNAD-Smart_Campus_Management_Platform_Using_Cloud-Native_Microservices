import api from "../api/axios";

/**
 * Dashboard data service.
 *
 * Each fetcher hits a real backend endpoint routed through the API Gateway.
 * Paginated endpoints are called with size=1 so we only need totalElements.
 */
const dashboardService = {
  /**
   * GET /api/v1/students/count
   * Accessible by all authenticated roles (ADMIN, FACULTY, STUDENT).
   */
  getStudentCount: async () => {
    const response = await api.get("/api/v1/students/count");
    return response.data?.count ?? response.data?.totalElements ?? response.data;
  },

  /**
   * GET /api/v1/courses?size=1
   * Accessible by all authenticated roles.
   */
  getCourseCount: async () => {
    const response = await api.get("/api/v1/courses", {
      params: { page: 0, size: 1 },
    });
    return response.data.totalElements;
  },

  /**
   * GET /api/v1/subjects?size=1
   * Accessible by all authenticated roles.
   */
  getSubjectCount: async () => {
    const response = await api.get("/api/v1/subjects", {
      params: { page: 0, size: 1 },
    });
    return response.data.totalElements;
  },

  /**
   * GET /api/v1/schedules?size=1
   * Accessible by all authenticated roles.
   */
  getScheduleCount: async () => {
    const response = await api.get("/api/v1/schedules", {
      params: { page: 0, size: 1 },
    });
    return response.data.totalElements;
  },
};

export default dashboardService;
