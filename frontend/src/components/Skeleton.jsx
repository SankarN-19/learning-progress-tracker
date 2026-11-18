/** Placeholder blocks shown while a page loads. */
export default function PageSkeleton({ label = 'Loading…' }) {
  return (
    <div role="status" className="skeleton-page">
      <span className="sr-only">{label}</span>
      <div className="skeleton skeleton-title" aria-hidden="true" />
      <div className="skeleton-row" aria-hidden="true">
        <div className="skeleton skeleton-tile" />
        <div className="skeleton skeleton-tile" />
        <div className="skeleton skeleton-tile" />
      </div>
      <div className="skeleton skeleton-block" aria-hidden="true" />
    </div>
  );
}
