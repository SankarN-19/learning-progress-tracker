import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AddCandidateModal } from '../pages/LearnersPage';
import { ToastProvider } from '../components/Toast';
import { learnersApi } from '../api/services';

vi.mock('../api/services', () => ({
  learnersApi: { list: vi.fn(), create: vi.fn() },
}));

const onClose = vi.fn();
const onCreated = vi.fn();

const CREATED = {
  learner: { id: 4, displayName: 'Jane Doe', cohort: 'Backend cohort', email: 'jane@example.test' },
  credentials: { username: 'jane@example.test', temporaryPassword: 'Abcd2345Wxyz67', expiresAt: '2026-11-15T10:00:00Z' },
};

function setup() {
  render(
    <ToastProvider>
      <AddCandidateModal onClose={onClose} onCreated={onCreated} />
    </ToastProvider>
  );
  return userEvent.setup();
}

async function fill(user, { name = 'Jane Doe', email = 'jane@example.test', cohort = 'Backend cohort' } = {}) {
  if (name) await user.type(screen.getByLabelText('Full name'), name);
  if (email) await user.type(screen.getByLabelText('Email'), email);
  if (cohort) await user.type(screen.getByLabelText('Cohort'), cohort);
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('AddCandidateModal', () => {
  it('requires name, email and cohort', async () => {
    const user = setup();
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));

    expect(await screen.findByText('Name is required.')).toBeInTheDocument();
    expect(screen.getByText('Email is required.')).toBeInTheDocument();
    expect(screen.getByText('Cohort is required.')).toBeInTheDocument();
    expect(learnersApi.create).not.toHaveBeenCalled();
  });

  it('rejects an email that is not an email', async () => {
    const user = setup();
    await fill(user, { email: 'not-an-email' });
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));

    expect(await screen.findByText('Enter a valid email address.')).toBeInTheDocument();
    expect(learnersApi.create).not.toHaveBeenCalled();
  });

  it('shows the duplicate-email message inline on the email field', async () => {
    learnersApi.create.mockRejectedValue({
      response: {
        status: 409,
        data: { message: 'A candidate with this email already exists.', errors: { email: 'A candidate with this email already exists.' } },
      },
    });
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));

    expect(await screen.findByText('A candidate with this email already exists.')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByText(/Shown only once/)).not.toBeInTheDocument(); // no credentials were issued
  });

  it('shows the one-time credentials, with a warning, after the candidate is created', async () => {
    learnersApi.create.mockResolvedValue(CREATED);
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));

    expect(learnersApi.create).toHaveBeenCalledWith({
      name: 'Jane Doe',
      email: 'jane@example.test',
      cohort: 'Backend cohort',
    });
    expect(await screen.findByText(/Shown only once/)).toBeInTheDocument();
    expect(screen.getByText('Abcd2345Wxyz67')).toBeInTheDocument();
    expect(onCreated).toHaveBeenCalled();
  });

  it('copies the credentials to the clipboard', async () => {
    learnersApi.create.mockResolvedValue(CREATED);
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));

    await user.click(await screen.findByRole('button', { name: 'Copy credentials' }));

    expect(await screen.findByRole('button', { name: 'Copied' })).toBeInTheDocument();
    expect(await navigator.clipboard.readText()).toContain('Abcd2345Wxyz67');
  });

  it('cannot be dismissed by accident while the credentials are showing', async () => {
    learnersApi.create.mockResolvedValue(CREATED);
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Add candidate' }));
    await screen.findByText(/Shown only once/);

    await user.keyboard('{Escape}');
    expect(onClose).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: 'Close dialog' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /I have copied them/ }));
    expect(onClose).toHaveBeenCalled();
  });
});
