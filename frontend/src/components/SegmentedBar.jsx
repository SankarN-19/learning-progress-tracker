/** One bar split into Completed / In progress / Not started, with a text legend. */
export default function SegmentedBar({ completed, inProgress, notStarted, showLegend = true }) {
  const total = completed + inProgress + notStarted;
  if (total === 0) return <p className="muted">No training assigned yet.</p>;
  const pct = (n) => `${(n / total) * 100}%`;
  const label = `${completed} completed, ${inProgress} in progress, ${notStarted} not started`;
  return (
    <div className="segbar-wrap">
      <div className="segbar" role="img" aria-label={label}>
        <span className="seg-done" style={{ width: pct(completed) }} />
        <span className="seg-doing" style={{ width: pct(inProgress) }} />
        <span className="seg-todo" style={{ width: pct(notStarted) }} />
      </div>
      {showLegend && (
        <ul className="legend">
          <li>
            <span className="dot seg-done" /> Completed <strong>{completed}</strong>
          </li>
          <li>
            <span className="dot seg-doing" /> In progress <strong>{inProgress}</strong>
          </li>
          <li>
            <span className="dot seg-todo" /> Not started <strong>{notStarted}</strong>
          </li>
        </ul>
      )}
    </div>
  );
}
