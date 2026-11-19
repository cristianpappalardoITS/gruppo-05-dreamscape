import React from "react";

const CardGridItem = ({ card, quantity, isSelected, onSelect }) => {
  if (!card) return null;

  return (
    <div
      className={`card-grid-item ${isSelected ? "selected" : ""}`}
      onClick={() => onSelect(card)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") onSelect(card);
      }}
    >
      <div className="card-image-wrapper">
        <img
          src={card.illustrationThumbUrl || card.illustrationUrl}
          alt={card.name}
          className="card-image"
          loading="lazy"
          onError={(e) => { e.currentTarget.src = "/images/placeholder.webp"; }}
        />
        {quantity > 1 && (
          <span className="card-quantity-badge">x{quantity}</span>
        )}
      </div>
      <div className="card-info">
        <span className="card-name">{card.name}</span>
        {card.rarity && (
          <span className={`card-rarity rarity-${card.rarity.code?.toLowerCase()}`}>
            {card.rarity.name}
          </span>
        )}
      </div>
    </div>
  );
};

export default CardGridItem;
