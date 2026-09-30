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

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
  },
};

export default authService;