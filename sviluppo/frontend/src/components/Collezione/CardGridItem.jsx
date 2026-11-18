// Una carta nella griglia della collezione: miniatura, nome, rarità e copie possedute
export default function CardGridItem({ entry, isSelected, onSelect }) {
  const { card, quantity } = entry;
  const isFullArt = card.variantType === 'FULL_ART';

  return (
    <button
      type="button"
      className={`grid-card grid-card--${card.rarity.code.toLowerCase()}${isSelected ? ' is-selected' : ''}`}
      onClick={() => onSelect(entry)}
      aria-pressed={isSelected}
    >
      <div className="grid-card__image">
        {card.illustrationThumbUrl ? (
          <img
            src={card.illustrationThumbUrl}
            alt=""
            loading="lazy"
            style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }}
          />
        ) : (
          <span className="grid-card__placeholder">{card.code}</span>
        )}
        <span className="grid-card__quantity">x{quantity}</span>
      </div>
      <span className="grid-card__name">{card.name}</span>
      <span className="grid-card__meta">
        {card.rarity.name}{isFullArt ? ' · Full art' : ''}
      </span>
    </button>
  );
}
