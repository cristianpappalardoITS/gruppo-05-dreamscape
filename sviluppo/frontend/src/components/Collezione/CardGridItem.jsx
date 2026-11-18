import Card from '../Carta/Card';

// Una carta nella griglia: la carta montata e, se quantity c'è, il numero di copie
export default function CardGridItem({ entry, isSelected, onSelect }) {
  const { card, quantity } = entry;
  const isFullArt = card.variantType === 'FULL_ART';
  const label = `${card.name}${isFullArt ? ', full art' : ''}${quantity !== undefined ? `, ${quantity} copie` : ''}`;

  return (
    <button
      type="button"
      className={`grid-card grid-card--${card.rarity.code.toLowerCase()}${isSelected ? ' is-selected' : ''}`}
      onClick={() => onSelect(entry)}
      aria-pressed={isSelected}
      aria-label={label}
    >
      <Card card={card} />
      {quantity !== undefined && <span className="grid-card__quantity">x{quantity}</span>}
    </button>
  );
}
