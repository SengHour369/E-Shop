// src/contexts/AuthContext.jsx
import { createContext, useCallback, useContext, useState } from 'react';

const AuthContext = createContext(null);

const TOKEN_KEY = 'accessToken';

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? null);
  const [user, setUser] = useState(null);

  /** Persist token after a successful login response. */
  const login = useCallback((accessToken, userPayload = null) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    setToken(accessToken);
    if (userPayload) setUser(userPayload);
  }, []);

  /** Clear auth state on logout (mirrors the interceptor cleanup). */
  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const isAuthenticated = Boolean(token);

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthContext;
