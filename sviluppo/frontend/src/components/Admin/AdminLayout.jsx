import { useEffect, useState } from 'react';
import { api, logout } from '../../api.js';
import '../../styles/home.css';
import '../Collezione/Collezione.css';
import './Admin.css';

// Le pagine del backoffice (schema.md, pagine del sito). Messaggi arriva con il form Contatti (#162)
const adminLinks = [
  { label: 'Dashboard', href: '/admin', exact: true },
  { label: 'Carte', href: '/admin/carte' },
  { label: 'Pacchetti', href: '/admin/pacchetti' },
  { label: 'Configurazione', href: '/admin/configurazione' },
  { label: 'Messaggi', href: '/admin/messaggi' },
];

const currentPath = () => window.location.pathname.replace(/\/$/, '') || '/';
const isActive = ({ href, exact }) => (exact ? currentPath() === href : currentPath().startsWith(href));

// Cornice di tutte le pagine del backoffice: intestazione con il menu dell'admin e controllo dell'accesso.
// Chi non è collegato va all'accesso (lo fa api.js), chi è un giocatore torna alla sua Dashboard.
// Il contenuto si mostra solo dopo il controllo: le API /admin rispondono comunque 403 ai giocatori.
export default function AdminLayout({ title, actions, children }) {
  const [admin, setAdmin] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/me')
      .then((me) => {
        if (me.role !== 'ADMIN') {
          window.location.replace('/dashboard');
          return;
        }
        setAdmin(me);
      })
      .catch(setError);
  }, []);

  const signOut = async () => {
    try {
      await logout();
    } finally {
      window.location.assign('/login');
    }
  };

  return (
    <div className="admin-page">
      <header className="home-header admin-header">
        <a className="home-header__brand" href="/admin" aria-label="Dreamscape, torna alla Dashboard admin">
          <span className="home-header__logo" aria-hidden="true" />
        </a>

        <nav className="home-header__navigation" aria-label="Navigazione del backoffice">
          {adminLinks.map((link) => (
            <a key={link.href} href={link.href} className={isActive(link) ? 'is-active' : undefined} aria-current={isActive(link) ? 'page' : undefined}>
              {link.label}
            </a>
          ))}
        </nav>

        <div className="admin-header__account">
          <span className="admin-badge">Backoffice</span>
          {admin && <span className="admin-header__name">{admin.username}</span>}
          <button type="button" className="admin-button" onClick={signOut}>Esci</button>
        </div>
      </header>

      <main className="admin-content">
        <div className="collection-title-row">
          <h1>{title}</h1>
          {admin && actions && <div className="title-actions">{actions}</div>}
        </div>

        {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}
        {!error && !admin && <p className="collection-message">Caricamento...</p>}
        {admin && children}
      </main>
    </div>
  );
}
