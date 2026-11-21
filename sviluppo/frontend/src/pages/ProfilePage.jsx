import { useEffect, useState } from 'react';
import { api, errorsByField, logout } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import FactionEmblem from '../components/home/FactionEmblem';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Scambi/Scambi.css';
import '../components/Negozio/Negozio.css';
import '../components/Profilo/Profilo.css';

// Messaggio per gli account creati con Google, che non hanno una password (PASSWORD_NOT_SET)
const PASSWORD_NOT_SET_TEXT = 'Il tuo account è collegato a Google e non ha ancora una password: sceglila prima con "Password dimenticata" nella pagina di accesso.';

// Messaggio di un errore: per PASSWORD_NOT_SET quello della pagina, per gli altri quello del backend
const errorText = (error) => (error.code === 'PASSWORD_NOT_SET' ? PASSWORD_NOT_SET_TEXT : error.message);

// Campo di un form del profilo con l'errore sotto
function ProfileField({ label, error, ...props }) {
  const fieldId = `profile-${props.name}`;
  return (
    <label className="deck-name profile-field" htmlFor={fieldId}>
      <span>{label}</span>
      <input {...props} id={fieldId} aria-invalid={Boolean(error)} aria-describedby={error ? `${fieldId}-error` : undefined} />
      {error && <small className="field-error" id={`${fieldId}-error`}>{error}</small>}
    </label>
  );
}

// Fazioni: nome e simbolo per la testata (la fazione è quella scelta nel Benvenuto)
const factionNames = { DREAMS: 'Dreams', NIGHTMARES: 'Nightmares', MEMORIES: 'Memories' };

// Migliaia sempre con il punto (toLocaleString in italiano non lo mette sotto 10.000)
const formatNumber = (value) => String(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

// Profilo del Dreamweaver (GET /me): testata con fazione e lega, i suoi numeri (crediti, punteggio,
// partite, carte, amici), cambio email (PUT /me) e password (PUT /me/password), uscita
// (POST /auth/logout) e cancellazione (DELETE /me)
export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loadError, setLoadError] = useState(null);
  // Dati della testata e dei numeri: se uno non arriva, la sua parte mostra "–"
  const [summary, setSummary] = useState(null);
  const [faction, setFaction] = useState(null);
  const [stats, setStats] = useState({});

  useEffect(() => {
    api('/me').then(setProfile).catch(setLoadError);
    api('/me/summary').then(setSummary).catch(() => {});
    api('/me/welcome').then((welcome) => setFaction(welcome.starterFaction)).catch(() => {});
    api('/me/matches?page=0&size=1').then((data) => setStats((current) => ({ ...current, matches: data.totalItems }))).catch(() => {});
    api('/friends').then((friends) => setStats((current) => ({ ...current, friends: friends.length }))).catch(() => {});
    // /me/cards è a pagine: basta il totale
    api('/me/cards?page=0&size=1').then((data) => setStats((current) => ({ ...current, cards: data.totalItems }))).catch(() => {});
  }, []);

  return (
    <div className="decks-page profile-page">
      <HomeHeader />

      <main className="profile-content">
        {loadError && <p className="collection-message shop-error" role="alert">{loadError.message}</p>}
        {!loadError && !profile && <p className="collection-message">Caricamento...</p>}

        {profile && (
          <>
            <ProfileHero profile={profile} summary={summary} faction={faction} />
            <ProfileStats summary={summary} stats={stats} />

            <h2 className="profile-section-title">Impostazioni dell'account</h2>
            <div className="profile-settings">
              <EmailForm profile={profile} onSaved={setProfile} />
              <PasswordForm />
            </div>

            <DeleteAccountForm />
          </>
        )}
      </main>
    </div>
  );
}

