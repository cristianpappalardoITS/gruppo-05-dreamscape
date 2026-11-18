import { isLoggedIn } from '../../api.js';

// Menu per chi non è collegato: le parti della home
const publicLinks = [
  { label: 'Home', href: '/' },
  { label: 'Gioco', href: '/#gioco' },
  { label: 'Fazioni', href: '/#fazioni' },
  { label: 'Regolamento', href: '/#regolamento' },
];

// Menu per chi è collegato: le pagine del gioco
const playerLinks = [
  { label: 'Dashboard', href: '/dashboard' },
  { label: 'Collezione', href: '/collezione' },
  { label: 'Mazzi', href: '/mazzi' },
  { label: 'Negozio', href: '/negozio' },
  { label: 'Amici', href: '/amici' },
  { label: 'Scambi', href: '/scambi' },
  { label: 'Sfide', href: '/sfide' },
];

export default function HomeHeader() {
  const loggedIn = isLoggedIn();

  return (
    <header className="home-header">
      <a className="home-header__brand" href="/" aria-label="Dreamscape, torna alla home">
        <span className="home-header__logo" aria-hidden="true" />
      </a>

      <nav className="home-header__navigation" aria-label="Navigazione principale">
        {(loggedIn ? playerLinks : publicLinks).map(({ label, href }) => <a key={label} href={href}>{label}</a>)}
      </nav>

      <a className="home-header__account" href={loggedIn ? '/dashboard' : '/login'} aria-label={loggedIn ? 'La tua dashboard' : 'Accedi al tuo account'}>
        <svg viewBox="0 0 32 32" aria-hidden="true">
          <circle cx="16" cy="16" r="13" fill="none" stroke="currentColor" strokeWidth="2" />
          <circle cx="16" cy="12" r="3.5" fill="none" stroke="currentColor" strokeWidth="2" />
          <path d="M9.5 24c.6-4 3-6 6.5-6s5.9 2 6.5 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </a>
    </header>
  );
}
