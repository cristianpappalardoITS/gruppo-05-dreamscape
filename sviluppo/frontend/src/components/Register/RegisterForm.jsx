import React, { useState } from 'react';
import InputField from '../Login/InputField';
import PasswordField from '../Login/PasswordField';
import RegisterButton from './RegisterButton';
import TermsCheckbox from './TermsCheckbox';
import SocialLogin from '../Login/SocialLogin';

const RegisterForm = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [error, setError] = useState('');

  const handleRegister = (e) => {
    e.preventDefault();
    setError('');

    // Validazione base
    if (!email || !password) {
      setError('Compila tutti i campi.');
      return;
    }
    if (!termsAccepted) {
      setError('Devi accettare i Termini e la Privacy.');
      return;
    }

    console.log("Dati inviati:", { email, password });
    // Qui andrà la chiamata API per la registrazione
  };

  return (
    <form onSubmit={handleRegister}>
      <InputField 
        label="Email" 
        type="email" 
        placeholder="nome@esempio.com" 
        icon="✉" 
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      
      <PasswordField 
        label="Password" 
        placeholder="Crea una password" 
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <TermsCheckbox 
        isChecked={termsAccepted} 
        onChange={(e) => setTermsAccepted(e.target.checked)} 
      />

      {error && <p style={{ color: '#ff4d4d', fontSize: '0.8rem', marginBottom: '10px' }}>{error}</p>}

      <RegisterButton type="submit" disabled={!termsAccepted}>
        Crea Account
      </RegisterButton>

      <div className="divider">OPPURE</div>
      <SocialLogin />
    </form>
  );
};

export default RegisterForm;