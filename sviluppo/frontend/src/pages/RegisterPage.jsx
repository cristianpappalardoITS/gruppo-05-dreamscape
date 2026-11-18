import { useState } from 'react';
import BackToHomeLink from '../components/Login/BackToHomeLink';
import { api, errorsByField, login } from '../api.js';
import AuthArtwork from '../components/Login/AuthArtwork';
import InputField from '../components/Login/InputField';
import PasswordField from '../components/Login/PasswordField';
import LoginButton from '../components/Login/LoginButton';
import '../components/Login/Login.css';

export default function RegisterPage() {
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const password = form.get('password');
    setErrors({});
    setMessage('');

    if (password !== form.get('confirmPassword')) {
      setErrors({ confirmPassword: 'Le password non coincidono.' });
      return;
    }

    setSending(true);
    try {
      await api('/auth/register', {
        method: 'POST',
        body: {
          username: form.get('username'),
          email: form.get('email'),
          password,
          termsAccepted: form.has('terms'),
          privacyAccepted: form.has('privacy'),
        },
      });
    } catch (err) {
      // Gli errori dei campi si vedono accanto al campo; gli altri sopra il bottone
      const fieldErrors = errorsByField(err);
      setErrors(fieldErrors);
      setMessage(Object.keys(fieldErrors).length === 0 ? err.message : '');
      setSending(false);
      return;
    }

    // Account creato: si entra subito. Finché la pagina Benvenuto non esiste si va alla Collezione.
    // Se l'accesso non riesce, l'utente lo rifà dalla pagina di accesso.
    try {
      await login(form.get('email'), password);
      window.location.assign('/collezione');
    } catch {
      window.location.assign('/login');
    }
  };

  return (
    <div className="login-page-wrapper auth-page register-page">
      <BackToHomeLink />
      <AuthArtwork />

      <main className="auth-content">
        <div className="login-header mobile-header">
          <span className="auth-mobile-logo auth-official-logo" role="img" aria-label="Logo Dreamscape" />
          <div className="header-top-text">Un mondo oltre il velo</div>
          <h1 className="brand-title">DREAMSCAPE</h1>
          <p className="brand-subtitle">entra nel mondo che vive oltre i sogni.</p>
        </div>

        <div className="desktop-intro">
          <span className="desktop-eyebrow">UNISCITI A DREAMSCAPE</span>
          <h1>CREA IL TUO ACCOUNT</h1>
          <p>Il tuo prossimo viaggio comincia da qui.</p>
        </div>

        <section className="login-card" aria-labelledby="register-title">
          <div className="card-subtitle-top">Inizia il tuo viaggio</div>
          <h2 className="card-title" id="register-title">CREA IL TUO ACCOUNT</h2>
          <p className="card-description">Registrati per entrare nel mondo Dreamscape.</p>

          <form onSubmit={handleSubmit}>
            <InputField label="Email" name="email" type="email" autoComplete="email" placeholder="nome@esempio.com" maxLength={255} required error={errors.email} />
            <InputField label="Username" name="username" type="text" icon="key" autoComplete="username" placeholder="Scegli il tuo nome" minLength={3} maxLength={30} pattern="[A-Za-z0-9_]+" required error={errors.username} />
            <PasswordField label="Password" name="password" autoComplete="new-password" placeholder="Crea una password" minLength={8} maxLength={100} required error={errors.password} />
            <PasswordField label="Conferma password" name="confirmPassword" autoComplete="new-password" placeholder="Ripeti la password" minLength={8} required error={errors.confirmPassword} />

            <div className="register-consents">
              <label className="checkbox-container"><input type="checkbox" name="terms" required /> Accetto i Termini di servizio</label>
              <label className="checkbox-container"><input type="checkbox" name="privacy" required /> Accetto l'Informativa Privacy</label>
            </div>
            {message && <p className="form-message" role="alert">{message}</p>}
            <LoginButton type="submit" disabled={sending}>{sending ? 'ATTENDI...' : 'REGISTRATI'}</LoginButton>
          </form>
          <div className="auth-footer">Hai già un account? <a href="/login">Accedi</a></div>
        </section>
        <p className="page-bottom-text">La tua prossima storia ti aspetta</p>
      </main>
    </div>
  );
}
