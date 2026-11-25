// Campo di un form del backoffice: etichetta sopra, suggerimento ed errore del backend sotto.
// children è il campo vero (input, select, textarea); wide lo allarga a tutta la riga del form.
export default function AdminField({ label, error, hint, wide = false, children }) {
  return (
    <label className={`admin-field${wide ? ' is-wide' : ''}`}>
      <span>{label}</span>
      {children}
      {hint && !error && <small>{hint}</small>}
      {error && <small className="field-error" role="alert">{error}</small>}
    </label>
  );
}

