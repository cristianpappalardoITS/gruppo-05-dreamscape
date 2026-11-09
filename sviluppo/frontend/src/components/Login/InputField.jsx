import React from "react";

const InputField = ({ label, icon, ...props }) => {
  return (
    <div className="input-group">
      <label>{label}</label>
      <div className="input-wrapper">
        {icon && <span className="input-icon">{icon}</span>}
        <input {...props} />
      </div>
    </div>
  );
};

export default InputField;