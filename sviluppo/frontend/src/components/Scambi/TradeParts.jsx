// Pezzi in comune tra la pagina Scambi e il Nuovo scambio

// Carte di un lato dello scambio: [{ card, quantity }], con miniatura, nome e copie
export function TradeCards({ entries, emptyText = 'Nessuna carta' }) {
  if (entries.length === 0) return <p className="collection-message">{emptyText}</p>;
  return (
    <ul className="trade-cards">
      {entries.map(({ card, quantity }) => (
        <li key={card.id} className={`grid-card--${card.rarity.code.toLowerCase()}`}>
          {card.illustrationThumbUrl
            ? <img src={card.illustrationThumbUrl} alt="" style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }} />
            : <span className="trade-cards__placeholder" />}
          <span className="trade-cards__name">
            {card.name}
            <small>{card.rarity.name}{card.variantType === 'FULL_ART' ? ' · Full art' : ''}</small>
          </span>
          <strong>x{quantity}</strong>
        </li>
      ))}
    </ul>
  );
}

// Avvisi su mazzi e ultime copie (POST /trades/preview, GET /trades/{tradeId}/deck-warnings)
export function TradeWarnings({ warnings }) {
  const { deckWarnings, lastCopyWarnings } = warnings;
  if (deckWarnings.length === 0 && lastCopyWarnings.length === 0) return null;
  return (
    <ul className="trade-warnings">
      {deckWarnings.map((warning) => (
        <li key={`${warning.deckId}-${warning.cardName}`}>
          {warning.outcome === 'NORMAL_VERSION'
            ? `Il mazzo ${warning.deckName} userà la versione normale di ${warning.cardName}.`
            : `Il mazzo ${warning.deckName} diventerà non valido${warning.isRanked ? ' (è il tuo mazzo classificato)' : ''}.`}
        </li>
      ))}
      {lastCopyWarnings.map((warning) => (
        <li key={warning.cardId}>Dopo lo scambio non avrai più {warning.cardName}.</li>
      ))}
    </ul>
  );
}

