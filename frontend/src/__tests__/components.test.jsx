import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import StatusBadge from '../components/StatusBadge';
import ProgressBar from '../components/ProgressBar';
import SegmentedBar from '../components/SegmentedBar';

describe('StatusBadge', () => {
  it.each(['Not Started', 'In Progress', 'Completed'])('shows the status as text, not colour alone: %s', (status) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(status)).toBeInTheDocument();
  });
});

describe('ProgressBar', () => {
  it('reports progress to assistive technology', () => {
    render(<ProgressBar done={1} total={2} label="Jane progress" />);

    const bar = screen.getByRole('progressbar', { name: 'Jane progress' });
    expect(bar).toHaveAttribute('aria-valuenow', '50');
    expect(screen.getByText('1 of 2 completed')).toBeInTheDocument();
  });

  it('shows an empty message when nothing is assigned', () => {
    render(<ProgressBar done={0} total={0} />);

    expect(screen.getByText('No training assigned yet.')).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });
});

describe('SegmentedBar', () => {
  it('describes every segment in text', () => {
    render(<SegmentedBar completed={3} inProgress={2} notStarted={1} />);

    expect(screen.getByRole('img', { name: '3 completed, 2 in progress, 1 not started' })).toBeInTheDocument();
  });

  it('shows an empty message when there is nothing to show', () => {
    render(<SegmentedBar completed={0} inProgress={0} notStarted={0} />);

    expect(screen.getByText('No training assigned yet.')).toBeInTheDocument();
  });
});
