import { Link } from 'react-router-dom';
import Logo from '../components/Logo';

export default function NotFoundPage() {
  return (
    <div className="center-page">
      <main id="main" className="center-card">
        <Logo />
        <h1>We can’t find that page</h1>
        <p className="muted">The address may be mistyped, or the page may have moved.</p>
        <Link className="btn btn-primary" to="/">
          Go to the home page
        </Link>
      </main>
    </div>
  );
}
