import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Spinner from '../components/Spinner';

/** Where a signed-in user belongs. Mirrors the server rules. */
export function homePath(user) {
  if (user.mustChangePassword) return '/change-password';
  return user.role === 'COORDINATOR' ? '/dashboard' : '/my-training';
}

/** Needs a signed-in user (with the right role) who has already changed the temporary password. */
export function RequireAuth({ roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Spinner label="Loading…" />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.mustChangePassword) return <Navigate to="/change-password" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={homePath(user)} replace />;
  return <Outlet />;
}

/** Only for users who still have a temporary password. */
export function RequirePasswordChange() {
  const { user, loading } = useAuth();
  if (loading) return <Spinner label="Loading…" />;
  if (!user) return <Navigate to="/login" replace />;
  if (!user.mustChangePassword) return <Navigate to={homePath(user)} replace />;
  return <Outlet />;
}

/** Login page: signed-in users are sent to their own home instead. */
export function PublicOnly() {
  const { user, loading } = useAuth();
  if (loading) return <Spinner label="Loading…" />;
  if (user) return <Navigate to={homePath(user)} replace />;
  return <Outlet />;
}

export function HomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <Spinner label="Loading…" />;
  return <Navigate to={user ? homePath(user) : '/login'} replace />;
}
