import { useState } from 'react';
import AuthShell from '../components/AuthShell';
import { PasswordInput, TextField } from '../components/Fields';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';

export default function LoginPage() {
  const { login, notice } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!email.trim()) next.email = 'Enter your email address.';
    if (!password) next.password = 'Enter your password.';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length > 0) return;

    setBusy(true);
    try {
      // On success the route guard moves the user to their own home page.
      await login(email.trim(), password);
    } catch (err) {
      setFormError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <AuthShell title="Sign in" subtitle="Use the email and password your coordinator gave you.">
      {notice && (
        <p className="alert alert-info" role="status">
          {notice}
        </p>
      )}
      {formError && (
        <p className="alert alert-error" role="alert">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <TextField
          id="login-email"
          label="Email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <PasswordInput
          id="login-password"
          label="Password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
        />
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="center muted">
        Do not have an account yet? Accounts are created by your coordinator. Ask them for your email and temporary
        password.
      </p>
    </AuthShell>
  );
}
