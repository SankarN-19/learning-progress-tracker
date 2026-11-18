import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { homePath } from '../auth/guards';
import Avatar from '../components/Avatar';
import Icon from '../components/Icons';
import Logo from '../components/Logo';
import SegmentedBar from '../components/SegmentedBar';

/* ---------- the interactive moment: a miniature cohort board with sample data ---------- */

const LABELS = ['Not Started', 'In Progress', 'Completed'];
const KEYS = ['not-started', 'in-progress', 'completed'];
const MARKS = ['○', '◐', '✓'];

const SEED = [
  { name: 'Jordan Lee', items: [{ t: 'React Basics', s: 1 }, { t: 'REST APIs', s: 0 }, { t: 'SQL Foundations', s: 2 }] },
  { name: 'Priya Nair', items: [{ t: 'React Basics', s: 2 }, { t: 'REST APIs', s: 1 }] },
  { name: 'Sam Okafor', items: [{ t: 'Git and Collaboration', s: 0 }] },
];

function CohortBoardDemo() {
  const [rows, setRows] = useState(SEED);
  const all = rows.flatMap((r) => r.items);
  const count = (s) => all.filter((i) => i.s === s).length;

  function cycle(rowIndex, itemIndex) {
    setRows((prev) =>
      prev.map((r, a) =>
        a !== rowIndex
          ? r
          : { ...r, items: r.items.map((it, b) => (b !== itemIndex ? it : { ...it, s: (it.s + 1) % 3 })) }
      )
    );
  }

  return (
    <div className="board-wrap">
      <div className="board-stage">
        <section className="board" aria-labelledby="board-title">
          <div className="board-head">
            <div>
              <h2 id="board-title">Frontend cohort</h2>
              <p className="board-summary" aria-live="polite">
                {count(2)} of {all.length} activities completed
              </p>
            </div>
            <button type="button" className="link-btn" onClick={() => setRows(SEED)}>
              Reset sample
            </button>
          </div>

          <SegmentedBar completed={count(2)} inProgress={count(1)} notStarted={count(0)} />

          <ul className="board-rows">
            {rows.map((r, ri) => {
              const done = r.items.filter((i) => i.s === 2).length;
              return (
                <li key={r.name}>
                  <div className="board-person">
                    <Avatar name={r.name} />
                    <div>
                      <strong>{r.name}</strong>
                      <span className="muted">
                        {' '}
                        {done} of {r.items.length} done
                      </span>
                    </div>
                  </div>
                  <ul className="chips">
                    {r.items.map((it, ii) => (
                      <li key={it.t}>
                        <button
                          type="button"
                          className={`chip chip-${KEYS[it.s]}`}
                          onClick={() => cycle(ri, ii)}
                          aria-label={`${r.name}, ${it.t}: ${LABELS[it.s]}. Press to change the status.`}
                        >
                          <span className="chip-title">{it.t}</span>
                          <span className="chip-status">
                            <span aria-hidden="true">{MARKS[it.s]}</span> {LABELS[it.s]}
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
          </ul>
          <p className="board-note">Sample data. Select any status to change it and watch the totals follow.</p>
        </section>
      </div>
    </div>
  );
}

/* ---------- page ---------- */

const STEPS = [
  ['Add a candidate', 'The coordinator enters a name, email and cohort. The system creates the account and shows a temporary password once.'],
  ['Hand over the credentials', 'Copy the email and temporary password and give them to the candidate. The password expires after 24 hours.'],
  ['First sign-in', 'The candidate signs in and is asked to choose their own password before anything else.'],
  ['Assign training', 'The coordinator picks activities from the list and assigns them. An activity can only be assigned once per learner.'],
  ['Update progress', 'Candidates mark each activity Not Started, In Progress or Completed as they work through it.'],
  ['Review the cohort', 'Dashboard totals and each learner’s progress are calculated from saved data, so they are always current.'],
];

const COORDINATOR_POINTS = [
  'Create and edit training activities with a topic and planned hours',
  'Add candidates and hand over one-time credentials',
  'Assign activities to learners, with duplicates blocked',
  'See every learner’s assignments and progress',
  'Reset a candidate’s password if they are locked out',
  'Read the cohort summary at a glance',
];

const CANDIDATE_POINTS = [
  'Sign in with the credentials your coordinator gave you',
  'Choose your own password on first sign-in',
  'See only the training assigned to you',
  'Update each activity as you go',
  'Sign out when you are done',
];

const SAFEGUARDS = [
  ['clock', 'Temporary passwords expire', 'They are random, shown once, and stop working after 24 hours.'],
  ['lock', 'Your training stays yours', 'The server checks every request, so a candidate cannot open anyone else’s assignments.'],
  ['shield', 'Resets sign people out', 'When a coordinator resets a password, old sessions stop working straight away.'],
  ['users', 'No public sign-up', 'Only a coordinator can create an account, so only invited people get in.'],
];

export default function LandingPage() {
  const { user } = useAuth();

  let ctaLabel = 'Sign in';
  let ctaTo = '/login';
  if (user) {
    ctaTo = homePath(user);
    ctaLabel = user.mustChangePassword ? 'Finish setting up' : user.role === 'COORDINATOR' ? 'Open dashboard' : 'Open my training';
  }

  return (
    <div className="land">
      <a className="skip-link" href="#main">
        Skip to content
      </a>

      <header className="land-header">
        <Link to="/" aria-label="Learning Progress Tracker home">
          <Logo />
        </Link>
        <nav aria-label="Page sections" className="land-nav">
          <a href="#how">How it works</a>
          <a href="#roles">Who it is for</a>
          <a href="#privacy">Privacy</a>
        </nav>
        <Link className="btn btn-primary" to={ctaTo}>
          {ctaLabel}
        </Link>
      </header>

      <main id="main">
        <section className="hero">
          <div className="hero-copy">
            <h1>Know where every trainee stands, without chasing updates.</h1>
            <p className="lead">
              Learning Progress Tracker lets a training coordinator assign activities to a cohort and see who has
              started, who is partway through and who has finished. Trainees sign in to see their own training and
              keep it up to date.
            </p>
            <div className="hero-actions">
              <Link className="btn btn-primary btn-lg" to={ctaTo}>
                {ctaLabel}
              </Link>
              <a className="btn btn-ghost btn-lg" href="#how">
                See how it works
              </a>
            </div>
            <p className="fineprint">
              There is no public sign-up. Your coordinator creates your account and gives you a temporary password.
            </p>
          </div>
          <CohortBoardDemo />
        </section>

        <section className="band" id="how" aria-labelledby="how-h">
          <div className="band-inner how">
            <div className="how-intro">
              <h2 id="how-h">From a new candidate to a finished cohort in six steps</h2>
              <p className="muted">
                The same flow every time, so nobody has to remember who is waiting on whom.
              </p>
            </div>
            <ol className="steps">
              {STEPS.map(([title, text]) => (
                <li key={title}>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="band" id="roles" aria-labelledby="roles-h">
          <div className="band-inner">
            <h2 id="roles-h" className="band-title">
              One tool, two views of the same training
            </h2>
            <div className="roles">
              <div className="roles-coordinator">
                <h3>For coordinators</h3>
                <ul>
                  {COORDINATOR_POINTS.map((p) => (
                    <li key={p}>
                      <Icon name="check" size={18} /> {p}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="roles-candidate">
                <h3>For candidates</h3>
                <ul>
                  {CANDIDATE_POINTS.map((p) => (
                    <li key={p}>
                      <Icon name="check" size={18} /> {p}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="band" id="privacy" aria-labelledby="privacy-h">
          <div className="band-inner">
            <h2 id="privacy-h" className="band-title">
              Built so people only see what they should
            </h2>
            <ul className="safeguards">
              {SAFEGUARDS.map(([icon, title, text]) => (
                <li key={title}>
                  <span className="safe-icon">
                    <Icon name={icon} size={22} />
                  </span>
                  <div>
                    <h3>{title}</h3>
                    <p>{text}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="cta-band" aria-labelledby="cta-h">
          <div className="band-inner cta-inner">
            <div>
              <h2 id="cta-h">Ready to see your cohort?</h2>
              <p>Sign in with the email and temporary password your coordinator gave you.</p>
            </div>
            <Link className="btn btn-accent btn-lg" to={ctaTo}>
              {ctaLabel}
            </Link>
          </div>
        </section>
      </main>

      <footer className="land-footer">
        <Logo />
        <p className="muted">A capstone project. Every name and figure shown here is fictional sample data.</p>
      </footer>
    </div>
  );
}
