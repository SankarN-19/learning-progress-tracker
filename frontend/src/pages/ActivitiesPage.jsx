import { useState } from 'react';
import useLoad from '../utils/useLoad';
import { activitiesApi } from '../api/services';
import { errorMessage, fieldErrors } from '../api/client';
import PageSkeleton from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
import TopicChip from '../components/TopicChip';
import ConfirmDialog from '../components/ConfirmDialog';
import Icon from '../components/Icons';
import Modal from '../components/Modal';
import { TextField } from '../components/Fields';
import { useToast } from '../components/Toast';
import { formatHours } from '../utils/format';

export function ActivityModal({ activity, onClose, onSaved }) {
  const toast = useToast();
  const [title, setTitle] = useState(activity?.title ?? '');
  const [topic, setTopic] = useState(activity?.topic ?? '');
  const [hours, setHours] = useState(activity ? String(activity.plannedHours) : '');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    const found = {};
    if (!title.trim()) found.title = 'Title is required.';
    if (!topic.trim()) found.topic = 'Topic is required.';
    if (hours.trim() === '') found.plannedHours = 'Planned hours is required.';
    else if (!(Number(hours) > 0)) found.plannedHours = 'Planned hours must be greater than zero.';
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length > 0) return;

    const payload = { title: title.trim(), topic: topic.trim(), plannedHours: Number(hours) };
    setBusy(true);
    try {
      if (activity) await activitiesApi.update(activity.id, payload);
      else await activitiesApi.create(payload);
      toast.success('Activity saved.');
      onSaved();
    } catch (err) {
      const fe = fieldErrors(err);
      setErrors(fe);
      setFormError(Object.keys(fe).length ? '' : errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <Modal title={activity ? 'Edit activity' : 'Add activity'} onClose={onClose}>
      {formError && (
        <p className="alert alert-error" role="alert">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <TextField id="act-title" label="Title" value={title} onChange={(e) => setTitle(e.target.value)} error={errors.title} maxLength={150} />
        <TextField id="act-topic" label="Topic" value={topic} onChange={(e) => setTopic(e.target.value)} error={errors.topic} maxLength={100} />
        <TextField
          id="act-hours"
          label="Planned hours"
          type="number"
          inputMode="decimal"
          min="0"
          step="0.5"
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          error={errors.plannedHours}
        />
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : 'Save activity'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function ActivitiesPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useLoad(() => activitiesApi.list(), []);
  const [editing, setEditing] = useState(null); // null | 'new' | activity
  const [deleting, setDeleting] = useState(null); // null | activity
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  function onSaved() {
    setEditing(null);
    reload();
  }

  function closeDelete() {
    setDeleting(null);
    setDeleteError('');
  }

  async function confirmDelete() {
    setDeleteBusy(true);
    setDeleteError('');
    try {
      await activitiesApi.remove(deleting.id);
      toast.success('Activity deleted.');
      setDeleting(null);
      reload();
    } catch (err) {
      setDeleteError(errorMessage(err));
    } finally {
      setDeleteBusy(false);
    }
  }

  const addButton = (
    <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}>
      <Icon name="plus" size={18} /> Add activity
    </button>
  );

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Activities</h1>
          <p className="muted">The training you can assign to learners.</p>
        </div>
        {addButton}
      </div>

      {loading && !data && <PageSkeleton label="Loading activities…" />}
      {error && (
        <div className="alert alert-error" role="alert">
          {error}{' '}
          <button type="button" className="link-btn" onClick={reload}>
            Try again
          </button>
        </div>
      )}

      {data && data.length === 0 && (
        <div className="panel">
          <EmptyState icon="activities" title="No activities yet" action={addButton}>
            Add an activity with a topic and planned hours, then assign it to your learners.
          </EmptyState>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="panel panel-flush table-wrap">
          <table>
            <caption className="sr-only">Training activities</caption>
            <thead>
              <tr>
                <th scope="col">Title</th>
                <th scope="col">Topic</th>
                <th scope="col">Planned hours</th>
                <th scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {data.map((a) => (
                <tr key={a.id}>
                  <td className="cell-strong">{a.title}</td>
                  <td>
                    <TopicChip topic={a.topic} />
                  </td>
                  <td>{formatHours(a.plannedHours)}</td>
                  <td className="actions">
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing(a)} aria-label={`Edit ${a.title}`}>
                      <Icon name="edit" size={16} /> Edit
                    </button>
                    <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => setDeleting(a)} aria-label={`Delete ${a.title}`}>
                      <Icon name="trash" size={16} /> Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <ActivityModal
          activity={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={onSaved}
        />
      )}

      {deleting && (
        <ConfirmDialog title="Delete activity" busy={deleteBusy} error={deleteError} onConfirm={confirmDelete} onCancel={closeDelete}>
          <p>
            Delete <strong>{deleting.title}</strong>? It will also be removed from every learner it is assigned to,
            together with their progress on it. This cannot be undone.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
