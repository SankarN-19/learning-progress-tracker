/** Lower-case and strip accents, so 'Jose' finds 'José'. */
const fold = (text) =>
  String(text ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

/**
 * True when every word of the query appears somewhere in the given fields.
 * Case and accents are ignored; an empty query matches everything.
 */
export function matchesQuery(fields, query) {
  const words = fold(query).split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const haystack = fold(fields.join(' '));
  return words.every((word) => haystack.includes(word));
}

export function filterBy(items, query, fieldsOf) {
  return items.filter((item) => matchesQuery(fieldsOf(item), query));
}

export const plural = (count, one, many = `${one}s`) => `${count} ${count === 1 ? one : many}`;
