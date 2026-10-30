import axios from "axios";

// In development mode without Docker, default to http://localhost:8080.
// In containerized production (served via Nginx), default to "" so requests
// pass through the Nginx reverse proxy (/api/ -> http://api-gateway:8080).
const baseURL =
  import.meta.env.VITE_API_BASE_URL !== undefined
    ? import.meta.env.VITE_API_BASE_URL
    : import.meta.env.DEV
    ? "http://localhost:8080"
    : "";

const api = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
