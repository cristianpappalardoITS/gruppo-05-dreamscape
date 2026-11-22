import { useEffect, useRef, useState } from 'react';
import CardGridItem from '../Collezione/CardGridItem';
import SelectedCardDetail from '../Collezione/SelectedCardDetail';
import CardZoom from '../Carta/CardZoom';

// Distanza minima in pixel perché uno swipe verso sinistra passi alla carta successiva
const SWIPE_DISTANCE = 50;

// Apertura classica di un pacchetto: le carte escono una alla volta, nell'ordine di uscita.
// "Mostra tutte" salta alla fine; alla fine si vedono tutte le carte del pacchetto insieme e, premendone
// una, si ingrandisce (e si può girare). extraActions: altri pulsanti accanto a quello per andare avanti
// (nel negozio: ricomprare lo stesso pacchetto). onReveal: chiamata a ogni carta che esce (il suono)
export default function PackOpening({ opening, nextLabel, onDone, extraActions = null, onReveal = null }) {
  const [index, setIndex] = useState(0);
  // Carta ingrandita alla fine del pacchetto (premendo una delle carte)
  const [zoomCard, setZoomCard] = useState(null);
  // Punto in cui il dito ha toccato lo schermo (un ref: deve essere pronto subito, senza aspettare un render)
  const touchStartX = useRef(null);
  const total = opening.cards.length;
  const isFinished = index >= total;

  const next = () => setIndex((current) => Math.min(current + 1, total));

  // Ogni carta che esce (non con "Mostra tutte", che salta alla fine)
  useEffect(() => {
    if (onReveal && index < total) onReveal(opening.cards[index].card);
  }, [index, total, onReveal, opening]);

  const handleTouchEnd = (event) => {
    if (touchStartX.current !== null && touchStartX.current - event.changedTouches[0].clientX > SWIPE_DISTANCE) next();
    touchStartX.current = null;
  };

  if (isFinished) {
    // Righe e colonne della griglia finale: una riga fino a 6 carte, poi due. Le usa l'apertura
    // a misura di schermo (Benvenuto.css, .opening-stage) per calcolare quanto grandi possono essere
    const rows = total <= 6 ? 1 : 2;
    const columns = Math.ceil(total / rows);
    return (
      <section className="pack-opening" aria-labelledby="pack-opening-title">
        <div className="welcome-intro">
          <span className="welcome-eyebrow">Pacchetto aperto</span>
          <h1 id="pack-opening-title">{opening.packName}</h1>
          <p>Queste carte sono già nella tua collezione.</p>
        </div>
        <div className="pack-opening__all" style={{ '--columns': columns, '--rows': rows }}>
          {opening.cards.map(({ card }, position) => (
            <div className="reveal-card" style={{ '--reveal-delay': `${position * 80}ms` }} key={`${card.id}-${position}`}>
              <CardGridItem entry={{ card }} isSelected={false} onSelect={() => setZoomCard(card)} label={`${card.name}: ingrandisci`} />
            </div>
          ))}
        </div>
        <div className="welcome-confirm">
          <div className="pack-opening__end-actions">
            {extraActions}
            <button type="button" className={`home-button ${extraActions ? 'home-button--outlined' : 'home-button--filled'}`} onClick={onDone}>{nextLabel}</button>
          </div>
        </div>
        {zoomCard && <CardZoom card={zoomCard} label={opening.packName} onClose={() => setZoomCard(null)} />}
      </section>
    );
  }

  const { card } = opening.cards[index];

  return (
    <section
      className="pack-opening"
      aria-labelledby="pack-opening-title"
      onTouchStart={(event) => { touchStartX.current = event.touches[0].clientX; }}
      onTouchEnd={handleTouchEnd}
    >
      <div className="pack-opening__header">
        <h1 id="pack-opening-title">{opening.packName}</h1>
        <span>Carta {index + 1} di {total}</span>
      </div>
      <div className="pack-opening__progress" aria-hidden="true">
        {opening.cards.map((_, position) => (
          <span key={position} className={position <= index ? 'is-done' : ''} />
        ))}
      </div>

      {/* La chiave cambia a ogni carta: così l'animazione di uscita riparte */}
      <div className={`pack-opening__card faction-glow--${card.faction.code.toLowerCase()}`} key={index}>
        <SelectedCardDetail entry={{ card }} />
      </div>

      <p className="pack-opening__labels">
        <span>{card.rarity.name}</span>
        {card.lordTitle && <span>{card.lordTitle}</span>}
        {card.variantType === 'FULL_ART' && <span>Full art</span>}
      </p>

      <div className="pack-opening__actions">
        <button type="button" className="home-button home-button--filled" onClick={next}>
          {index + 1 < total ? 'Prossima carta' : 'Fine'}
        </button>
        <button type="button" className="pack-opening__show-all" onClick={() => setIndex(total)}>
          Mostra tutte
        </button>
      </div>
      <p className="pack-opening__hint">Tocca «Prossima carta» o scorri verso sinistra.</p>
    </section>
  );
}
