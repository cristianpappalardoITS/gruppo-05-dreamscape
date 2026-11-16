// src/components/Collezione/CardGridItem.jsx
import React from 'react';

const CardGridItem = ({ card, isSelected, onSelect }) => {
  const getRarityColor = (rarity) => {
    switch(rarity) {
      case 'Comune': return '#9c95a8';
      case 'Rara': return '#4da6ff';
      case 'Epica': return '#b366ff';
      case 'Leggendaria': return '#dca466';
      default: return '#333';
    }
  };

  return (
    <div 
      className={`grid-card ${isSelected ? 'selected' : ''}`} 
      onClick={() => onSelect(card)}
      style={{ borderColor: getRarityColor(card.rarity) }}
    >
      <div className="card-header-grid">
        <span className="card-id">{card.id}</span>
        <span className="card-rarity" style={{ color: getRarityColor(card.rarity) }}>{card.rarity}</span>
      </div>
      
      <h4 className="grid-card-title">{card.name}</h4>
      
      <div className="grid-card-image">
        <div className="image-placeholder">X</div>
      </div>
      
      <div className="grid-card-stats">
        <span>⚔️ {card.attack}</span>
        <span>🛡️ {card.defense}</span>
      </div>
    </div>
  );
};

export default CardGridItem;