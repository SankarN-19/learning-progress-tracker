import { Link } from 'react-router-dom';
import Icon from './Icons';
import Logo from './Logo';

const DEFAULT_ASIDE = {
  title: 'Pick up where you left off.',
  text: 'Coordinators manage cohorts and training. Candidates see their own training and keep their progress up to date.',
  points: [
    { icon: 'users', text: 'Your coordinator created your account.' },
    { icon: 'lock', text: 'You only ever see your own training.' },
    { icon: 'clock', text: 'Temporary passwords expire after 24 hours.' },
  ],
};

/** Split layout used by Login and Change Password: a brand panel beside the form. */
export default function AuthShell({ title, subtitle, children, aside = DEFAULT_ASIDE }) {
  return (
    <div className="auth-split">
      <aside className="auth-aside">
        <Link to="/" className="auth-brand" aria-label="Learning Progress Tracker home">
          <Logo light />
        </Link>
        <div className="auth-aside-body">
          <h2>{aside.title}</h2>
          <p>{aside.text}</p>
          <ul className="auth-points">
            {aside.points.map((p) => (
              <li key={p.text}>
                <span className="auth-point-icon">
                  <Icon name={p.icon} size={18} />
                </span>
                {p.text}
              </li>
            ))}
          </ul>
        </div>
      </aside>

      <main id="main" className="auth-main">
        <div className="auth-form-wrap">
          <Link to="/" className="back-link">
            Back to home
          </Link>
          <h1>{title}</h1>
          {subtitle && <p className="muted lead-sm">{subtitle}</p>}
          {children}
        </div>
      </main>
    </div>
  );
}
