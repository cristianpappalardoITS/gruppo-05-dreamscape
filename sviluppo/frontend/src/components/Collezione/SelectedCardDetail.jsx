import { useState } from 'react';
import Card from '../Carta/Card';
import CardZoom from '../Carta/CardZoom';

// Dettaglio della carta scelta nella collezione (e della carta appena uscita da un pacchetto):
// la carta grande e, sotto, codice, copie e copie libere per gli scambi. Premendo la carta si
// ingrandisce e si può girare per vedere il retro.
export default function SelectedCardDetail({ entry }) {
  const [zoomed, setZoomed] = useState(false);

  if (!entry) {
    return (
      <div className="card-detail card-detail--empty">
        <p>Seleziona una carta per vedere i dettagli.</p>
      </div>
    );
  }

  const { card, quantity, availableForTrade } = entry;
  const isFullArt = card.variantType === 'FULL_ART';

  return (
    <article className="card-detail" aria-labelledby="card-detail-title">
      <h2 id="card-detail-title" className="visually-hidden">{card.name}</h2>
      <button type="button" className="card-detail__zoom" onClick={() => setZoomed(true)} aria-label={`${card.name}: ingrandisci e gira`}>
        <Card card={card} size="large" />
      </button>

      <dl className="card-detail__info">
        <div>
          <dt>Codice</dt>
          <dd>{card.code}{isFullArt ? ' · Full art' : ''}</dd>
        </div>
        <div>
          <dt>Fazione e rarità</dt>
          <dd>{card.faction.name} · {card.rarity.name}</dd>
        </div>
        {quantity !== undefined && (
          <div>
            <dt>Copie</dt>
            <dd>{quantity}{availableForTrade !== undefined && ` · ${availableForTrade} libere per gli scambi`}</dd>
          </div>
        )}
      </dl>

      {zoomed && <CardZoom card={card} label={card.rarity.name} onClose={() => setZoomed(false)} />}
    </article>
  );
}
