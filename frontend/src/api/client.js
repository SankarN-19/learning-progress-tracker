import axios from 'axios';

// Same-origin requests (Vite proxy). Axios reads the XSRF-TOKEN cookie that Spring sets
// and sends it back in the X-XSRF-TOKEN header on every unsafe request.
const api = axios.create({
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'Content-Type': 'application/json' },
});

let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

// A 401 on any normal call means the session ended (expired, logged out elsewhere, or reset).
// Login / me / change-password handle their own 401s.
const SELF_HANDLED = ['/auth/login', '/auth/me', '/auth/change-password'];
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    if (status === 401 && onUnauthorized && !SELF_HANDLED.some((p) => url.includes(p))) {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (err && err.response) return err.response.data?.message || fallback;
  if (err && err.request) return 'Cannot reach the server. Check that the backend is running.';
  return fallback;
}

/** Field-level messages from the API: { email: "...", plannedHours: "..." } */
export function fieldErrors(err) {
  return err?.response?.data?.errors || {};
}

export default api;
