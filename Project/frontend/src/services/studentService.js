import api from "../api/axios";

/**
 * Student Service — API client for student management endpoints.
 * Calls /api/v1/students through the API Gateway at http://localhost:8080.
 * Restricted to ADMIN and FACULTY roles.
 */
const studentService = {
  /**
   * Retrieve a paginated list of students.
   *
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-indexed page number
   * @param {number} [params.size=20] - Number of items per page
   * @param {string} [params.sort="createdAt,desc"] - Sort field and direction
   * @returns {Promise<Object>} Spring Data Page<StudentResponse> object
   */
  getStudents: async ({ page = 0, size = 20, sort = "createdAt,desc" } = {}) => {
    const response = await api.get("/api/v1/students", {
      params: { page, size, sort },
    });
    return response.data;
  },

  /**
   * Retrieve a single student by primary key ID.
   *
   * @param {number|string} id
   * @returns {Promise<Object>} StudentResponse
   */
  getStudentById: async (id) => {
    const response = await api.get(`/api/v1/students/${id}`);
    return response.data;
  },

  /**
   * Create a new student profile.
   * Restricted to ADMIN role.
   *
   * @param {Object} studentData
   * @param {number} studentData.userId - Referenced Auth user ID
   * @param {string} studentData.studentNumber - Unique student registration number
   * @param {string} studentData.firstName - First name
   * @param {string} studentData.lastName - Last name
   * @param {string} [studentData.dateOfBirth] - Date of birth (YYYY-MM-DD)
   * @param {string} [studentData.phone] - Contact phone number
   * @param {string} [studentData.department] - Academic department
   * @param {string} [studentData.program] - Academic degree / program
   * @param {number} [studentData.yearOfStudy] - Year of study (1-10)
   * @param {string} [studentData.section] - Assigned class section
   * @returns {Promise<Object>} StudentResponse
   */
  createStudent: async (studentData) => {
    const response = await api.post("/api/v1/students", studentData);
    return response.data;
  },

  /**
   * Retrieve total student count.
   * Accessible by all authenticated roles (ADMIN, FACULTY, STUDENT).
   *
   * @returns {Promise<Object>} StudentCountResponse { count, totalElements }
   */
  getStudentCount: async () => {
    const response = await api.get("/api/v1/students/count");
    return response.data;
  },
};

export default studentService;
