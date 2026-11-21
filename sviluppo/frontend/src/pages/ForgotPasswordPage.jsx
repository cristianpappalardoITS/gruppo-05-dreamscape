import { useState } from 'react';
import { api, errorsByField } from '../api.js';
import BackToHomeLink from '../components/Login/BackToHomeLink';
import AuthArtwork from '../components/Login/AuthArtwork';
import InputField from '../components/Login/InputField';
import LoginButton from '../components/Login/LoginButton';
import '../components/Login/Login.css';

// Password dimenticata (POST /auth/password-reset-requests): si chiede l'email e il backend manda un
// link per scegliere la password nuova. La risposta è sempre 202, anche se l'email non è registrata,
// quindi la pagina mostra sempre lo stesso messaggio (così nessuno scopre chi è iscritto).
const ForgotPasswordPage = () => {
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [fieldError, setFieldError] = useState(null);
  const [message, setMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSending(true);
    setFieldError(null);
    setMessage('');
    try {
      await api('/auth/password-reset-requests', { method: 'POST', body: { email: form.get('email') } });
      setSent(true);
    } catch (err) {
      const errors = errorsByField(err);
      setFieldError(errors.email ?? null);
      setMessage(errors.email ? '' : err.message);
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
          <h1>PASSWORD DIMENTICATA</h1>
          <p>Ti mandiamo un link per sceglierne una nuova.</p>
        </div>

        <section className="login-card" aria-labelledby="forgot-title">
          <div className="card-subtitle-top">Succede anche ai Dreamweaver</div>
          <h2 className="card-title" id="forgot-title">RECUPERA L’ACCESSO</h2>

          {sent ? (
            <>
              <p className="form-message form-message--ok" role="status">
                Se l’email è registrata, ti abbiamo mandato un link per scegliere la password nuova.
                Controlla la posta (anche lo spam): il link vale un’ora e si può usare una volta sola.
              </p>
              <a className="btn-primary auth-link-button" href="/login">TORNA ALL’ACCESSO</a>
              <button type="button" className="auth-text-button" onClick={() => setSent(false)}>
                Non è arrivato niente? Chiedi un altro link
              </button>
            </>
          ) : (
            <>
              <p className="card-description">Scrivi l’email del tuo account: ti mandiamo un link per scegliere una password nuova.</p>
              <form onSubmit={handleSubmit}>
                <InputField label="Email" name="email" type="email" autoComplete="email" placeholder="nome@esempio.com" maxLength={255} required error={fieldError} />
                {message && <p className="form-message" role="alert">{message}</p>}
                <LoginButton type="submit" disabled={sending}>{sending ? 'ATTENDI...' : 'MANDA IL LINK'}</LoginButton>
              </form>
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

export default ForgotPasswordPage;
