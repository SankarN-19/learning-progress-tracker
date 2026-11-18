import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { authApi } from '../api/services';
import { setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { email, role, mustChangePassword } | null
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState('');
  const userRef = useRef(null);

  useEffect(() => {
    userRef.current = user;
  }, [user]);

  // Any 401 from a normal API call = the session ended.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (userRef.current) {
        setNotice('Your session has ended. Please sign in again.');
        setUser(null);
      }
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  // Restore the session after a page refresh. This GET also makes Spring send the XSRF-TOKEN cookie.
  useEffect(() => {
    let alive = true;
    authApi
      .me()
      .then((me) => alive && setUser(me))
      .catch(() => alive && setUser(null))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, []);

  const login = useCallback(async (email, password) => {
    await authApi.login(email, password);
    const me = await authApi.me();
    setNotice('');
    setUser(me);
    return me;
  }, []);

  const changePassword = useCallback(async (currentPassword, newPassword) => {
    await authApi.changePassword(currentPassword, newPassword);
    setUser((u) => (u ? { ...u, mustChangePassword: false } : u));
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      /* the local session is cleared either way */
    }
    setNotice('');
    setUser(null);
  }, []);

  const endSession = useCallback((message) => {
    setNotice(message);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, notice, login, changePassword, logout, endSession }),
    [user, loading, notice, login, changePassword, logout, endSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
