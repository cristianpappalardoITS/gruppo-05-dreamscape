import { useEffect, useState } from 'react';
import { api, errorsByField, logout } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
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

// Profilo del Dreamweaver (GET /me): dati dell'account, cambio email (PUT /me),
// cambio password (PUT /me/password), uscita (POST /auth/logout) e cancellazione (DELETE /me)
export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    api('/me').then(setProfile).catch(setLoadError);
  }, []);

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content social-content profile-content">
        <div className="collection-title-row">
          <h1>Profilo</h1>
        </div>

        {loadError && <p className="collection-message shop-error" role="alert">{loadError.message}</p>}
        {!loadError && !profile && <p className="collection-message">Caricamento...</p>}

        {profile && (
          <>
            <AccountPanel profile={profile} />
            <div className="social-columns">
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

// Dati dell'account e pulsante per uscire
function AccountPanel({ profile }) {
  const [leaving, setLeaving] = useState(false);

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
    <section className="dashboard-panel profile-account" aria-labelledby="account-title">
      <h2 id="account-title">Il tuo account</h2>
      <dl className="profile-data">
        <div>
          <dt>Username</dt>
          <dd>{profile.username}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{profile.email}</dd>
        </div>
        <div>
          <dt>Dreamweaver dal</dt>
          <dd>{new Date(profile.createdAt).toLocaleDateString('it-IT')}</dd>
        </div>
      </dl>
      <p className="collection-message">Lo username non si cambia: è il nome che vedono i tuoi amici.</p>
      <button type="button" className="social-action social-action--quiet" onClick={handleLogout} disabled={leaving}>
        {leaving ? 'Uscita...' : 'Esci'}
      </button>
    </section>
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
    <section className="dashboard-panel" aria-labelledby="email-title">
      <h2 id="email-title">Cambia email</h2>
      <form className="profile-form" onSubmit={handleSubmit}>
        <ProfileField label="Nuova email" name="email" type="email" autoComplete="email" placeholder="nome@esempio.com" maxLength={255} required error={errors.email} />
        {message && <p className="social-message" role="status">{message}</p>}
        {error && <p className="collection-message shop-error" role="alert">{errorText(error)}</p>}
        <button type="submit" className="social-action" disabled={sending}>{sending ? 'Salvataggio...' : 'Salva email'}</button>
      </form>
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
    <section className="dashboard-panel" aria-labelledby="password-title">
      <h2 id="password-title">Cambia password</h2>
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
    <section className="dashboard-panel profile-danger" aria-labelledby="delete-title">
      <h2 id="delete-title">Cancella l'account</h2>
      <p className="collection-message">
        Username ed email spariscono e l'account non si può recuperare. Le partite e gli scambi già fatti restano nello storico degli altri giocatori.
      </p>
      <form className="profile-form" onSubmit={handleSubmit}>
        <ProfileField label="Password" name="password" type="password" autoComplete="current-password" required error={errors.password} />
        {error && <p className="collection-message shop-error" role="alert">{errorText(error)}</p>}
        <button type="submit" className="social-action profile-delete" disabled={sending}>{sending ? 'Cancellazione...' : 'Cancella account'}</button>
      </form>
    </section>
  );
}
