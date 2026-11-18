// Text plus a symbol, so the status never depends on colour alone.
const ICONS = { 'Not Started': '○', 'In Progress': '◐', Completed: '✓' };

export default function StatusBadge({ status }) {
  const key = status.toLowerCase().replace(/\s+/g, '-');
  return (
    <span className={`badge badge-${key}`}>
      <span aria-hidden="true">{ICONS[status] || '•'}</span> {status}
    </span>
  );
}
