import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import useLoad from '../utils/useLoad';
import { activitiesApi, learnersApi, summaryApi } from '../api/services';
import PageSkeleton from '../components/Skeleton';
import ProgressBar from '../components/ProgressBar';
import SegmentedBar from '../components/SegmentedBar';
import Avatar from '../components/Avatar';
import TopicChip from '../components/TopicChip';
import EmptyState from '../components/EmptyState';
import Icon from '../components/Icons';
import { formatHours } from '../utils/format';

function Tile({ variant, icon, label, value }) {
  return (
    <div className={`tile tile-${variant}`}>
      <span className="tile-icon">
        <Icon name={icon} size={22} />
      </span>
      <div>
        <p className="tile-value">{value}</p>
        <p className="tile-label">{label}</p>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { data, loading, error, reload } = useLoad(async () => {
    const [summary, learners, activities] = await Promise.all([
      summaryApi.get(),
      learnersApi.list(),
      activitiesApi.list(),
    ]);
    return { summary, learners, activities };
  }, []);

  // Refresh the numbers when the coordinator comes back to this tab.
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === 'visible') reload();
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [reload]);

  const s = data?.summary;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Cohort overview</h1>
          <p className="muted">Every figure here is calculated from saved training data.</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={reload} disabled={loading}>
          <Icon name="refresh" size={18} /> {loading && data ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      {loading && !data && <PageSkeleton label="Loading dashboard…" />}
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
          <section aria-label="Cohort summary" className="tiles">
            <Tile variant="learners" icon="users" label="Learners" value={s.learners} />
            <Tile variant="assignments" icon="list" label="Assignments" value={s.assignments} />
            <Tile variant="doing" icon="clock" label="In progress" value={s.inProgress} />
            <Tile variant="done" icon="check-circle" label="Completed" value={s.completed} />
          </section>

          <section className="panel" aria-labelledby="overall-h">
            <h2 id="overall-h">Overall progress</h2>
            <SegmentedBar completed={s.completed} inProgress={s.inProgress} notStarted={s.notStarted} />
          </section>

          <div className="grid-2">
            <section className="panel" aria-labelledby="dash-learners">
              <div className="panel-head">
                <h2 id="dash-learners">Learners</h2>
                <Link to="/learners">View all</Link>
              </div>
              {data.learners.length === 0 ? (
                <EmptyState icon="users" title="No learners yet">
                  Add your first candidate to start tracking progress.
                </EmptyState>
              ) : (
                <ul className="people">
                  {data.learners.map((l) => (
                    <li key={l.id}>
                      <Avatar name={l.displayName} />
                      <div className="people-main">
                        <Link to={`/learners/${l.id}`}>{l.displayName}</Link>
                        <ProgressBar done={l.completedCount} total={l.assignedCount} label={`${l.displayName} progress`} />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="panel" aria-labelledby="dash-activities">
              <div className="panel-head">
                <h2 id="dash-activities">Activities</h2>
                <Link to="/activities">Manage</Link>
              </div>
              {data.activities.length === 0 ? (
                <EmptyState icon="activities" title="No activities yet">
                  Create an activity so you can assign it to learners.
                </EmptyState>
              ) : (
                <ul className="acts">
                  {data.activities.map((a) => (
                    <li key={a.id}>
                      <div>
                        <strong>{a.title}</strong>
                        <TopicChip topic={a.topic} />
                      </div>
                      <span className="hours">{formatHours(a.plannedHours)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </>
  );
}
