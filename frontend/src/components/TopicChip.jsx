function hash(text) {
  return [...text].reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
}

export default function TopicChip({ topic }) {
  return <span className={`topic tone-${hash(topic) % 5}`}>{topic}</span>;
}
