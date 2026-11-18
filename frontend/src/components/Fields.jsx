import { useState } from 'react';

function describedBy(id, error, hint) {
  const ids = [error ? `${id}-error` : null, hint ? `${id}-hint` : null].filter(Boolean);
  return ids.length ? ids.join(' ') : undefined;
}

function FieldMessages({ id, error, hint }) {
  return (
    <>
      {hint && (
        <p id={`${id}-hint`} className="hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      )}
    </>
  );
}

export function TextField({ id, label, error, hint, ...inputProps }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} aria-invalid={error ? 'true' : undefined} aria-describedby={describedBy(id, error, hint)} {...inputProps} />
      <FieldMessages id={id} error={error} hint={hint} />
    </div>
  );
}

export function SelectField({ id, label, error, hint, children, ...selectProps }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <select id={id} aria-invalid={error ? 'true' : undefined} aria-describedby={describedBy(id, error, hint)} {...selectProps}>
        {children}
      </select>
      <FieldMessages id={id} error={error} hint={hint} />
    </div>
  );
}

/** Password field with a show/hide toggle. */
export function PasswordInput({ id, label, error, hint, ...inputProps }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="password-wrap">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={describedBy(id, error, hint)}
          {...inputProps}
        />
        <button
          type="button"
          className="btn-toggle"
          aria-pressed={visible}
          aria-label={`${visible ? 'Hide' : 'Show'} ${label.toLowerCase()}`}
          onClick={() => setVisible((v) => !v)}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
      <FieldMessages id={id} error={error} hint={hint} />
    </div>
  );
}
