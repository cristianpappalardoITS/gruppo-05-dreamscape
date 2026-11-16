// src/pages/RegisterPage.jsx
import React, { useState } from 'react';
// Importiamo i componenti CONDIVISI dalla cartella Login
import InputField from '../components/Login/InputField';
import PasswordField from '../components/Login/PasswordField';
import LoginButton from '../components/Login/LoginButton';
import SocialLogin from '../components/Login/SocialLogin';
// Importiamo il componente SPECIFICO dalla cartella Register
import TermsCheckbox from '../components/Register/TermsCheckbox';
import '../components/Login/Login.css'; // Usiamo lo stesso CSS per tutto

const RegisterPage = () => {
  const [termsAccepted, setTermsAccepted] = useState(false);

  const handleRegister = (e) => {
    e.preventDefault();
    if (!termsAccepted) {
      alert("Devi accettare i termini e la privacy per continuare.");
      return;
    }
    console.log("Registrazione effettuata");
  };

  return (
    <div className="login-page-wrapper">
      
      <div className="login-header">
        <div className="header-top-text">Un mondo oltre il velo</div>
        <h1 className="brand-title">DREAMSCAPE</h1>
        <p className="brand-subtitle">Entra nel mondo che vive oltre i tuoi sogni.</p>
        <div className="decorative-circle moon-circle"></div>
      </div>

      <div className="login-card">
        <div className="card-subtitle-top">BENVENUTO IN DREAMSCAPE</div>
        <h2 className="card-title">CREA IL TUO ACCOUNT</h2>
        <p className="card-description">Il tuo prossimo viaggio comincia da qui.</p>

        <form onSubmit={handleRegister}>
          <InputField 
            label="Email" 
            type="email" 
            placeholder="nome@esempio.com" 
            icon="✉" 
          />
          
          <PasswordField 
            label="Password" 
            placeholder="Crea una password" 
          />

          <TermsCheckbox 
            isChecked={termsAccepted} 
            onChange={(e) => setTermsAccepted(e.target.checked)} 
          />

          <LoginButton type="submit">
            Crea Account
          </LoginButton>
        </form>

        <div className="divider">OPPURE</div>
        <SocialLogin />

        {/* Footer specifico per la registrazione */}
        <div className="auth-footer">
          Hai già un account? <a href="/login">Accedi</a>
        </div>
      </div>

      <p className="page-bottom-text">La tua prossima storia ti aspetta.</p>
    </div>
  );
};

export default RegisterPage;