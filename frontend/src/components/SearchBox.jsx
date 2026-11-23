import Icon from './Icons';

/** A labelled search field. Esc or the x button clears it. */
export default function SearchBox({ id, label, value, onChange, placeholder }) {
  return (
    <div className="searchbox" role="search">
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Icon name="search" size={18} />
      <input
        id={id}
        type="search"
        value={value}
        placeholder={placeholder}
        autoComplete="off"
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && value) {
            e.preventDefault();
            onChange('');
          }
        }}
      />
      {value && (
        <button type="button" className="icon-btn" aria-label="Clear search text" onClick={() => onChange('')}>
          ×
        </button>
      )}
    </div>
  );
}
