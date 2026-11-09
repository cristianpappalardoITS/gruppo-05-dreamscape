import React from 'react';

const LoginOptions=()=>{
    return(
        <div className='login-options'>
            <label className='checkbox-container'>
                <input type='checkbox'/>
                Ricordami
            </label>
          <a href='#' className='forgot-password'>Password dimenticata?</a>
        </div>
    );
};

export default LoginOptions;