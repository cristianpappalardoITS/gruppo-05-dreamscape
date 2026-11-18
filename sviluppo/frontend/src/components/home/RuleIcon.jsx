// Icone del regolamento nella home: tratto sottile e uniforme, colore preso dal testo
const icons = {
  // Mazzo: tre carte a ventaglio
  deck: (
    <>
      <rect x="9" y="3" width="10" height="15" rx="1.6" />
      <path d="M7.4 5.6 5.2 6.2a1.6 1.6 0 0 0-1.1 2l3.2 11.6a1.6 1.6 0 0 0 2 1.1l5.4-1.5" />
      <path d="M14 7.5v6M11 10.5h6" />
    </>
  ),
  // Punti vita: cuore
  health: (
    <path d="M12 20.5s-7.5-4.4-8.9-9.6C2.1 7.3 4.6 4.5 7.6 4.5c1.9 0 3.4 1 4.4 2.6 1-1.6 2.5-2.6 4.4-2.6 3 0 5.5 2.8 4.5 6.4-1.4 5.2-8.9 9.6-8.9 9.6Z" />
  ),
  // Partita automatica: due spade incrociate
  battle: (
    <>
      <path d="M4 4h3l9.5 9.5M4 4v3l9.5 9.5" />
      <path d="M13 18.5l5.5-5.5M15.8 15.8l3.7 3.7M18.3 20.6l2.3-2.3" />
      <path d="M20 4h-3l-4.2 4.2M20 4v3l-4.2 4.2" />
      <path d="M11 18.5 5.5 13M8.2 15.8l-3.7 3.7M5.7 20.6l-2.3-2.3" />
    </>
  ),
  // Vantaggi di fazione: tre fazioni in cerchio, ognuna forte contro la successiva
  factions: (
    <>
      <circle cx="12" cy="4.8" r="2.3" />
      <circle cx="19" cy="17" r="2.3" />
      <circle cx="5" cy="17" r="2.3" />
      <path d="M14.2 6.6l3.6 6.4M16.3 12.1l1.5.9.5-1.7" />
      <path d="M16.7 17.4H7.3M8.8 16l-1.5 1.4 1.5 1.4" />
      <path d="M6.2 15 9.8 8.6M8.3 9.2l1.5-.6.2 1.7" />
    </>
  ),
};

export default function RuleIcon({ type }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {icons[type]}
    </svg>
  );
}
