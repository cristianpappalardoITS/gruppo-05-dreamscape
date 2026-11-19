import React from "react";

const SelectedCardDetail = ({ card }) => {
  if (!card) {
    return (
      <div className="selected-card-detail empty">
        <p>Seleziona una carta per vedere i dettagli.</p>
      </div>
    );
  }

  return (
    <div className="selected-card-detail">
      <div className="detail-image-wrapper">
        <img src={card.imageUrl} alt={card.name} className="detail-image" />
      </div>

      <h2 className="detail-name">{card.name}</h2>

      {card.rarity && (
        <span className={`detail-rarity rarity-${card.rarity.toLowerCase()}`}>
          {card.rarity}
        </span>
      )}

      {card.description && (
        <p className="detail-description">{card.description}</p>
      )}

      <div className="detail-stats">
        {card.attack !== undefined && (
          <div className="stat">
            <span className="stat-label">Attacco</span>
            <span className="stat-value">{card.attack}</span>
          </div>
        )}
        {card.defense !== undefined && (
          <div className="stat">
            <span className="stat-label">Difesa</span>
            <span className="stat-value">{card.defense}</span>
          </div>
        )}
        {card.cost !== undefined && (
          <div className="stat">
            <span className="stat-label">Costo</span>
            <span className="stat-value">{card.cost}</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default SelectedCardDetail;
