import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import useLoad from '../utils/useLoad';
import { activitiesApi, assignmentsApi, learnersApi } from '../api/services';
import { errorMessage } from '../api/client';
import PageSkeleton from '../components/Skeleton';
import Modal from '../components/Modal';
import ConfirmDialog from '../components/ConfirmDialog';
import { SelectField } from '../components/Fields';
import ProgressRing from '../components/ProgressRing';
import SegmentedBar from '../components/SegmentedBar';
import StatusBadge from '../components/StatusBadge';
import CredentialsBox from '../components/CredentialsBox';
import Avatar from '../components/Avatar';
import TopicChip from '../components/TopicChip';
import EmptyState from '../components/EmptyState';
import Icon from '../components/Icons';
import { useToast } from '../components/Toast';
import { formatHours } from '../utils/format';

function ResetPasswordModal({ learner, onClose }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  async function onConfirm() {
    setBusy(true);
    setError('');
    try {
      setResult(await learnersApi.resetPassword(learner.id));
      toast.success('New temporary credentials issued.');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (result) {
    return (
      <Modal title="New temporary credentials" onClose={onClose} dismissable={false}>
        <CredentialsBox credentials={result.credentials} />
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={onClose}>
            I have copied them. Done
          </button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Reset password" onClose={onClose}>
      <p>
        This signs <strong>{learner.displayName}</strong> out everywhere and replaces their password with a new
        temporary one. They must choose a new password on next login.
      </p>
      {error && (
        <p className="alert alert-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onClose}>
          Cancel
        </button>
        <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Resetting…' : 'Reset password'}
        </button>
      </div>
    </Modal>
  );
}

export default function LearnerDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { data, loading, error, reload } = useLoad(async () => {
    const [learners, assignments, activities] = await Promise.all([
      learnersApi.list(),
      learnersApi.assignments(id),
      activitiesApi.list(),
    ]);
    const learner = learners.find((l) => String(l.id) === id);
    return learner ? { learner, assignments, activities } : { missing: true };
  }, [id]);

  const [activityId, setActivityId] = useState('');
  const [assignError, setAssignError] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  async function onDelete() {
    setDeleteBusy(true);
    setDeleteError('');
    try {
      await learnersApi.remove(id);
      toast.success('Learner deleted.');
      navigate('/learners', { replace: true });
    } catch (err) {
      setDeleteError(errorMessage(err));
      setDeleteBusy(false);
    }
  }

  async function onAssign(e) {
    e.preventDefault();
    setAssignError('');
    if (!activityId) {
      setAssignError('Choose an activity to assign.');
      return;
    }
    setAssigning(true);
    try {
      await assignmentsApi.assign(Number(id), Number(activityId));
      toast.success('Activity assigned.');
      setActivityId('');
      await reload();
    } catch (err) {
      setAssignError(errorMessage(err)); // e.g. "Assignment already exists."
    } finally {
      setAssigning(false);
    }
  }

  const back = (
    <p className="back-row">
      <Link to="/learners">Back to learners</Link>
    </p>
  );

  if (loading && !data) return <PageSkeleton label="Loading learner…" />;
  if (error) {
    return (
      <>
        {back}
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      </>
    );
  }
  if (!data) return null;
  if (data.missing) {
    return (
      <>
        {back}
        <div className="panel">
          <EmptyState icon="users" title="Learner not found">
            This learner may have been removed, or the link is wrong.
          </EmptyState>
        </div>
      </>
    );
  }

  const { learner, assignments, activities } = data;
  const assignedIds = new Set(assignments.map((a) => a.activityId));
  const available = activities.filter((a) => !assignedIds.has(a.id));
  const completed = assignments.filter((a) => a.status === 'Completed').length;
  const inProgress = assignments.filter((a) => a.status === 'In Progress').length;
  const notStarted = assignments.length - completed - inProgress;

  return (
    <>
      {back}
      <div className="page-head">
        <div className="person-head">
          <Avatar name={learner.displayName} size="lg" />
          <div>
            <h1>{learner.displayName}</h1>
            <p className="muted">
              {learner.email}
              <span className="sep"> | </span>
              {learner.cohort}
            </p>
          </div>
        </div>
        <div className="head-actions">
          <button type="button" className="btn btn-secondary" onClick={() => setResetting(true)}>
            <Icon name="key" size={20} /> Reset password
          </button>
          <button type="button" className="btn btn-outline-danger" onClick={() => setDeleting(true)}>
            <Icon name="trash" size={20} /> Delete learner
          </button>
        </div>
      </div>

      <div className="detail-grid">
        <section className="panel" aria-labelledby="assigned-h">
          <h2 id="assigned-h">Assigned training</h2>
          {assignments.length === 0 ? (
            <EmptyState icon="flag" title="No training assigned yet">
              Use the form on this page to assign the first activity.
            </EmptyState>
          ) : (
            <div className="table-wrap">
              <table>
                <caption className="sr-only">Training assigned to {learner.displayName}</caption>
                <thead>
                  <tr>
                    <th scope="col">Activity</th>
                    <th scope="col">Topic</th>
                    <th scope="col">Hours</th>
                    <th scope="col">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {assignments.map((a) => (
                    <tr key={a.id}>
                      <td className="cell-strong">{a.activity.title}</td>
                      <td>
                        <TopicChip topic={a.activity.topic} />
                      </td>
                      <td>{formatHours(a.activity.plannedHours)}</td>
                      <td>
                        <StatusBadge status={a.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <div className="detail-side">
          <section className="panel" aria-labelledby="progress-h">
            <h2 id="progress-h">Progress</h2>
            <div className="ring-row">
              <ProgressRing done={completed} total={assignments.length} />
              <div>
                <p className="ring-caption">
                  {assignments.length === 0 ? 'Nothing assigned yet' : `${completed} of ${assignments.length} completed`}
                </p>
                <SegmentedBar completed={completed} inProgress={inProgress} notStarted={notStarted} showLegend={false} />
              </div>
            </div>
          </section>

          <section className="panel" aria-labelledby="assign-h">
            <h2 id="assign-h">Assign an activity</h2>
            {available.length === 0 ? (
              <p className="muted">
                {activities.length === 0
                  ? 'There are no activities to assign yet. Create one on the Activities page.'
                  : 'Every activity is already assigned to this learner.'}
              </p>
            ) : (
              <form onSubmit={onAssign} noValidate>
                <SelectField
                  id="assign-activity"
                  label="Activity"
                  value={activityId}
                  onChange={(e) => setActivityId(e.target.value)}
                  error={assignError}
                >
                  <option value="">Select an activity…</option>
                  {available.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.title} ({a.topic}, {formatHours(a.plannedHours)})
                    </option>
                  ))}
                </SelectField>
                <button type="submit" className="btn btn-primary btn-block" disabled={assigning}>
                  <Icon name="plus" size={18} /> {assigning ? 'Assigning…' : 'Assign activity'}
                </button>
              </form>
            )}
          </section>
        </div>
      </div>

      {resetting && <ResetPasswordModal learner={learner} onClose={() => setResetting(false)} />}

      {deleting && (
        <ConfirmDialog
          title="Delete learner"
          busy={deleteBusy}
          error={deleteError}
          onConfirm={onDelete}
          onCancel={() => {
            setDeleting(false);
            setDeleteError('');
          }}
        >
          <p>
            This permanently removes <strong>{learner.displayName}</strong>: their account, {assignments.length}{' '}
            assignment{assignments.length === 1 ? '' : 's'} with all progress, and any open sessions. Their email can
            be used again. This cannot be undone.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
