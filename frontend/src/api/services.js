import api from './client';

const data = (promise) => promise.then((res) => res.data);

export const authApi = {
  me: () => data(api.get('/api/v1/auth/me')),
  login: (email, password) => data(api.post('/api/v1/auth/login', { email, password })),
  changePassword: (currentPassword, newPassword) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),
  logout: () => api.post('/api/v1/auth/logout'),
};

export const activitiesApi = {
  list: () => data(api.get('/api/v1/activities')),
  create: (payload) => data(api.post('/api/v1/activities', payload)),
  update: (id, payload) => data(api.put(`/api/v1/activities/${id}`, payload)),
  remove: (id) => api.delete(`/api/v1/activities/${id}`),
};

export const learnersApi = {
  list: () => data(api.get('/api/v1/learners')),
  create: (payload) => data(api.post('/api/v1/learners', payload)),
  assignments: (id) => data(api.get(`/api/v1/learners/${id}/assignments`)),
  resetPassword: (id) => data(api.post(`/api/v1/learners/${id}/reset-password`)),
  remove: (id) => api.delete(`/api/v1/learners/${id}`),
};

export const assignmentsApi = {
  assign: (learnerId, activityId) => data(api.post('/api/v1/assignments', { learnerId, activityId })),
  updateStatus: (id, status) => data(api.patch(`/api/v1/assignments/${id}/status`, { status })),
};

export const meApi = {
  assignments: () => data(api.get('/api/v1/me/assignments')),
};

export const summaryApi = {
  get: () => data(api.get('/api/v1/cohort-summary')),
};
