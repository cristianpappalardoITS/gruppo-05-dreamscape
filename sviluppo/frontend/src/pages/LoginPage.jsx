import { useState } from 'react';
import BackToHomeLink from '../components/Login/BackToHomeLink';
import { login } from '../api.js';
import AuthArtwork from '../components/Login/AuthArtwork';
import InputField from '../components/Login/InputField';
import PasswordField from '../components/Login/PasswordField';
import LoginButton from '../components/Login/LoginButton';
import AuthFooter from '../components/Login/AuthFooter';
import '../components/Login/Login.css';

const LoginPage = () => {
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSending(true);
    setError(null);
    try {
      const user = await login(form.get('login'), form.get('password'));
      // Finché la Dashboard non esiste, il giocatore va alla Collezione
      window.location.assign(user.role === 'ADMIN' ? '/admin' : '/collezione');
    } catch (err) {
      setError(err);
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
          <h1>BENTORNATO</h1>
          <p>La tua avventura ti sta aspettando.</p>
        </div>

        <section className="login-card" aria-labelledby="login-title">
          <div className="card-subtitle-top">Bentornato, Viaggiatore</div>
          <h2 className="card-title" id="login-title">ACCEDI AL TUO MONDO</h2>
          <p className="card-description">Inserisci le tue credenziali per continuare.</p>

          <form onSubmit={handleSubmit}>
            <InputField label="Username o email" name="login" type="text" autoComplete="username" placeholder="nome@esempio.com" maxLength={255} required />
            <PasswordField label="Password" name="password" autoComplete="current-password" placeholder="Inserisci la password" maxLength={100} required />
            {error && (
              <p className="form-message" role="alert">
                {error.code === 'INVALID_CREDENTIALS' ? (
                  <>L’account potrebbe non esistere oppure la password potrebbe essere errata. Se non hai un account, <a href="/register">registrati</a>.</>
                ) : error.message}
              </p>
            )}
            <LoginButton type="submit" disabled={sending}>{sending ? 'ATTENDI...' : 'ACCEDI'}</LoginButton>
          </form>

          <AuthFooter />
        </section>

        <p className="page-bottom-text">La tua prossima storia ti aspetta</p>
      </main>
    </div>
  );
};

export default LoginPage;
