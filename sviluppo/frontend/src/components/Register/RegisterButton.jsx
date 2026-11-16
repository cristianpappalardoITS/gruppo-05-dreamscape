import React from 'react';

const RegisterButton = ({ children, disabled, ...props }) => {
  return (
    <button 
      className={`btn-primary ${disabled ? 'btn-disabled' : ''}`} 
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
};

export default RegisterButton;