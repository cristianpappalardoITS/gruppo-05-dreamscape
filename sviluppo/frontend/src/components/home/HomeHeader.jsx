import { isLoggedIn } from '../../api.js';

// Menu per chi non è collegato: le parti della home
const publicLinks = [
  { label: 'Home', href: '/' },
  { label: 'Gioco', href: '/#gioco' },
  { label: 'Fazioni', href: '/#fazioni' },
  { label: 'Regolamento', href: '/#regolamento' },
];

// Menu per chi è collegato, come nei mockup. Ogni voce è attiva anche nelle sue pagine interne
// (es. Mazzi nell'editor, Arena nelle partite). Dashboard dal logo, Negozio dal carrello,
// Profilo dall'icona; Amici si apre da Scambio e Arena.
const playerLinks = [
  { label: 'Collezione', href: '/collezione', paths: ['/collezione'] },
  { label: 'Mazzi', href: '/mazzi', paths: ['/mazzi'] },
  { label: 'Scambio', href: '/scambi', paths: ['/scambi'] },
  { label: 'Arena', href: '/arena', paths: ['/arena', '/sfide', '/partite'] },
];

const isActive = (paths) => paths.some((path) => window.location.pathname.startsWith(path));

// compact: intestazione più bassa, per le pagine che devono stare in uno schermo (partita, apertura)
export default function HomeHeader({ compact = false }) {
  const loggedIn = isLoggedIn();
  const shopActive = loggedIn && isActive(['/negozio']);
  const profileActive = loggedIn && isActive(['/profilo']);

  return (
    <header className={`home-header${compact ? ' home-header--compact' : ''}`}>
      <a className="home-header__brand" href={loggedIn ? '/dashboard' : '/'} aria-label={loggedIn ? 'Dreamscape, vai alla dashboard' : 'Dreamscape, torna alla home'}>
        <span className="home-header__logo" aria-hidden="true" />
      </a>

      <nav className="home-header__navigation" aria-label="Navigazione principale">
        {loggedIn
          ? playerLinks.map(({ label, href, paths }) => (
            <a key={label} href={href} className={isActive(paths) ? 'is-active' : undefined} aria-current={isActive(paths) ? 'page' : undefined}>
              {label}
            </a>
          ))
          : publicLinks.map(({ label, href }) => <a key={label} href={href}>{label}</a>)}
      </nav>

      <div className="home-header__icons">
        {loggedIn && (
          <a className={`home-header__icon${shopActive ? ' is-active' : ''}`} href="/negozio" aria-label="Negozio" aria-current={shopActive ? 'page' : undefined}>
            <svg viewBox="0 0 32 32" aria-hidden="true">
              <path d="M3 5h4l3.6 14.2a2 2 0 0 0 2 1.5h11.6a2 2 0 0 0 1.9-1.4L29 10H8.8" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              <circle cx="13" cy="26" r="2.3" fill="currentColor" />
              <circle cx="24" cy="26" r="2.3" fill="currentColor" />
            </svg>
          </a>
        )}

        <a
          className={`home-header__icon home-header__account${profileActive ? ' is-active' : ''}`}
          href={loggedIn ? '/profilo' : '/login'}
          aria-label={loggedIn ? 'Il tuo profilo' : 'Accedi al tuo account'}
          aria-current={profileActive ? 'page' : undefined}
        >
          <svg viewBox="0 0 32 32" aria-hidden="true">
            <circle cx="16" cy="16" r="13" fill="none" stroke="currentColor" strokeWidth="2" />
            <circle cx="16" cy="12" r="3.5" fill="none" stroke="currentColor" strokeWidth="2" />
            <path d="M9.5 24c.6-4 3-6 6.5-6s5.9 2 6.5 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </a>
      </div>
    </header>
  );
}
