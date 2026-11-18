import { useState } from 'react';
import { Link } from 'react-router-dom';
import useLoad from '../utils/useLoad';
import { learnersApi } from '../api/services';
import { errorMessage, fieldErrors } from '../api/client';
import PageSkeleton from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
import Avatar from '../components/Avatar';
import ConfirmDialog from '../components/ConfirmDialog';
import Icon from '../components/Icons';
import Modal from '../components/Modal';
import { TextField } from '../components/Fields';
import ProgressBar from '../components/ProgressBar';
import CredentialsBox from '../components/CredentialsBox';
import { useToast } from '../components/Toast';

export function AddCandidateModal({ onClose, onCreated }) {
  const toast = useToast();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [cohort, setCohort] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState(null);

  async function onSubmit(e) {
    e.preventDefault();
    const found = {};
    if (!name.trim()) found.name = 'Name is required.';
    if (!email.trim()) found.email = 'Email is required.';
    else if (!/^\S+@\S+\.\S+$/.test(email.trim())) found.email = 'Enter a valid email address.';
    if (!cohort.trim()) found.cohort = 'Cohort is required.';
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length > 0) return;

    setBusy(true);
    try {
      const result = await learnersApi.create({ name: name.trim(), email: email.trim(), cohort: cohort.trim() });
      setCreated(result);
      toast.success('Candidate added.');
      onCreated(); // refresh the list behind the dialog
    } catch (err) {
      const fe = fieldErrors(err);
      setErrors(fe); // a 409 puts the duplicate-email message on the email field
      setFormError(Object.keys(fe).length ? '' : errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (created) {
    return (
      <Modal title="Candidate added" onClose={onClose} dismissable={false}>
        <p>
          <strong>{created.learner.displayName}</strong> can now sign in. They will be asked to choose a new password
          on first login.
        </p>
        <CredentialsBox credentials={created.credentials} />
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={onClose}>
            I have copied them. Done
          </button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Add candidate" onClose={onClose}>
      {formError && (
        <p className="alert alert-error" role="alert">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <TextField id="cand-name" label="Full name" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} maxLength={100} />
        <TextField
          id="cand-email"
          label="Email"
          type="email"
          autoComplete="off"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <TextField id="cand-cohort" label="Cohort" value={cohort} onChange={(e) => setCohort(e.target.value)} error={errors.cohort} maxLength={100} />
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Adding…' : 'Add candidate'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function LearnersPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useLoad(() => learnersApi.list(), []);
  const [adding, setAdding] = useState(false);
  const [deleting, setDeleting] = useState(null); // null | learner
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  function closeDelete() {
    setDeleting(null);
    setDeleteError('');
  }

  async function confirmDelete() {
    setDeleteBusy(true);
    setDeleteError('');
    try {
      await learnersApi.remove(deleting.id);
      toast.success('Learner deleted.');
      setDeleting(null);
      reload();
    } catch (err) {
      setDeleteError(errorMessage(err));
    } finally {
      setDeleteBusy(false);
    }
  }

  const addButton = (
    <button type="button" className="btn btn-primary" onClick={() => setAdding(true)}>
      <Icon name="plus" size={18} /> Add candidate
    </button>
  );

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Learners</h1>
          <p className="muted">Everyone in your cohorts and how far along they are.</p>
        </div>
        {addButton}
      </div>

      {loading && !data && <PageSkeleton label="Loading learners…" />}
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
          <EmptyState icon="users" title="No learners yet" action={addButton}>
            Add a candidate to create their account and get one-time sign-in credentials.
          </EmptyState>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="panel panel-flush table-wrap">
          <table>
            <caption className="sr-only">Learners and their progress</caption>
            <thead>
              <tr>
                <th scope="col">Learner</th>
                <th scope="col">Cohort</th>
                <th scope="col">Progress</th>
                <th scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {data.map((l) => (
                <tr key={l.id}>
                  <td>
                    <div className="person-cell">
                      <Avatar name={l.displayName} />
                      <div>
                        <span className="cell-strong">{l.displayName}</span>
                        <span className="cell-sub">{l.email}</span>
                      </div>
                    </div>
                  </td>
                  <td>{l.cohort}</td>
                  <td>
                    <ProgressBar done={l.completedCount} total={l.assignedCount} label={`${l.displayName} progress`} />
                  </td>
                  <td className="actions">
                    <Link className="btn btn-secondary btn-sm" to={`/learners/${l.id}`} aria-label={`View ${l.displayName}`}>
                      View
                    </Link>
                    <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => setDeleting(l)} aria-label={`Delete ${l.displayName}`}>
                      <Icon name="trash" size={16} /> Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {adding && <AddCandidateModal onClose={() => setAdding(false)} onCreated={reload} />}

      {deleting && (
        <ConfirmDialog title="Delete learner" busy={deleteBusy} error={deleteError} onConfirm={confirmDelete} onCancel={closeDelete}>
          <p>
            This permanently removes <strong>{deleting.displayName}</strong>: their account, {deleting.assignedCount}{' '}
            assignment{deleting.assignedCount === 1 ? '' : 's'} with all progress, and any open sessions. Their email
            can be used again. This cannot be undone.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
