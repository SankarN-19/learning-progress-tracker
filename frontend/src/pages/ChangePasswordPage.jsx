import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthShell from '../components/AuthShell';
import { PasswordInput } from '../components/Fields';
import Icon from '../components/Icons';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { errorMessage, fieldErrors } from '../api/client';

const ASIDE = {
  title: 'One quick step before you start.',
  text: 'The password you were given is temporary. Choose one only you know, and you are in.',
  points: [
    { icon: 'key', text: 'The temporary password stops working once you change it.' },
    { icon: 'shield', text: 'Other sessions on your account are signed out.' },
    { icon: 'lock', text: 'Nobody else, including your coordinator, can see your new password.' },
  ],
};

function Requirement({ met, children }) {
  return (
    <li className={met ? 'req req-met' : 'req'}>
      <span className="req-mark" aria-hidden="true">
        {met && <Icon name="check" size={14} />}
      </span>
      <span className="sr-only">{met ? 'Met: ' : 'Not met yet: '}</span>
      {children}
    </li>
  );
}

export default function ChangePasswordPage() {
  const { user, changePassword, logout, endSession } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  function validate() {
    const e = {};
    if (!current) e.currentPassword = 'Enter your temporary password.';
    if (next.length < 8) e.newPassword = 'Use at least 8 characters.';
    else if (next.length > 72) e.newPassword = 'Use at most 72 characters.';
    else if (next === current) e.newPassword = 'Choose a password different from the temporary one.';
    if (confirm !== next) e.confirm = 'The passwords do not match.';
    return e;
  }

  async function onSubmit(ev) {
    ev.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length > 0) return;

    setBusy(true);
    try {
      await changePassword(current, next);
      toast.success('Password updated. Welcome!');
      // The route guard now sends the user to their home page.
    } catch (err) {
      if (err.response?.status === 401) {
        // e.g. the temporary password expired: the server ended the session
        endSession(errorMessage(err));
        return;
      }
      const fe = fieldErrors(err);
      setErrors(fe);
      setFormError(Object.keys(fe).length ? '' : errorMessage(err));
      setBusy(false);
    }
  }

  async function onLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <AuthShell
      title="Choose a new password"
      subtitle={`You are signed in as ${user.email} with a temporary password.`}
      aside={ASIDE}
    >
      {formError && (
        <p className="alert alert-error" role="alert">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <PasswordInput
          id="cp-current"
          label="Temporary password"
          autoComplete="current-password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          error={errors.currentPassword}
        />
        <PasswordInput
          id="cp-new"
          label="New password"
          autoComplete="new-password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          error={errors.newPassword}
        />
        <PasswordInput
          id="cp-confirm"
          label="Confirm new password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          error={errors.confirm}
        />

        <ul className="reqs" aria-label="Password requirements">
          <Requirement met={next.length >= 8}>At least 8 characters</Requirement>
          <Requirement met={next.length > 0 && next !== current}>Different from the temporary password</Requirement>
          <Requirement met={confirm.length > 0 && confirm === next}>Both entries match</Requirement>
        </ul>

        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy}>
          {busy ? 'Saving…' : 'Update password'}
        </button>
      </form>
      <p className="center">
        <button type="button" className="link-btn" onClick={onLogout}>
          Log out instead
        </button>
      </p>
    </AuthShell>
  );
}
