// src/components/Register/TermsCheckbox.jsx
import React from 'react';

const TermsCheckbox = ({ isChecked, onChange }) => {
  return (
    <div className="terms-container">
      <label className="checkbox-container">
        <input 
          type="checkbox" 
          checked={isChecked} 
          onChange={onChange} 
        />
        <span className="terms-text">
          Accetto i <a href="/termini">Termini</a> e la <a href="/privacy">Privacy</a>
        </span>
      </label>
    </div>
  );
};

export default TermsCheckbox;