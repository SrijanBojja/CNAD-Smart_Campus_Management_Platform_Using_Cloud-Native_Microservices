import api from "../api/axios";

const authService = {
  login: async (username, password) => {
    const response = await api.post("/api/v1/auth/login", {
      username,
      password,
    });

    return response.data;
  },

  getCurrentUser: async () => {
    const response = await api.get("/api/v1/auth/me");

    return response.data;
  },

  /**
   * Register a new student authentication account (ADMIN only).
   * Calls POST /api/v1/auth/students via the API Gateway.
   *
   * @param {Object} registrationData
   * @param {string} registrationData.username
   * @param {string} registrationData.email
   * @param {string} registrationData.password
   * @param {string} [registrationData.firstName]
   * @param {string} [registrationData.lastName]
   * @returns {Promise<Object>} UserDto { id, username, email, firstName, lastName, roles }
   */
  registerStudentAccount: async (registrationData) => {
    const response = await api.post("/api/v1/auth/students", registrationData);
    return response.data;
  },

  /**
   * Alias for backward compatibility.
   */
  registerStudent: async (registrationData) => {
    const response = await api.post("/api/v1/auth/students", registrationData);
    return response.data;
  },

  /**
   * Register a new faculty authentication account (ADMIN only).
   * Calls POST /api/v1/auth/faculty via the API Gateway.
   *
   * @param {Object} registrationData
   * @param {string} registrationData.username
   * @param {string} registrationData.email
   * @param {string} registrationData.password
   * @param {string} registrationData.firstName
   * @param {string} registrationData.lastName
   * @param {string} [registrationData.employeeId]
   * @param {string} [registrationData.department]
   * @param {string} [registrationData.phoneNumber]
   * @returns {Promise<Object>} UserDto
   */
  registerFacultyAccount: async (registrationData) => {
    const response = await api.post("/api/v1/auth/faculty", registrationData);
    return response.data;
  },

  registerFaculty: async (registrationData) => {
    const response = await api.post("/api/v1/auth/faculty", registrationData);
    return response.data;
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
  },

  /**
   * Get active faculty users for schedule/subject/session assignments.
   */
  getFacultyUsers: async () => {
    const response = await api.get("/api/v1/auth/faculty");
    return response.data;
  },
};

export default authService;