// Testata: simbolo della fazione, username, da quando gioca, lega e punteggio, pulsante Esci
function ProfileHero({ profile, summary, faction }) {
  const [leaving, setLeaving] = useState(false);
  const factionCode = faction ? faction.toLowerCase() : null;
  const since = new Date(profile.createdAt).toLocaleDateString('it-IT', { day: 'numeric', month: 'long', year: 'numeric' });

  const handleLogout = async () => {
    setLeaving(true);
    try {
      await logout();
    } finally {
      // Anche se il backend non risponde il token in memoria è già dimenticato
      window.location.assign('/');
    }
  };

  return (
    <section className={`profile-hero${factionCode ? ` profile-hero--${factionCode}` : ''}`} aria-labelledby="profile-title">
      <div className="profile-avatar">
        {factionCode
          ? <FactionEmblem faction={factionCode} />
          : <span aria-hidden="true">{profile.username.charAt(0).toUpperCase()}</span>}
      </div>

      <div className="profile-hero__text">
        <span className="profile-hero__eyebrow">
          {faction ? `Dreamweaver dei ${factionNames[faction] ?? faction}` : 'Dreamweaver'}
        </span>
        <h1 id="profile-title">{profile.username}</h1>
        <p>Nel mondo dei sogni dal {since}</p>
        {summary && (
          <a className="profile-league" href="/arena">
            <strong>Lega {summary.league.name}</strong>
            <span>{formatNumber(summary.rating)} punti</span>
          </a>
        )}
      </div>

      <button type="button" className="profile-logout" onClick={handleLogout} disabled={leaving}>
        {leaving ? 'Uscita...' : 'Esci'}
      </button>
    </section>
  );
}

// I numeri del Dreamweaver, ognuno porta alla sua pagina
function ProfileStats({ summary, stats }) {
  const show = (value) => (value === undefined || value === null ? '–' : formatNumber(value));
  const items = [
    { label: 'Dream Credits', value: show(summary?.credits), note: 'Movimenti', href: '/storico?scheda=crediti', coin: true },
    { label: 'Punteggio', value: show(summary?.rating), note: summary ? `Lega ${summary.league.name}` : 'Arena', href: '/arena' },
    { label: 'Partite giocate', value: show(stats.matches), note: 'Storico', href: '/storico' },
    { label: 'Carte diverse', value: show(stats.cards), note: 'Collezione', href: '/collezione' },
    { label: 'Amici', value: show(stats.friends), note: 'Amici', href: '/amici' },
  ];

  return (
    <ul className="profile-stats" aria-label="I tuoi numeri">
      {items.map((item) => (
        <li key={item.label}>
          <a href={item.href} className="profile-stat">
            <span className="profile-stat__label">{item.label}</span>
            <strong className="profile-stat__value">
              {item.coin && <span className="dc-coin" aria-hidden="true">C</span>}
              {item.value}
            </strong>
            <span className="profile-stat__note">{item.note}</span>
          </a>
        </li>
      ))}
    </ul>
  );
}

// Cambio email: PUT /me con { email }
function EmailForm({ profile, onSaved }) {
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const email = new FormData(event.currentTarget).get('email').trim();
    setErrors({});
    setError(null);
    setMessage(null);

    if (email.toLowerCase() === profile.email.toLowerCase()) {
      setErrors({ email: 'È già la tua email.' });
      return;
    }

    setSending(true);
    try {
      const updated = await api('/me', { method: 'PUT', body: { email } });
      onSaved(updated);
      setMessage('Email aggiornata.');
      event.target.reset();
    } catch (err) {
      const fieldErrors = errorsByField(err);
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).length === 0) setError(err);
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="profile-card" aria-labelledby="email-title">
      <h3 id="email-title">Email</h3>
      <p className="profile-card__current">
        <span>Email attuale</span>
        <strong>{profile.email}</strong>
      </p>
      <form className="profile-form" onSubmit={handleSubmit}>
        <ProfileField label="Nuova email" name="email" type="email" autoComplete="email" placeholder="nome@esempio.com" maxLength={255} required error={errors.email} />
        {message && <p className="social-message" role="status">{message}</p>}
        {error && <p className="collection-message shop-error" role="alert">{errorText(error)}</p>}
        <button type="submit" className="social-action" disabled={sending}>{sending ? 'Salvataggio...' : 'Salva email'}</button>
      </form>
      <p className="profile-card__note">Lo username non si cambia: è il nome che vedono i tuoi amici.</p>
    </section>
  );
}

