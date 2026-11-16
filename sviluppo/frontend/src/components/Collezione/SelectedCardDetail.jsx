// src/components/Collezione/SelectedCardDetail.jsx
import React from 'react';

const SelectedCardDetail = ({ card }) => {
  if (!card) {
    return (
      <div className="selected-card-container empty">
        <p>Seleziona una carta per vedere i dettagli.</p>
      </div>
    );
  }

  const getRarityColor = (rarityCode) => {
    switch(rarityCode) {
      case 'COMMON': return '#9c95a8';
      case 'RARE': return '#4da6ff';
      case 'EPIC': return '#b366ff';
      case 'LEGENDARY': return '#dca466';
      default: return '#333';
    }
  };

  const loreText = card.description ? card.description : "Nessuna descrizione disponibile.";
  const effectText = card.effect?.text ? card.effect.text : "Effetto non indicato.";

  return (
    <div 
      className="selected-card-container" 
      style={{ borderColor: getRarityColor(card.rarity.code) }}
    >
      <div className="card-header-detail">
        <span className="card-id">{card.code}</span>
        <span className="card-faction">{card.faction.name}</span>
        <span className="card-rarity" style={{ color: getRarityColor(card.rarity.code) }}>
          {card.rarity.name}
        </span>
      </div>

      <h2 className="selected-card-title">{card.name}</h2>
      
      <div className="selected-card-image">
        {card.illustrationUrl ? (
          <img src={card.illustrationUrl} alt={card.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <div className="image-placeholder">X</div>
        )}
      </div>
      
      <div className="selected-card-lore">
        <p><em>"{loreText}"</em></p>
      </div>

      <div className="selected-card-ability">
        <strong>Effetto:</strong>
        <p>{effectText}</p>
      </div>
      
      <div className="selected-card-stats">
        <div className="stat-box">
          <span className="stat-label">ATT</span>
          <span className="stat-value">⚔️ {card.attack}</span>
        </div>
        <div className="stat-box">
          <span className="stat-label">DIF</span>
          <span className="stat-value">🛡️ {card.defense}</span>
        </div>
      </div>
    </div>
  );
};

export default SelectedCardDetail;