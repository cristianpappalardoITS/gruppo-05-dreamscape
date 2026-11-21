import { useEffect, useRef, useState } from 'react';
import Card from './Card';
import { CARD_BACK_URL } from './cardBack';
import './CardZoom.css';

// Carta ingrandita sopra la pagina (dialog). Premendo la carta, o "Gira", si vede il retro; di nuovo
// per tornare al fronte. Si chiude con Esc, con "Chiudi" o con un clic fuori dalla carta.
// - card: la carta completa; se manca si mostra fallback (es. un segnaposto con il nome)
// - from: da dove arriva ('top' = dal lato dell'avversario, con mezzo giro; altrimenti dal basso)
// - label: la riga sopra la carta (es. "In campo per anna"); note: la riga sotto (es. ATT e DIF del round)
export default function CardZoom({ card, fallback = null, from = 'bottom', label, note, onClose }) {
  const dialogRef = useRef(null);
  const [flipped, setFlipped] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  const name = card?.name ?? 'Carta';

  return (
    <dialog
      ref={dialogRef}
      className={`card-zoom card-zoom--${from}`}
      aria-label={name}
      onClose={onClose}
      // Un clic fuori dalla carta (sullo sfondo del dialog) lo chiude
      onClick={(event) => { if (event.target === event.currentTarget) dialogRef.current.close(); }}
    >
      <div className="card-zoom__body">
        {label && <span className="card-zoom__label">{label}</span>}
        <div className="card-zoom__card">
          <button
            type="button"
            className={`card-flip${flipped ? ' is-flipped' : ''}`}
            onClick={() => setFlipped(!flipped)}
            aria-pressed={flipped}
            aria-label={flipped ? `Retro di ${name}: premi per vedere il fronte` : `${name}: premi per girarla`}
          >
            <span className="card-flip__inner">
              <span className="card-flip__face card-flip__front" aria-hidden={flipped}>
                {card ? <Card card={card} size="large" /> : fallback}
              </span>
              <span className="card-flip__face card-flip__back" aria-hidden={!flipped}>
                <img src={CARD_BACK_URL} alt="" />
              </span>
            </span>
          </button>
        </div>
        {note && <p className="card-zoom__numbers">{note}</p>}
        <div className="card-zoom__actions">
          <button type="button" className="card-zoom__turn" onClick={() => setFlipped(!flipped)}>
            {flipped ? 'Mostra il fronte' : 'Gira'}
          </button>
          <button type="button" className="card-zoom__close" onClick={() => dialogRef.current.close()}>Chiudi</button>
        </div>
      </div>
    </dialog>
  );
}
