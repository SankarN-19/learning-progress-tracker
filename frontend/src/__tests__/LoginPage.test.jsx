import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import LoginPage from '../pages/LoginPage';

const auth = vi.hoisted(() => ({ value: {} }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth.value }));

function setup(overrides = {}) {
  auth.value = { login: vi.fn().mockResolvedValue({}), notice: '', ...overrides };
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
  return userEvent.setup();
}

describe('LoginPage', () => {
  it('asks for an email and a password when the form is empty', async () => {
    const user = setup();
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(auth.value.login).not.toHaveBeenCalled();
  });

  it('submits the trimmed email and the password', async () => {
    const user = setup();
    await user.type(screen.getByLabelText('Email'), '  jane@example.test ');
    await user.type(screen.getByLabelText('Password'), 'secret-pass');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(auth.value.login).toHaveBeenCalledWith('jane@example.test', 'secret-pass');
  });

  it('shows the server message, without saying whether the account exists', async () => {
    const user = setup({
      login: vi.fn().mockRejectedValue({ response: { status: 401, data: { message: 'Invalid email or password.' } } }),
    });
    await user.type(screen.getByLabelText('Email'), 'jane@example.test');
    await user.type(screen.getByLabelText('Password'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.');
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled(); // can try again
  });

  it('shows a friendly message when the server cannot be reached', async () => {
    const user = setup({ login: vi.fn().mockRejectedValue({ request: {} }) });
    await user.type(screen.getByLabelText('Email'), 'jane@example.test');
    await user.type(screen.getByLabelText('Password'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
  });

  it('can show and hide the password', async () => {
    const user = setup();
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Show password' }));
    expect(input).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: 'Hide password' }));
    expect(input).toHaveAttribute('type', 'password');
  });

  it('explains why the user is back on the login page after a session ends', () => {
    setup({ notice: 'Your session has ended. Please sign in again.' });

    expect(screen.getByRole('status')).toHaveTextContent('Your session has ended');
  });

  it('does not offer public registration and says where accounts come from', () => {
    setup();

    expect(screen.queryByRole('link', { name: /sign up|register|create account/i })).not.toBeInTheDocument();
    expect(screen.getByText(/created by your coordinator/i)).toBeInTheDocument();
  });
});
