import React, { Children } from "react";

const LoginButton = ({ children, ...props }) => {
  return (
    <button className="btn-primary" {...props}>
      {children}
    </button>
  );
};

export default LoginButton;