// Cambio password: PUT /me/password con { currentPassword, newPassword }.
// Gli altri dispositivi vengono scollegati, questo resta collegato.
function PasswordForm() {
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const newPassword = form.get('newPassword');
    setErrors({});
    setError(null);
    setMessage(null);

    if (newPassword !== form.get('confirmPassword')) {
      setErrors({ confirmPassword: 'Le password non coincidono.' });
      return;
    }

    setSending(true);
    try {
      await api('/me/password', { method: 'PUT', body: { currentPassword: form.get('currentPassword'), newPassword } });
      setMessage('Password cambiata. Gli altri dispositivi sono stati scollegati.');
      event.target.reset();
    } catch (err) {
      // WRONG_PASSWORD non ha fieldErrors: si mostra sotto la password attuale
      const fieldErrors = err.code === 'WRONG_PASSWORD' ? { currentPassword: err.message } : errorsByField(err);
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).length === 0) setError(err);
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="profile-card" aria-labelledby="password-title">
      <h3 id="password-title">Password</h3>
      <p className="profile-card__note profile-card__note--top">Cambiandola, gli altri dispositivi vengono scollegati.</p>
      <form className="profile-form" onSubmit={handleSubmit}>
        <ProfileField label="Password attuale" name="currentPassword" type="password" autoComplete="current-password" required error={errors.currentPassword} />
        <ProfileField label="Nuova password" name="newPassword" type="password" autoComplete="new-password" minLength={8} maxLength={100} required error={errors.newPassword} />
        <ProfileField label="Ripeti la nuova password" name="confirmPassword" type="password" autoComplete="new-password" minLength={8} maxLength={100} required error={errors.confirmPassword} />
        {message && <p className="social-message" role="status">{message}</p>}
        {error && <p className="collection-message shop-error" role="alert">{errorText(error)}</p>}
        <button type="submit" className="social-action" disabled={sending}>{sending ? 'Salvataggio...' : 'Cambia password'}</button>
      </form>
    </section>
  );
}

// Cancellazione dell'account: DELETE /me con { password }, dopo una conferma.
// L'account diventa anonimo e non si recupera; il backend cancella il cookie.
function DeleteAccountForm() {
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const password = new FormData(event.currentTarget).get('password');
    setErrors({});
    setError(null);

    if (!window.confirm('Vuoi davvero cancellare il tuo account? Perdi carte, mazzi, crediti e amici, e non si può tornare indietro.')) return;

    setSending(true);
    try {
      await api('/me', { method: 'DELETE', body: { password } });
      window.location.assign('/');
    } catch (err) {
      const fieldErrors = err.code === 'WRONG_PASSWORD' ? { password: err.message } : errorsByField(err);
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).length === 0) setError(err);
      setSending(false);
    }
  };

  return (
    <details className="profile-danger">
      <summary>Cancella l'account</summary>
      <p className="profile-card__note">
        Username ed email spariscono e l'account non si può recuperare. Le partite e gli scambi già fatti restano nello storico degli altri giocatori.
      </p>
      <form className="profile-form" onSubmit={handleSubmit}>
        <ProfileField label="Password" name="password" type="password" autoComplete="current-password" required error={errors.password} />
        {error && <p className="collection-message shop-error" role="alert">{errorText(error)}</p>}
        <button type="submit" className="social-action profile-delete" disabled={sending}>{sending ? 'Cancellazione...' : 'Cancella account'}</button>
      </form>
    </details>
  );
}
