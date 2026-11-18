import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Accessible dialog: focus moves in, Tab stays inside, Esc closes, focus returns afterwards.
 * dismissable={false} disables Esc / outside click / the X (used while one-time credentials are shown).
 */
export default function Modal({ title, onClose, dismissable = true, children }) {
  const titleId = useId();
  const dialogRef = useRef(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const dialog = dialogRef.current;
    const first = dialog.querySelector('input, select, textarea') || dialog.querySelector(FOCUSABLE);
    (first || dialog).focus();
    document.body.classList.add('modal-open');

    function onKeyDown(e) {
      if (e.key === 'Escape' && dismissable) {
        e.stopPropagation();
        closeRef.current();
      }
      if (e.key === 'Tab') {
        const items = [...dialog.querySelectorAll(FOCUSABLE)].filter((el) => !el.disabled);
        if (items.length === 0) return;
        const firstEl = items[0];
        const lastEl = items[items.length - 1];
        if (e.shiftKey && document.activeElement === firstEl) {
          e.preventDefault();
          lastEl.focus();
        } else if (!e.shiftKey && document.activeElement === lastEl) {
          e.preventDefault();
          firstEl.focus();
        }
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('modal-open');
      if (previouslyFocused && previouslyFocused.focus) previouslyFocused.focus();
    };
  }, [dismissable]);

  return createPortal(
    <div
      className="modal-overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && dismissable) onClose();
      }}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={dialogRef} tabIndex={-1}>
        <div className="modal-header">
          <h2 id={titleId}>{title}</h2>
          {dismissable && (
            <button type="button" className="icon-btn" aria-label="Close dialog" onClick={onClose}>
              ×
            </button>
          )}
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>,
    document.body
  );
}
