import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { filterBy, matchesQuery, plural } from '../utils/search';
import ActivitiesPage from '../pages/ActivitiesPage';
import LearnersPage from '../pages/LearnersPage';
import { ToastProvider } from '../components/Toast';
import { activitiesApi, learnersApi } from '../api/services';

vi.mock('../api/services', () => ({
  activitiesApi: { list: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() },
  learnersApi: { list: vi.fn(), create: vi.fn(), remove: vi.fn() },
}));

const LEARNERS = [
  { id: 1, displayName: 'Jordan Lee', cohort: 'Frontend cohort', email: 'jordan.lee@example.test', assignedCount: 3, completedCount: 1 },
  { id: 2, displayName: 'Priya Nair', cohort: 'Frontend cohort', email: 'priya.nair@example.test', assignedCount: 2, completedCount: 1 },
  { id: 3, displayName: 'Aisha Khan', cohort: 'Backend cohort', email: 'aisha.khan@example.test', assignedCount: 3, completedCount: 2 },
];
const ACTIVITIES = [
  { id: 1, title: 'React Basics', topic: 'Frontend', plannedHours: 6 },
  { id: 2, title: 'REST APIs', topic: 'Backend', plannedHours: 8 },
  { id: 3, title: 'SQL Foundations', topic: 'Data', plannedHours: 4 },
];

beforeEach(() => {
  vi.clearAllMocks();
  learnersApi.list.mockResolvedValue(LEARNERS);
  activitiesApi.list.mockResolvedValue(ACTIVITIES);
});

describe('search helpers', () => {
  it('matches everything for an empty or blank query', () => {
    expect(matchesQuery(['Jordan Lee'], '')).toBe(true);
    expect(matchesQuery(['Jordan Lee'], '   ')).toBe(true);
  });

  it('ignores case and matches part of a word', () => {
    expect(matchesQuery(['Jordan Lee'], 'JORDAN')).toBe(true);
    expect(matchesQuery(['Jordan Lee'], 'ord')).toBe(true);
    expect(matchesQuery(['Jordan Lee'], 'priya')).toBe(false);
  });

  it('requires every word to match, in any field', () => {
    expect(matchesQuery(['Jordan Lee', 'Frontend cohort'], 'jordan frontend')).toBe(true);
    expect(matchesQuery(['Jordan Lee', 'Frontend cohort'], 'jordan backend')).toBe(false);
  });

  it('ignores accents in both directions', () => {
    expect(matchesQuery(['José Álvarez'], 'jose alvarez')).toBe(true);
    expect(matchesQuery(['Jose'], 'josé')).toBe(true);
  });

  it('tolerates missing fields', () => {
    expect(matchesQuery([null, undefined, 'x'], 'x')).toBe(true);
  });

  it('filters a list and builds counts', () => {
    expect(filterBy(LEARNERS, 'backend', (l) => [l.cohort]).map((l) => l.id)).toEqual([3]);
    expect(plural(1, 'learner')).toBe('1 learner');
    expect(plural(3, 'learner')).toBe('3 learners');
    expect(plural(1, 'activity', 'activities')).toBe('1 activity');
    expect(plural(2, 'activity', 'activities')).toBe('2 activities');
  });
});

describe('searching learners', () => {
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
  const box = () => screen.getByRole('searchbox', { name: 'Search learners' });

  it('shows everyone and the total before anything is typed', async () => {
    setup();

    expect(await screen.findByText('Jordan Lee')).toBeInTheDocument();
    expect(screen.getByText('Priya Nair')).toBeInTheDocument();
    expect(screen.getByText('3 learners')).toBeInTheDocument();
  });

  it('filters by name as the user types and reports the count', async () => {
    const user = setup();
    await screen.findByText('Jordan Lee');

    await user.type(box(), 'priya');

    expect(screen.getByText('Priya Nair')).toBeInTheDocument();
    expect(screen.queryByText('Jordan Lee')).not.toBeInTheDocument();
    expect(screen.getByText('Showing 1 of 3 learners')).toBeInTheDocument();
  });

  it('also searches email and cohort', async () => {
    const user = setup();
    await screen.findByText('Jordan Lee');

    await user.type(box(), 'backend');
    expect(screen.getByText('Aisha Khan')).toBeInTheDocument();
    expect(screen.queryByText('Jordan Lee')).not.toBeInTheDocument();

    await user.clear(box());
    await user.type(box(), 'jordan.lee@');
    expect(screen.getByText('Jordan Lee')).toBeInTheDocument();
    expect(screen.queryByText('Aisha Khan')).not.toBeInTheDocument();
  });

  it('needs every word to match', async () => {
    const user = setup();
    await screen.findByText('Jordan Lee');

    await user.type(box(), 'lee frontend');
    expect(screen.getByText('Jordan Lee')).toBeInTheDocument();
    expect(screen.queryByText('Priya Nair')).not.toBeInTheDocument();

    await user.clear(box());
    await user.type(box(), 'lee backend');
    expect(screen.getByRole('heading', { name: 'No learners match your search' })).toBeInTheDocument();
  });

  it('shows a no-match state with a way back', async () => {
    const user = setup();
    await screen.findByText('Jordan Lee');

    await user.type(box(), 'zzz');
    expect(screen.getByRole('heading', { name: 'No learners match your search' })).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear search' }));
    expect(screen.getByText('Jordan Lee')).toBeInTheDocument();
    expect(box()).toHaveValue('');
  });

  it('clears with the x button and with Escape', async () => {
    const user = setup();
    await screen.findByText('Jordan Lee');

    await user.type(box(), 'priya');
    await user.click(screen.getByRole('button', { name: 'Clear search text' }));
    expect(box()).toHaveValue('');
    expect(screen.getByText('Jordan Lee')).toBeInTheDocument();

    await user.type(box(), 'priya');
    await user.keyboard('{Escape}');
    expect(box()).toHaveValue('');
  });
});

describe('searching activities', () => {
  function setup() {
    render(
      <ToastProvider>
        <ActivitiesPage />
      </ToastProvider>
    );
    return userEvent.setup();
  }
  const box = () => screen.getByRole('searchbox', { name: 'Search activities' });

  it('filters by title and by topic', async () => {
    const user = setup();
    await screen.findByText('React Basics');
    expect(screen.getByText('3 activities')).toBeInTheDocument();

    await user.type(box(), 'backend'); // a topic
    expect(screen.getByText('REST APIs')).toBeInTheDocument();
    expect(screen.queryByText('React Basics')).not.toBeInTheDocument();
    expect(screen.getByText('Showing 1 of 3 activities')).toBeInTheDocument();

    await user.clear(box());
    await user.type(box(), 'sql'); // a title
    expect(screen.getByText('SQL Foundations')).toBeInTheDocument();
    expect(screen.queryByText('REST APIs')).not.toBeInTheDocument();
  });

  it('shows a no-match state that can be cleared', async () => {
    const user = setup();
    await screen.findByText('React Basics');

    await user.type(box(), 'zzz');
    expect(screen.getByRole('heading', { name: 'No activities match your search' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear search' }));
    expect(screen.getByText('React Basics')).toBeInTheDocument();
  });
});
