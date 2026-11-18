import Card from '../Carta/Card';

// Dettaglio della carta scelta nella collezione: la carta grande e, sotto, codice, copie
// e copie libere per gli scambi
export default function SelectedCardDetail({ entry }) {
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
      <Card card={card} size="large" />

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
    </article>
  );
}
