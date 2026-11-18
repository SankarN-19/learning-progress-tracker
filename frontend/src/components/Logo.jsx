/** Three rising steps, the top one in the accent colour. */
export default function Logo({ light = false }) {
  return (
    <span className={`logo${light ? ' logo-light' : ''}`}>
      <svg width="30" height="30" viewBox="0 0 28 28" aria-hidden="true" focusable="false">
        <rect x="3" y="15" width="6" height="10" rx="1.5" fill="currentColor" opacity="0.55" />
        <rect x="11" y="9" width="6" height="16" rx="1.5" fill="currentColor" opacity="0.8" />
        <rect x="19" y="3" width="6" height="22" rx="1.5" fill="#F0B429" />
      </svg>
      <span className="logo-text">Learning Progress Tracker</span>
    </span>
  );
}
