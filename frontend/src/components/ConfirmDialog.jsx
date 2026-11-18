import Modal from './Modal';

/** Asks before something destructive. Focus starts on a safe control, never on the delete button. */
export default function ConfirmDialog({ title, children, confirmLabel = 'Delete', busy = false, error = '', onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      {children}
      {error && (
        <p className="alert alert-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Deleting…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
