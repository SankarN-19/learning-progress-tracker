export function initials(name) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return (first + last).toUpperCase();
}

function hash(text) {
  return [...text].reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
}

export default function Avatar({ name, size = 'md' }) {
  return (
    <span className={`avatar avatar-${size} tone-${hash(name) % 5}`} aria-hidden="true">
      {initials(name)}
    </span>
  );
}
