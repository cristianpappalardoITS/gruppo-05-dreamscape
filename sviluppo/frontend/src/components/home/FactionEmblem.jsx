// Simboli ufficiali delle fazioni, dalla presentazione di Digital (Dreamscape.pdf):
// luna per Dreams, occhio per Nightmares, clessidra per Memories.
const labels = {
  dreams: 'Simbolo dei Dreams: luna',
  nightmares: 'Simbolo dei Nightmares: occhio',
  memories: 'Simbolo dei Memories: clessidra',
};

export default function FactionEmblem({ faction }) {
  const name = labels[faction] ? faction : 'dreams';
  return <img className="faction-emblem" src={`/artwork/simbolo-${name}.webp`} alt={labels[name]} />;
}
