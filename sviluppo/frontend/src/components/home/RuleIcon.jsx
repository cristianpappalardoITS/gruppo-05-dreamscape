export default function RuleIcon({ type }) {
  if (type === 'deck') {
    return <svg viewBox="0 0 80 70" aria-hidden="true"><rect x="17" y="11" width="32" height="44" rx="3" transform="rotate(-12 17 11)" fill="none" stroke="currentColor" strokeWidth="3" /><rect x="32" y="14" width="32" height="44" rx="3" transform="rotate(9 32 14)" fill="none" stroke="currentColor" strokeWidth="3" /><path d="M42 34l7-8 7 8-7 8z" fill="currentColor" /></svg>;
  }
  if (type === 'health') {
    return <svg viewBox="0 0 80 70" aria-hidden="true"><path d="M40 58C14 39 12 25 20 17c7-7 16-3 20 4 4-7 13-11 20-4 8 8 6 22-20 41Z" fill="currentColor" /><text x="40" y="40" textAnchor="middle" fill="#f3e7d6" fontSize="23" fontWeight="700">20</text></svg>;
  }
  if (type === 'battle') {
    return <svg viewBox="0 0 80 70" aria-hidden="true"><path d="M20 14l40 42M60 14L20 56M17 12l8 8M55 20l8-8M16 50l8 8M56 58l8-8" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" /></svg>;
  }
  return <svg viewBox="0 0 80 70" aria-hidden="true"><circle cx="40" cy="19" r="7" fill="none" stroke="currentColor" strokeWidth="3" /><circle cx="21" cy="28" r="6" fill="none" stroke="currentColor" strokeWidth="3" /><circle cx="59" cy="28" r="6" fill="none" stroke="currentColor" strokeWidth="3" /><path d="M27 56c1-12 6-19 13-19s12 7 13 19M9 54c1-9 5-15 12-15 3 0 5 1 7 3M71 54c-1-9-5-15-12-15-3 0-5 1-7 3" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" /></svg>;
}
