export default function FactionEmblem({ faction }) {
  if (faction === 'memories') {
    return (
      <svg viewBox="0 0 100 100" role="img" aria-label="Simbolo dei Memories">
        <path d="M38 16h24M38 84h24M42 18c0 17 17 20 17 32S42 65 42 82M58 18c0 17-17 20-17 32S58 65 58 82" fill="none" stroke="currentColor" strokeWidth="3" />
        <path d="M50 44l7 6-7 6-7-6z" fill="currentColor" />
        <path d="M50 8l3 6 7 2-7 2-3 6-3-6-7-2 7-2z" fill="currentColor" />
      </svg>
    );
  }

  if (faction === 'dreams') {
    return (
      <svg viewBox="0 0 100 100" role="img" aria-label="Simbolo dei Dreams">
        <path d="M63 17A34 34 0 1 0 63 83 29 29 0 1 1 63 17Z" fill="currentColor" />
        <path d="M61 31l3 10 10 3-10 3-3 10-3-10-10-3 10-3zM75 57l2 6 6 2-6 2-2 6-2-6-6-2 6-2z" fill="currentColor" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 100 100" role="img" aria-label="Simbolo dei Nightmares">
      <path d="M9 50C24 34 38 27 50 27s26 7 41 23C76 66 62 73 50 73S24 66 9 50Z" fill="none" stroke="currentColor" strokeWidth="3" />
      <circle cx="50" cy="50" r="13" fill="none" stroke="currentColor" strokeWidth="3" />
      <path d="M50 17l4 17 16 16-16 16-4 17-4-17-16-16 16-16z" fill="none" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
}
