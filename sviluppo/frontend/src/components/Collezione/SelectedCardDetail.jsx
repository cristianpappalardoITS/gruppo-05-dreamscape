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

  const getRarityColor = (rarity) => {
    switch(rarity) {
      case 'Comune': return '#9c95a8';
      case 'Rara': return '#4da6ff';
      case 'Epica': return '#b366ff';
      case 'Leggendaria': return '#dca466';
      default: return '#333';
    }
  };

  // Controlli di sicurezza per evitare "undefined" a schermo
  const loreText = card.lore ? card.lore : "Nessuna descrizione disponibile.";
  const effectText = card.effect ? card.effect : "Effetto non indicato.";

  return (
    <div 
      className="selected-card-container" 
      style={{ borderColor: getRarityColor(card.rarity) }}
    >
      <div className="card-header-detail">
        <span className="card-id">{card.id}</span>
        <span className="card-faction">{card.faction}</span>
        <span className="card-rarity" style={{ color: getRarityColor(card.rarity) }}>{card.rarity}</span>
      </div>

      <h2 className="selected-card-title">{card.name}</h2>
      
      <div className="selected-card-image">
        <div className="image-placeholder">X</div>
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