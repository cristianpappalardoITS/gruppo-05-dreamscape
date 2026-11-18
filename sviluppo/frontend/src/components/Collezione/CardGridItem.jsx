import Card from '../Carta/Card';

// Una carta nella griglia: la carta montata e, se quantity c'è, il numero di copie.
// badge sostituisce il testo dell'etichetta (es. "1/2" nell'editor del mazzo),
// label il testo letto dai lettori di schermo (es. "Togli una copia di ...").
export default function CardGridItem({ entry, isSelected, onSelect, badge, label }) {
  const { card, quantity } = entry;
  const isFullArt = card.variantType === 'FULL_ART';
  const badgeText = badge ?? (quantity !== undefined ? `x${quantity}` : null);
  const ariaLabel = label ?? `${card.name}${isFullArt ? ', full art' : ''}${quantity !== undefined ? `, ${quantity} copie` : ''}`;

  return (
    <button
      type="button"
      className={`grid-card grid-card--${card.rarity.code.toLowerCase()}${isSelected ? ' is-selected' : ''}`}
      onClick={() => onSelect(entry)}
      aria-pressed={isSelected}
      aria-label={ariaLabel}
    >
      <Card card={card} />
      {badgeText && <span className="grid-card__quantity">{badgeText}</span>}
    </button>
  );
}
