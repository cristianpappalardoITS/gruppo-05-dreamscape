import { useState } from 'react';
import { api, errorsByField } from '../api.js';
import BackToHomeLink from '../components/Login/BackToHomeLink';
import AuthArtwork from '../components/Login/AuthArtwork';
import PasswordField from '../components/Login/PasswordField';
import LoginButton from '../components/Login/LoginButton';
import '../components/Login/Login.css';

// Nuova password dal link dell'email (POST /auth/password-resets): il token arriva nell'indirizzo
// (?token=...). Dopo il cambio tutti i dispositivi vengono scollegati, quindi si torna all'accesso.
const ResetPasswordPage = () => {
  const token = new URLSearchParams(window.location.search).get('token');
  // 'form' finché si sceglie la password, 'done' dopo il cambio, 'invalid' se il link non vale
  const [state, setState] = useState(token ? 'form' : 'invalid');
  const [sending, setSending] = useState(false);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const newPassword = form.get('newPassword');
    setErrors({});
    setMessage('');

    if (newPassword !== form.get('confirmPassword')) {
      setErrors({ confirmPassword: 'Le password non coincidono.' });
      return;
    }

    setSending(true);
    try {
      await api('/auth/password-resets', { method: 'POST', body: { token, newPassword } });
      setState('done');
    } catch (err) {
      if (err.code === 'INVALID_RESET_TOKEN') {
        setState('invalid');
      } else {
        const fieldErrors = errorsByField(err);
        setErrors(fieldErrors);
        setMessage(Object.keys(fieldErrors).length === 0 ? err.message : '');
      }
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="login-page-wrapper auth-page">
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
          <span className="desktop-eyebrow">AREA RISERVATA</span>
          <h1>NUOVA PASSWORD</h1>
          <p>Scegli la chiave per tornare nel tuo mondo.</p>
        </div>

        <section className="login-card" aria-labelledby="reset-title">
          <div className="card-subtitle-top">Quasi fatto, Viaggiatore</div>
          <h2 className="card-title" id="reset-title">SCEGLI LA PASSWORD</h2>

          {state === 'form' && (
            <>
              <p className="card-description">Almeno 8 caratteri. Dopo il cambio dovrai accedere di nuovo su tutti i dispositivi.</p>
              <form onSubmit={handleSubmit}>
                <PasswordField label="Nuova password" name="newPassword" autoComplete="new-password" placeholder="Crea una password" minLength={8} maxLength={100} required error={errors.newPassword} />
                <PasswordField label="Conferma password" name="confirmPassword" autoComplete="new-password" placeholder="Ripeti la password" minLength={8} maxLength={100} required error={errors.confirmPassword} />
                {message && <p className="form-message" role="alert">{message}</p>}
                <LoginButton type="submit" disabled={sending}>{sending ? 'ATTENDI...' : 'CAMBIA PASSWORD'}</LoginButton>
              </form>
            </>
          )}

          {state === 'done' && (
            <>
              <p className="form-message form-message--ok" role="status">
                Password cambiata. Ora puoi accedere con quella nuova.
              </p>
              <a className="btn-primary auth-link-button" href="/login">ACCEDI</a>
            </>
          )}

          {state === 'invalid' && (
            <>
              <p className="form-message" role="alert">
                Il link non è più valido: forse è scaduto (vale un’ora), è già stato usato o non è completo.
              </p>
              <a className="btn-primary auth-link-button" href="/password-dimenticata">CHIEDI UN NUOVO LINK</a>
            </>
          )}

          <div className="auth-footer">
            Ti sei ricordato la password? <a href="/login">Accedi</a>
          </div>
        </section>

        <p className="page-bottom-text">La tua prossima storia ti aspetta</p>
      </main>
    </div>
  );
};

export default ResetPasswordPage;
