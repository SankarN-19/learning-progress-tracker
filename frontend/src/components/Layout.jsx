import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Avatar from './Avatar';
import Icon from './Icons';
import Logo from './Logo';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isCoordinator = user.role === 'COORDINATOR';
  const displayName = user.email.split('@')[0].replace(/[._-]+/g, ' ');

  async function onLogout() {
    await logout();
    navigate('/', { replace: true });
  }

  return (
    <div className="shell">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <aside className="sidebar">
        <Link to="/" className="sidebar-brand" aria-label="Learning Progress Tracker home">
          <Logo light />
        </Link>

        <nav aria-label="Main" className="side-nav">
          {isCoordinator ? (
            <>
              <NavLink to="/dashboard">
                <Icon name="dashboard" /> <span>Dashboard</span>
              </NavLink>
              <NavLink to="/activities">
                <Icon name="activities" /> <span>Activities</span>
              </NavLink>
              <NavLink to="/learners">
                <Icon name="users" /> <span>Learners</span>
              </NavLink>
            </>
          ) : (
            <NavLink to="/my-training">
              <Icon name="flag" /> <span>My training</span>
            </NavLink>
          )}
        </nav>

        <div className="sidebar-user">
          <Avatar name={displayName} />
          <div className="sidebar-user-text">
            <span className="sidebar-role">{isCoordinator ? 'Coordinator' : 'Candidate'}</span>
            <span className="sidebar-email">{user.email}</span>
          </div>
          <button type="button" className="side-logout" onClick={onLogout}>
            <Icon name="logout" size={18} /> <span>Log out</span>
          </button>
        </div>
      </aside>

      <main id="main" className="content" tabIndex={-1}>
        <div className="content-inner">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
