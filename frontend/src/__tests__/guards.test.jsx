import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { PublicOnly, RequireAuth, RequirePasswordChange } from '../auth/guards';

const auth = vi.hoisted(() => ({ value: {} }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth.value }));

const COORDINATOR = { email: 'c@example.test', role: 'COORDINATOR', mustChangePassword: false };
const CANDIDATE = { email: 'k@example.test', role: 'CANDIDATE', mustChangePassword: false };
const NEW_CANDIDATE = { email: 'n@example.test', role: 'CANDIDATE', mustChangePassword: true };

function visit(path, user, loading = false) {
  auth.value = { user, loading };
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<PublicOnly />}>
          <Route path="/login" element={<p>login page</p>} />
        </Route>
        <Route element={<RequirePasswordChange />}>
          <Route path="/change-password" element={<p>change password page</p>} />
        </Route>
        <Route element={<RequireAuth roles={['COORDINATOR']} />}>
          <Route path="/dashboard" element={<p>dashboard page</p>} />
        </Route>
        <Route element={<RequireAuth roles={['CANDIDATE']} />}>
          <Route path="/my-training" element={<p>training page</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe('route guards mirror the server rules', () => {
  it('sends signed-out visitors to the login page', () => {
    visit('/dashboard', null);
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('waits while the session is being restored', () => {
    visit('/dashboard', null, true);
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('lets a coordinator into coordinator pages', () => {
    visit('/dashboard', COORDINATOR);
    expect(screen.getByText('dashboard page')).toBeInTheDocument();
  });

  it('keeps candidates out of coordinator pages', () => {
    visit('/dashboard', CANDIDATE);
    expect(screen.getByText('training page')).toBeInTheDocument();
  });

  it('keeps coordinators out of the candidate page', () => {
    visit('/my-training', COORDINATOR);
    expect(screen.getByText('dashboard page')).toBeInTheDocument();
  });

  it('forces a candidate with a temporary password to change it first', () => {
    visit('/my-training', NEW_CANDIDATE);
    expect(screen.getByText('change password page')).toBeInTheDocument();
  });

  it('does not let a pending password change reach any other page', () => {
    visit('/dashboard', NEW_CANDIDATE);
    expect(screen.getByText('change password page')).toBeInTheDocument();
  });

  it('shows the change-password page only to users who need it', () => {
    visit('/change-password', CANDIDATE);
    expect(screen.getByText('training page')).toBeInTheDocument();
  });

  it('sends signed-out visitors away from change-password', () => {
    visit('/change-password', null);
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('sends signed-in users away from the login page', () => {
    visit('/login', COORDINATOR);
    expect(screen.getByText('dashboard page')).toBeInTheDocument();
  });
});
