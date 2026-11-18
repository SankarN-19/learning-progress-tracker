import { useState } from 'react';
import { formatDateTime } from '../utils/format';
import Icon from './Icons';

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch {
      ok = false;
    }
    document.body.removeChild(area);
    return ok;
  }
}

/** Shows one-time credentials with a copy button and a "shown only once" warning. */
export default function CredentialsBox({ credentials }) {
  const [copied, setCopied] = useState(false);
  const [failed, setFailed] = useState(false);

  async function onCopy() {
    const text =
      `Email: ${credentials.username}\n` +
      `Temporary password: ${credentials.temporaryPassword}\n` +
      `Expires: ${formatDateTime(credentials.expiresAt)}`;
    const ok = await copyText(text);
    setCopied(ok);
    setFailed(!ok);
    setTimeout(() => {
      setCopied(false);
      setFailed(false);
    }, 3000);
  }

  return (
    <div className="credentials">
      <p className="warning" role="alert">
        <Icon name="clock" size={20} />
        <span>
          <strong>Shown only once.</strong> Copy these credentials now and give them to the candidate privately.
          They cannot be viewed again and expire on {formatDateTime(credentials.expiresAt)}.
        </span>
      </p>
      <dl className="key-card">
        <div>
          <dt>Email (username)</dt>
          <dd>{credentials.username}</dd>
        </div>
        <div>
          <dt>Temporary password</dt>
          <dd>
            <code className="key-secret">{credentials.temporaryPassword}</code>
          </dd>
        </div>
      </dl>
      <div className="copy-row">
        <button type="button" className="btn btn-secondary" onClick={onCopy}>
          <Icon name={copied ? 'check' : 'copy'} size={18} /> {copied ? 'Copied' : 'Copy credentials'}
        </button>
        <span className="copy-status" role="status">
          {failed && 'Could not copy. Select the text and copy it manually.'}
        </span>
      </div>
    </div>
  );
}
