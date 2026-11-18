import { useLayoutEffect, useRef } from 'react';
import { cardZones, frameUrl, zoneStyle } from './cardZones';
import './Card.css';

// Rimpicciolisce il testo di un elemento finché non sta nella sua zona: scrive il fattore
// nella variabile CSS --fit, che moltiplica la dimensione del testo (in cqw, quindi vale per
// qualsiasi grandezza della carta). Il nome si controlla in larghezza, il testo in altezza.
function useFitText(ref, text, { minFit, direction }) {
  useLayoutEffect(() => {
    const element = ref.current;
    if (!element) return undefined;
    let cancelled = false;

    const fit = () => {
      if (cancelled) return;
      const overflows = () => (direction === 'width'
        ? element.scrollWidth > element.clientWidth + 1
        : element.scrollHeight > element.clientHeight + 1);
      let value = 1;
      element.style.setProperty('--fit', value);
      while (overflows() && value > minFit) {
        value = Math.max(minFit, value - 0.04);
        element.style.setProperty('--fit', value);
      }
    };

    fit();
    // Con il font vero (Cinzel, Inter) le misure cambiano: si ricontrolla quando è caricato.
    // I font si scaricano solo quando servono, quindi si chiede di caricarli e poi si rimisura.
    Promise.all([
      document.fonts?.load('700 16px Cinzel'),
      document.fonts?.load('400 16px Inter'),
      document.fonts?.load('italic 400 16px "Cormorant Garamond"'),
    ]).then(fit, fit);
    return () => {
      cancelled = true;
    };
  }, [ref, text, minFit, direction]);
}

// La carta montata dal frontend con i dati del backend (guida per il frontend, capitolo 12):
// illustrazione ritagliata, cornice della fazione e della rarità sopra, testi nelle loro zone.
// size "thumb" usa la miniatura dell'illustrazione, "large" l'immagine grande.
// Effetto e descrizione (flavour text) ci sono sempre: se non entrano, il testo si rimpicciolisce.
export default function Card({ card, size = 'thumb' }) {
  const nameRef = useRef(null);
  const textRef = useRef(null);
  const isFullArt = card.variantType === 'FULL_ART';
  const zones = cardZones(card);
  const art = size === 'large' ? card.illustrationUrl ?? card.illustrationThumbUrl : card.illustrationThumbUrl ?? card.illustrationUrl;
  // Testo per chi usa un lettore di schermo
  const label = `${card.name}, ${card.faction.name}, ${card.rarity.name}, attacco ${card.attack}, difesa ${card.defense}`;
  const artPosition = { objectPosition: `50% ${card.illustrationFocusY * 100}%` };
  const textKey = `${card.lordTitle ?? ''}|${card.effect?.text ?? ''}|${card.description ?? ''}`;

  useFitText(nameRef, card.name, { minFit: 0.55, direction: 'width' });
  useFitText(textRef, textKey, { minFit: 0.5, direction: 'height' });

  return (
    <figure
      className={`card card--${size} card--${card.faction.code.toLowerCase()}${isFullArt ? ' card--full-art' : ''}`}
      role="img"
      aria-label={label}
    >
      {/* 1. Illustrazione: nella finestra della cornice, o su tutta la carta per la full art */}
      <div className="card__art" style={isFullArt ? undefined : zoneStyle(zones.art)}>
        {art ? <img src={art} alt="" loading="lazy" style={artPosition} /> : <span className="card__no-art">{card.code}</span>}
      </div>

      {/* 2. Cornice sopra: la full art non ce l'ha */}
      {!isFullArt && <img className="card__frame" src={frameUrl(card)} alt="" />}

      {/* 3. Testi nelle loro zone, centrati */}
      <div className="card__zone card__name" style={zoneStyle(zones.name)}>
        <span ref={nameRef}>{card.name}</span>
      </div>

      <div className="card__zone card__text" style={zoneStyle(zones.text)}>
        <div className="card__text-inner" ref={textRef}>
          {card.lordTitle && <p className="card__lord">{card.lordTitle}</p>}
          {card.effect && (
            <p className="card__effect">
              <strong>{card.effect.name}</strong> {card.effect.text}
            </p>
          )}
          {card.description && <p className="card__description">{card.description}</p>}
        </div>
      </div>

      <div className="card__zone card__stat" style={zoneStyle(zones.attack)}>{card.attack}</div>
      <div className="card__zone card__stat" style={zoneStyle(zones.defense)}>{card.defense}</div>
    </figure>
  );
}
