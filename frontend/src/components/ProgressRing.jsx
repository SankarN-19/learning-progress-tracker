const R = 42;
const C = 2 * Math.PI * R;

export default function ProgressRing({ done, total, size = 120 }) {
  const pct = total ? Math.round((done / total) * 100) : 0;
  return (
    <svg
      className="ring"
      width={size}
      height={size}
      viewBox="0 0 100 100"
      role="img"
      aria-label={total ? `${done} of ${total} completed (${pct} percent)` : 'No training assigned yet'}
    >
      <circle className="ring-bg" cx="50" cy="50" r={R} fill="none" strokeWidth="10" />
      <circle
        className="ring-fg"
        cx="50"
        cy="50"
        r={R}
        fill="none"
        strokeWidth="10"
        strokeLinecap="round"
        strokeDasharray={`${(C * pct) / 100} ${C}`}
        transform="rotate(-90 50 50)"
      />
      <text className="ring-text" x="50" y="50" textAnchor="middle" dominantBaseline="central">
        {total ? `${pct}%` : '–'}
      </text>
    </svg>
  );
}
