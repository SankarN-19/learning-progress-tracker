import Icon from './Icons';

/** An empty screen is an invitation to act: say what is missing and offer the next step. */
export default function EmptyState({ icon = 'inbox', title, children, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} size={26} />
      </span>
      <h2>{title}</h2>
      {children && <p className="muted">{children}</p>}
      {action}
    </div>
  );
}
