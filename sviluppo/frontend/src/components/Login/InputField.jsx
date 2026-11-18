
const InputField = ({ label, icon = 'mail', error, ...props }) => {
  const fieldId = props.id || props.name;
  return (
    <div className="input-group">
      <label htmlFor={fieldId}>{label}</label>
      <div className="input-wrapper">
        {icon === 'mail' && <svg className="input-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M3 5h18a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Zm0 2v.4l9 6.6 9-6.6V7l-9 6.6L3 7Z"/></svg>}
        {icon === 'key' && <svg className="input-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M15.5 3a5.5 5.5 0 0 0-5.35 6.78L2 17.94V22h4.06l1.5-1.5V18h2.5l1.87-1.87A5.5 5.5 0 1 0 15.5 3Zm2 6a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Z"/></svg>}
        <input {...props} id={fieldId} aria-invalid={Boolean(error)} aria-describedby={error ? `${fieldId}-error` : undefined} />
      </div>
      {error && <small className="field-error" id={`${fieldId}-error`}>{error}</small>}
    </div>
  );
};

export default InputField;
