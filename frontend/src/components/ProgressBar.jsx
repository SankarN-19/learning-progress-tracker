export default function ProgressBar({ done, total, label = 'Completion' }) {
  if (!total) return <span className="muted">No training assigned yet.</span>;
  const pct = Math.round((done / total) * 100);
  return (
    <div className="progress">
      <div
        className="progress-track"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={pct}
      >
        <div className="progress-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="progress-text">
        {done} of {total} completed
      </span>
    </div>
  );
}
