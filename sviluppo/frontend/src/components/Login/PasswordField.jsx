import React, { lazy } from "react";

const PasswordField = ({ label, ...props }) => {
  return (
    <div className="input-group">
      <label>{label}</label>
      <div className="input-wrapper">
        <span className="input-icon">🔑</span>
        <input type="password" {...props} />
      </div>
    </div>
  );
};

export default PasswordField;
