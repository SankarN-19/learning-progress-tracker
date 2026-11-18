import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ChangePasswordPage from '../pages/ChangePasswordPage';
import { ToastProvider } from '../components/Toast';

const auth = vi.hoisted(() => ({ value: {} }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth.value }));

function setup(overrides = {}) {
  auth.value = {
    user: { email: 'jane@example.test', role: 'CANDIDATE', mustChangePassword: true },
    changePassword: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    endSession: vi.fn(),
    ...overrides,
  };
  render(
    <MemoryRouter>
      <ToastProvider>
        <ChangePasswordPage />
      </ToastProvider>
    </MemoryRouter>
  );
  return userEvent.setup();
}

async function fill(user, { current = 'Temp-Pass-1234', next = 'Brand-new-pass-1', confirm = next } = {}) {
  await user.type(screen.getByLabelText('Temporary password'), current);
  await user.type(screen.getByLabelText('New password'), next);
  await user.type(screen.getByLabelText('Confirm new password'), confirm);
}

describe('ChangePasswordPage', () => {
  it('shows who is signed in', () => {
    setup();
    expect(screen.getByText(/jane@example.test/)).toBeInTheDocument();
  });

  it('rejects a new password that is too short', async () => {
    const user = setup();
    await fill(user, { next: 'short' });
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    expect(await screen.findByText('Use at least 8 characters.')).toBeInTheDocument();
    expect(auth.value.changePassword).not.toHaveBeenCalled();
  });

  it('rejects reusing the temporary password', async () => {
    const user = setup();
    await fill(user, { current: 'Temp-Pass-1234', next: 'Temp-Pass-1234' });
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    expect(await screen.findByText('Choose a password different from the temporary one.')).toBeInTheDocument();
    expect(auth.value.changePassword).not.toHaveBeenCalled();
  });

  it('rejects a confirmation that does not match', async () => {
    const user = setup();
    await fill(user, { next: 'Brand-new-pass-1', confirm: 'Different-pass-2' });
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    expect(await screen.findByText('The passwords do not match.')).toBeInTheDocument();
    expect(auth.value.changePassword).not.toHaveBeenCalled();
  });

  it('ticks the requirements off as the user types', async () => {
    const user = setup();
    const lengthRule = screen.getByText('At least 8 characters');
    expect(lengthRule).not.toHaveClass('req-met');

    await user.type(screen.getByLabelText('New password'), 'LongEnough1');
    expect(lengthRule).toHaveClass('req-met');

    await user.type(screen.getByLabelText('Confirm new password'), 'LongEnough1');
    expect(screen.getByText('Both entries match')).toHaveClass('req-met');
  });

  it('changes the password and confirms with a message', async () => {
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    expect(auth.value.changePassword).toHaveBeenCalledWith('Temp-Pass-1234', 'Brand-new-pass-1');
    expect(await screen.findByText('Password updated. Welcome!')).toBeInTheDocument();
  });

  it('shows a server error on the field it belongs to', async () => {
    const user = setup({
      changePassword: vi.fn().mockRejectedValue({
        response: {
          status: 400,
          data: { message: 'Current password is incorrect.', errors: { currentPassword: 'Current password is incorrect.' } },
        },
      }),
    });
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    expect(await screen.findByText('Current password is incorrect.')).toBeInTheDocument();
    expect(screen.getByLabelText('Temporary password')).toHaveAttribute('aria-invalid', 'true');
  });

  it('ends the session when the temporary password has expired', async () => {
    const message = 'Your temporary password has expired. Ask a coordinator for a reset.';
    const user = setup({
      changePassword: vi.fn().mockRejectedValue({ response: { status: 401, data: { message } } }),
    });
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Update password' }));

    await vi.waitFor(() => expect(auth.value.endSession).toHaveBeenCalledWith(message));
  });
});
