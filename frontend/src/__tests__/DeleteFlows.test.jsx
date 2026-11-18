import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ConfirmDialog from '../components/ConfirmDialog';
import ActivitiesPage from '../pages/ActivitiesPage';
import LearnersPage from '../pages/LearnersPage';
import { ToastProvider } from '../components/Toast';
import { activitiesApi, learnersApi } from '../api/services';

vi.mock('../api/services', () => ({
  activitiesApi: { list: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() },
  learnersApi: { list: vi.fn(), create: vi.fn(), remove: vi.fn() },
}));

const ACTIVITIES = [
  { id: 1, title: 'React Basics', topic: 'Frontend', plannedHours: 6 },
  { id: 2, title: 'REST APIs', topic: 'Backend', plannedHours: 8 },
];
const LEARNERS = [
  { id: 4, displayName: 'Jane Doe', cohort: 'Backend cohort', email: 'jane@example.test', assignedCount: 2, completedCount: 1 },
];

beforeEach(() => {
  vi.clearAllMocks();
  activitiesApi.list.mockResolvedValue(ACTIVITIES);
  activitiesApi.remove.mockResolvedValue(undefined);
  learnersApi.list.mockResolvedValue(LEARNERS);
  learnersApi.remove.mockResolvedValue(undefined);
});

describe('ConfirmDialog', () => {
  it('can be cancelled without confirming', async () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog title="Delete thing" onConfirm={onConfirm} onCancel={onCancel}>
        <p>This cannot be undone.</p>
      </ConfirmDialog>
    );

    expect(screen.getByRole('dialog', { name: 'Delete thing' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('confirms, and shows an error or a busy state', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(
      <ConfirmDialog title="Delete thing" onConfirm={onConfirm} onCancel={() => {}}>
        <p>Sure?</p>
      </ConfirmDialog>
    );
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(onConfirm).toHaveBeenCalled();

    rerender(
      <ConfirmDialog title="Delete thing" busy error="Learner not found." onConfirm={onConfirm} onCancel={() => {}}>
        <p>Sure?</p>
      </ConfirmDialog>
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Learner not found.');
    expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
  });
});

describe('deleting an activity', () => {
  function setup() {
    render(
      <ToastProvider>
        <ActivitiesPage />
      </ToastProvider>
    );
    return userEvent.setup();
  }

  it('asks first, then deletes and refreshes the list', async () => {
    const user = setup();
    await user.click(await screen.findByRole('button', { name: 'Delete REST APIs' }));

    expect(screen.getByRole('dialog', { name: 'Delete activity' })).toHaveTextContent('removed from every learner');
    expect(activitiesApi.remove).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(activitiesApi.remove).toHaveBeenCalledWith(2);
    expect(await screen.findByText('Activity deleted.')).toBeInTheDocument();
    expect(activitiesApi.list).toHaveBeenCalledTimes(2);
  });

  it('does nothing when cancelled', async () => {
    const user = setup();
    await user.click(await screen.findByRole('button', { name: 'Delete REST APIs' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(activitiesApi.remove).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps the dialog open and shows the error when deleting fails', async () => {
    activitiesApi.remove.mockRejectedValue({ response: { status: 404, data: { message: 'Activity not found.' } } });
    const user = setup();
    await user.click(await screen.findByRole('button', { name: 'Delete REST APIs' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Activity not found.');
    expect(screen.getByRole('dialog', { name: 'Delete activity' })).toBeInTheDocument();
    expect(activitiesApi.list).toHaveBeenCalledTimes(1);
  });
});

describe('deleting a learner', () => {
  function setup() {
    render(
      <MemoryRouter>
        <ToastProvider>
          <LearnersPage />
        </ToastProvider>
      </MemoryRouter>
    );
    return userEvent.setup();
  }

  it('says what will be removed, then deletes and refreshes the list', async () => {
    const user = setup();
    await user.click(await screen.findByRole('button', { name: 'Delete Jane Doe' }));

    expect(screen.getByRole('dialog', { name: 'Delete learner' })).toHaveTextContent(/2 assignments/);
    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(learnersApi.remove).toHaveBeenCalledWith(4);
    expect(await screen.findByText('Learner deleted.')).toBeInTheDocument();
    expect(learnersApi.list).toHaveBeenCalledTimes(2);
  });

  it('can be cancelled', async () => {
    const user = setup();
    await user.click(await screen.findByRole('button', { name: 'Delete Jane Doe' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(learnersApi.remove).not.toHaveBeenCalled();
  });
});
