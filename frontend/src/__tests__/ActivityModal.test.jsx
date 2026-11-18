import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ActivityModal } from '../pages/ActivitiesPage';
import { ToastProvider } from '../components/Toast';
import { activitiesApi } from '../api/services';

vi.mock('../api/services', () => ({
  activitiesApi: { list: vi.fn(), create: vi.fn(), update: vi.fn() },
}));

const onClose = vi.fn();
const onSaved = vi.fn();

function setup(activity = null) {
  render(
    <ToastProvider>
      <ActivityModal activity={activity} onClose={onClose} onSaved={onSaved} />
    </ToastProvider>
  );
  return userEvent.setup();
}

async function fill(user, { title = 'React Basics', topic = 'Frontend', hours = '6' } = {}) {
  if (title) await user.type(screen.getByLabelText('Title'), title);
  if (topic) await user.type(screen.getByLabelText('Topic'), topic);
  if (hours) await user.type(screen.getByLabelText('Planned hours'), hours);
}

beforeEach(() => {
  vi.clearAllMocks();
  activitiesApi.create.mockResolvedValue({ id: 1 });
  activitiesApi.update.mockResolvedValue({ id: 7 });
});

describe('ActivityModal', () => {
  it('requires title, topic and planned hours', async () => {
    const user = setup();
    await user.click(screen.getByRole('button', { name: 'Save activity' }));

    expect(await screen.findByText('Title is required.')).toBeInTheDocument();
    expect(screen.getByText('Topic is required.')).toBeInTheDocument();
    expect(screen.getByText('Planned hours is required.')).toBeInTheDocument();
    expect(activitiesApi.create).not.toHaveBeenCalled();
  });

  it('rejects zero and negative planned hours', async () => {
    const user = setup();
    await fill(user, { hours: '0' });
    await user.click(screen.getByRole('button', { name: 'Save activity' }));
    expect(await screen.findByText('Planned hours must be greater than zero.')).toBeInTheDocument();

    await user.clear(screen.getByLabelText('Planned hours'));
    await user.type(screen.getByLabelText('Planned hours'), '-2');
    await user.click(screen.getByRole('button', { name: 'Save activity' }));
    expect(screen.getByText('Planned hours must be greater than zero.')).toBeInTheDocument();
    expect(activitiesApi.create).not.toHaveBeenCalled();
  });

  it('saves a valid activity, shows a message and tells the page to refresh', async () => {
    const user = setup();
    await fill(user, { title: '  React Basics ', topic: ' Frontend ', hours: '6' });
    await user.click(screen.getByRole('button', { name: 'Save activity' }));

    expect(activitiesApi.create).toHaveBeenCalledWith({ title: 'React Basics', topic: 'Frontend', plannedHours: 6 });
    expect(await screen.findByText('Activity saved.')).toBeInTheDocument();
    expect(onSaved).toHaveBeenCalled();
  });

  it('edits an existing activity with its values prefilled', async () => {
    const user = setup({ id: 7, title: 'REST APIs', topic: 'Backend', plannedHours: 8 });

    expect(screen.getByRole('heading', { name: 'Edit activity' })).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toHaveValue('REST APIs');
    expect(screen.getByLabelText('Planned hours')).toHaveValue(8);

    await user.clear(screen.getByLabelText('Planned hours'));
    await user.type(screen.getByLabelText('Planned hours'), '9.5');
    await user.click(screen.getByRole('button', { name: 'Save activity' }));

    expect(activitiesApi.update).toHaveBeenCalledWith(7, { title: 'REST APIs', topic: 'Backend', plannedHours: 9.5 });
  });

  it('shows an error state and keeps the dialog open when saving fails', async () => {
    activitiesApi.create.mockRejectedValue({ request: {} });
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Save activity' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Save activity' })).toBeEnabled();
  });

  it('shows a server validation message on its field', async () => {
    activitiesApi.create.mockRejectedValue({
      response: { status: 400, data: { message: 'Title is required.', errors: { title: 'Title must be at most 150 characters.' } } },
    });
    const user = setup();
    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Save activity' }));

    expect(await screen.findByText('Title must be at most 150 characters.')).toBeInTheDocument();
  });

  it('closes with Escape', async () => {
    const user = setup();
    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalled();
  });
});
