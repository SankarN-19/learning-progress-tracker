export const STATUSES = ['Not Started', 'In Progress', 'Completed'];

export function formatHours(hours) {
  return `${Number(hours)} h`;
}

export function formatDateTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
