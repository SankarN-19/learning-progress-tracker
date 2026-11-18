import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import LandingPage from '../pages/LandingPage';

const auth = vi.hoisted(() => ({ value: {} }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth.value }));

function setup(user = null) {
  auth.value = { user, loading: false };
  render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>
  );
  return userEvent.setup();
}

describe('LandingPage', () => {
  it('explains the product and points visitors to sign in', () => {
    setup();

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Know where every trainee stands');
    const signInLinks = screen.getAllByRole('link', { name: 'Sign in' });
    expect(signInLinks.length).toBeGreaterThan(0);
    signInLinks.forEach((link) => expect(link).toHaveAttribute('href', '/login'));
  });

  it('says there is no public sign-up and offers no registration link', () => {
    setup();

    expect(screen.getByText(/There is no public sign-up/)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /sign up|register|create account/i })).not.toBeInTheDocument();
  });

  it('describes the six-step flow', () => {
    setup();

    expect(screen.getAllByRole('listitem').some((li) => li.textContent.includes('Add a candidate'))).toBe(true);
    expect(screen.getByRole('heading', { name: 'Review the cohort' })).toBeInTheDocument();
  });

  it('sends a signed-in coordinator to the dashboard', () => {
    setup({ email: 'c@example.test', role: 'COORDINATOR', mustChangePassword: false });

    const link = screen.getAllByRole('link', { name: 'Open dashboard' })[0];
    expect(link).toHaveAttribute('href', '/dashboard');
  });

  it('sends a signed-in candidate to their training', () => {
    setup({ email: 'k@example.test', role: 'CANDIDATE', mustChangePassword: false });

    expect(screen.getAllByRole('link', { name: 'Open my training' })[0]).toHaveAttribute('href', '/my-training');
  });

  it('lets visitors try the sample board: statuses change and the totals follow', async () => {
    const user = setup();
    expect(screen.getByText('2 of 6 activities completed')).toBeInTheDocument();

    const chip = () => screen.getByRole('button', { name: /Sam Okafor, Git and Collaboration/ });
    await user.click(chip()); // Not Started -> In Progress
    expect(chip()).toHaveAccessibleName(/In Progress/);
    expect(screen.getByText('2 of 6 activities completed')).toBeInTheDocument();

    await user.click(chip()); // In Progress -> Completed
    expect(screen.getByText('3 of 6 activities completed')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Reset sample' }));
    expect(screen.getByText('2 of 6 activities completed')).toBeInTheDocument();
  });
});
