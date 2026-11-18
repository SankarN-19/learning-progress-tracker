import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MyTrainingPage from '../pages/MyTrainingPage';
import { ToastProvider } from '../components/Toast';
import { assignmentsApi, meApi } from '../api/services';

vi.mock('../api/services', () => ({
  meApi: { assignments: vi.fn() },
  assignmentsApi: { updateStatus: vi.fn() },
}));

const ASSIGNMENTS = [
  { id: 1, learnerId: 1, activityId: 1, status: 'In Progress', activity: { id: 1, title: 'React Basics', topic: 'Frontend', plannedHours: 6 } },
  { id: 2, learnerId: 1, activityId: 2, status: 'Not Started', activity: { id: 2, title: 'REST APIs', topic: 'Backend', plannedHours: 8 } },
];

function setup() {
  render(
    <ToastProvider>
      <MyTrainingPage />
    </ToastProvider>
  );
  return userEvent.setup();
}

beforeEach(() => {
  vi.clearAllMocks();
  meApi.assignments.mockResolvedValue(ASSIGNMENTS);
  assignmentsApi.updateStatus.mockResolvedValue({});
});

describe('MyTrainingPage', () => {
  it('shows a loading state first', () => {
    meApi.assignments.mockReturnValue(new Promise(() => {}));
    setup();

    expect(screen.getByText('Loading your training…')).toBeInTheDocument();
  });

  it('shows an empty state when nothing is assigned', async () => {
    meApi.assignments.mockResolvedValue([]);
    setup();

    expect(await screen.findByRole('heading', { name: 'No training assigned yet' })).toBeInTheDocument();
  });

  it('lists the assigned activities with their status and overall progress', async () => {
    setup();

    expect(await screen.findByRole('heading', { name: 'React Basics' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'REST APIs' })).toBeInTheDocument();
    expect(screen.getByText('0 of 2 completed')).toBeInTheDocument();
    expect(within(screen.getByRole('group', { name: 'Status for React Basics' })).getByRole('radio', { name: 'In Progress' })).toBeChecked();
    expect(within(screen.getByRole('group', { name: 'Status for REST APIs' })).getByRole('radio', { name: 'Not Started' })).toBeChecked();
  });

  it('saves a new status and refreshes the list', async () => {
    const user = setup();
    const group = await screen.findByRole('group', { name: 'Status for REST APIs' });

    await user.click(within(group).getByRole('radio', { name: 'Completed' }));

    expect(assignmentsApi.updateStatus).toHaveBeenCalledWith(2, 'Completed');
    expect(await screen.findByText('Progress saved.')).toBeInTheDocument();
    expect(meApi.assignments).toHaveBeenCalledTimes(2); // loaded again after saving
  });

  it('shows an error and keeps the old status when saving fails', async () => {
    assignmentsApi.updateStatus.mockRejectedValue({
      response: { status: 403, data: { message: 'You can only update your own training.' } },
    });
    const user = setup();
    const group = await screen.findByRole('group', { name: 'Status for REST APIs' });

    await user.click(within(group).getByRole('radio', { name: 'Completed' }));

    expect(await screen.findByText('You can only update your own training.')).toBeInTheDocument();
    expect(within(group).getByRole('radio', { name: 'Not Started' })).toBeChecked();
  });

  it('shows an error state with a retry when loading fails', async () => {
    meApi.assignments.mockRejectedValueOnce({ request: {} });
    const user = setup();

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');

    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('heading', { name: 'React Basics' })).toBeInTheDocument();
  });
});
