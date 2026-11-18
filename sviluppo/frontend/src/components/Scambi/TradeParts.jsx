// Pezzi in comune tra la pagina Scambi e il Nuovo scambio
import Card from '../Carta/Card';

// Carte di un lato dello scambio: [{ card, quantity }], come piccole carte con le copie
export function TradeCards({ entries, emptyText = 'Nessuna carta' }) {
  if (entries.length === 0) return <p className="collection-message">{emptyText}</p>;
  return (
    <ul className="trade-cards">
      {entries.map(({ card, quantity }) => (
        <li key={card.id}>
          <Card card={card} />
          {quantity > 1 && <span className="grid-card__quantity">x{quantity}</span>}
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

