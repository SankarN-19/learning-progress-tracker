import { useState } from 'react';
import useLoad from '../utils/useLoad';
import { assignmentsApi, meApi } from '../api/services';
import { errorMessage } from '../api/client';
import PageSkeleton from '../components/Skeleton';
import ProgressRing from '../components/ProgressRing';
import SegmentedBar from '../components/SegmentedBar';
import StatusBadge from '../components/StatusBadge';
import TopicChip from '../components/TopicChip';
import EmptyState from '../components/EmptyState';
import { useToast } from '../components/Toast';
import { STATUSES, formatHours } from '../utils/format';

const keyOf = (status) => status.toLowerCase().replace(/\s+/g, '-');

/** Three native radio buttons styled as one control: arrow keys work and screen readers get a group. */
function StatusPicker({ assignment, disabled, onChange }) {
  return (
    <fieldset className="status-picker" disabled={disabled}>
      <legend className="sr-only">Status for {assignment.activity.title}</legend>
      {STATUSES.map((s) => (
        <label key={s} className={`seg seg-${keyOf(s)}`}>
          <input
            type="radio"
            name={`status-${assignment.id}`}
            value={s}
            checked={assignment.status === s}
            onChange={() => onChange(assignment, s)}
          />
          <span>{s}</span>
        </label>
      ))}
    </fieldset>
  );
}

export default function MyTrainingPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useLoad(() => meApi.assignments(), []);
  const [savingId, setSavingId] = useState(null);

  async function onChange(assignment, status) {
    setSavingId(assignment.id);
    try {
      await assignmentsApi.updateStatus(assignment.id, status);
      toast.success('Progress saved.');
      await reload();
    } catch (err) {
      // the radios are controlled by the saved status, so they snap back on failure
      toast.error(errorMessage(err));
    } finally {
      setSavingId(null);
    }
  }

  const completed = data ? data.filter((a) => a.status === 'Completed').length : 0;
  const inProgress = data ? data.filter((a) => a.status === 'In Progress').length : 0;
  const notStarted = data ? data.length - completed - inProgress : 0;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>My training</h1>
          <p className="muted">Update each activity as you go. Your coordinator sees changes straight away.</p>
        </div>
      </div>

      {loading && !data && <PageSkeleton label="Loading your training…" />}
      {error && (
        <div className="alert alert-error" role="alert">
          {error}{' '}
          <button type="button" className="link-btn" onClick={reload}>
            Try again
          </button>
        </div>
      )}

      {data && (
        <>
          <section className="panel" aria-labelledby="my-progress">
            <h2 id="my-progress">Your progress</h2>
            <div className="ring-row">
              <ProgressRing done={completed} total={data.length} />
              <div>
                <p className="ring-caption">
                  {data.length === 0 ? 'Nothing assigned yet' : `${completed} of ${data.length} completed`}
                </p>
                <SegmentedBar completed={completed} inProgress={inProgress} notStarted={notStarted} />
              </div>
            </div>
          </section>

          {data.length === 0 ? (
            <div className="panel">
              <EmptyState icon="flag" title="No training assigned yet">
                When your coordinator assigns an activity, it will appear here.
              </EmptyState>
            </div>
          ) : (
            <ul className="training-list">
              {data.map((a) => (
                <li key={a.id} className={`training-item item-${keyOf(a.status)}`}>
                  <div className="training-main">
                    <h3>{a.activity.title}</h3>
                    <p className="training-meta">
                      <TopicChip topic={a.activity.topic} />
                      <span className="muted">{formatHours(a.activity.plannedHours)} planned</span>
                    </p>
                  </div>
                  <div className="training-side">
                    <StatusBadge status={a.status} />
                    <StatusPicker assignment={a} disabled={savingId === a.id} onChange={onChange} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  );
}
