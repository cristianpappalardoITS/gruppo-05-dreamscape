import React from "react";
import InputField from "../components/Login/InputField";
import PasswordField from "../components/Login/PasswordField";
import LoginButton from "../components/Login/LoginButton";
import SocialLogin from "../components/Login/SocialLogin";
import LoginOptions from "../components/Login/LoginOptions";
import AuthFooter from "../components/Login/AuthFooter";
import "../components/Login/Login.css"; // Assicurati di aver creato questo file!

const LoginPage = () => {
  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("login effettuato");
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-header">
        <div className="header-top-text">Un mondo oltre il velo</div>
        <h1 className="brand-title">DREAMSCAPE</h1>
        <p className="brand-subtitle">
          entra nel mondo che vive oltre i sogni.
        </p>
        <div className="decorative-circle"></div>
      </div>

      {/*card principale*/}
      <div className="login-card">
        <div className="card-subtitle-top">Bentornato, Viaggiatore</div>
        <h2 className="card-title">ACCEDU AL TUO MONDO</h2>
        <p className="card-description">
          Riprendi il filo della tua avventura.
        </p>

        <form onSubmit={handleSubmit}>
          <InputField
            label="Email"
            type="email"
            placeholder="nome@esempio.com"
            icon="✉"
          />

          <PasswordField label="Password" placeholder="inserisci la password" />
         
          <LoginOptions />

          <LoginButton type="submit">ACCEDI</LoginButton>
        </form>
        <div className="divider">OPPURE CONTINUA CON</div>
        <SocialLogin/>

        <AuthFooter/>
      </div>
      <p className="page-buttom-text">La tua prossima storia ti aspetta</p>
    </div>
  );
};
