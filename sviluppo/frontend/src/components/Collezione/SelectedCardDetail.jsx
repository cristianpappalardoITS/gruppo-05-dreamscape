// Dettaglio della carta scelta nella collezione
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
    <article className={`card-detail card-detail--${card.rarity.code.toLowerCase()}`} aria-labelledby="card-detail-title">
      <div className="card-detail__header">
        <span>{card.code}</span>
        <span>{card.faction.name}</span>
        <span>{card.rarity.name}</span>
      </div>

      <h2 id="card-detail-title">{card.name}</h2>
      {card.lordTitle && <p className="card-detail__lord">{card.lordTitle}</p>}

      <div className={`card-detail__image${isFullArt ? ' card-detail__image--full-art' : ''}`}>
        {card.illustrationUrl ? (
          <img
            src={card.illustrationUrl}
            alt={card.name}
            style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }}
          />
        ) : (
          <span className="grid-card__placeholder">{card.code}</span>
        )}
      </div>

      <div className="card-detail__stats">
        <div><span>ATT</span><strong>{card.attack}</strong></div>
        <div><span>DIF</span><strong>{card.defense}</strong></div>
      </div>

      {card.effect && (
        <div className="card-detail__effect">
          <strong>{card.effect.name}</strong>
          <p>{card.effect.text}</p>
        </div>
      )}

      {card.description && <p className="card-detail__description">{card.description}</p>}

      <p className="card-detail__copies">
        Copie: {quantity} · Disponibili per gli scambi: {availableForTrade}
      </p>
    </article>
  );
}
