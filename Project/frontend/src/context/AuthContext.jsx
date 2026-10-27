import { createContext, useContext, useEffect, useState } from "react";
import authService from "../services/authService";

const AuthContext = createContext(null);

/**
 * Normalizes a single role string by stripping "ROLE_" prefix and converting to uppercase.
 */
export const normalizeRoleString = (role) => {
  if (typeof role !== "string") return null;
  const trimmed = role.trim();
  if (!trimmed) return null;
  const upper = trimmed.toUpperCase();
  return upper.startsWith("ROLE_") ? upper.substring(5) : upper;
};

/**
 * Extracts the primary role from raw user data.
 * Priority order: ADMIN > FACULTY > STUDENT.
 * Returns null if no recognized role is found.
 */
export const extractRole = (rawUser) => {
  if (!rawUser || typeof rawUser !== "object") return null;

  const candidateRoles = [];

  // 1. Direct `role` property (string or object)
  if (typeof rawUser.role === "string") {
    const norm = normalizeRoleString(rawUser.role);
    if (norm) candidateRoles.push(norm);
  } else if (rawUser.role && typeof rawUser.role === "object") {
    const norm = normalizeRoleString(rawUser.role.name || rawUser.role.authority);
    if (norm) candidateRoles.push(norm);
  }

  // 2. `roles` array (strings or objects)
  if (Array.isArray(rawUser.roles)) {
    for (const r of rawUser.roles) {
      if (typeof r === "string") {
        const norm = normalizeRoleString(r);
        if (norm) candidateRoles.push(norm);
      } else if (r && typeof r === "object") {
        const norm = normalizeRoleString(r.name || r.authority);
        if (norm) candidateRoles.push(norm);
      }
    }
  }

  // 3. `authorities` array (common in Spring Security principals)
  if (Array.isArray(rawUser.authorities)) {
    for (const a of rawUser.authorities) {
      const norm = normalizeRoleString(typeof a === "string" ? a : a?.authority);
      if (norm) candidateRoles.push(norm);
    }
  }

  if (candidateRoles.length === 0) {
    return null;
  }

  // Privilege hierarchy: ADMIN > FACULTY > STUDENT
  if (candidateRoles.includes("ADMIN")) return "ADMIN";
  if (candidateRoles.includes("FACULTY")) return "FACULTY";
  if (candidateRoles.includes("STUDENT")) return "STUDENT";

  return candidateRoles[0];
};

/**
 * Normalizes user object from login response (data.user) or /api/v1/auth/me (currentUser).
 * Guarantees a consistent structure with both `role` (singular string) and `roles` (array).
 */
export const normalizeUser = (rawUser) => {
  if (!rawUser || typeof rawUser !== "object") return null;

  const primaryRole = extractRole(rawUser);

  let rolesList = [];
  if (Array.isArray(rawUser.roles)) {
    rolesList = rawUser.roles
      .map((r) => (typeof r === "string" ? normalizeRoleString(r) : normalizeRoleString(r?.name || r?.authority)))
      .filter(Boolean);
  }
  if (primaryRole && !rolesList.includes(primaryRole)) {
    rolesList.unshift(primaryRole);
  }

  return {
    ...rawUser,
    role: primaryRole,
    roles: rolesList,
  };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem("user");
    if (!storedUser) return null;
    try {
      const parsed = JSON.parse(storedUser);
      return normalizeUser(parsed);
    } catch {
      return null;
    }
  });

  const [loading, setLoading] = useState(true);

  const login = async (username, password) => {
    const data = await authService.login(username, password);

    const token = data.accessToken || data.token;
    const rawUser = data.user || data;
    const normalizedUser = normalizeUser(rawUser);

    if (token) {
      localStorage.setItem("accessToken", token);
    }
    if (normalizedUser) {
      localStorage.setItem("user", JSON.stringify(normalizedUser));
    }

    setUser(normalizedUser);

    return data;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  useEffect(() => {
    const restoreSession = async () => {
      const token = localStorage.getItem("accessToken");

      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const currentUser = await authService.getCurrentUser();
        const normalizedUser = normalizeUser(currentUser);

        if (normalizedUser) {
          localStorage.setItem("user", JSON.stringify(normalizedUser));
        }
        setUser(normalizedUser);
      } catch {
        authService.logout();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    restoreSession();
  }, []);

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};