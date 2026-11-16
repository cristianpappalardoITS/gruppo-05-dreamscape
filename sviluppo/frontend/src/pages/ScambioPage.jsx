// src/pages/ScambioPage.jsx
import React, { useState } from 'react';
import { mockCards } from '../data/cards';
import TradeOfferBox from '../components/Scambio/TradeOfferBox';
import TradeCardItem from '../components/Scambio/TradeCardItem';
import './ScambioPage.css';

const ScambioPage = () => {
  // Stato per le carte che stai offrendo
  const [myOffer, setMyOffer] = useState([]);
  
  // Simuliamo l'offerta dell'avversario (Luca)
  const [lucaOffer] = useState([mockCards[0], mockCards[1]]); 

  // Funzione per aggiungere/togliere una carta dall'offerta
  const toggleCardInOffer = (card) => {
    if (myOffer.find(c => c.id === card.id)) {
      setMyOffer(myOffer.filter(c => c.id !== card.id));
    } else {
      setMyOffer([...myOffer, card]);
    }
  };

  return (
    <div className="scambio-page">
      
      {/* HEADER */}
      <header className="scambio-header">
        <div className="logo-area">
          <span className="logo-icon">🏰</span>
          <h1>DREAMSCAPE</h1>
        </div>
        <nav className="nav-links">
          <a href="#">COLLEZIONE</a>
          <a href="#">MAZZI</a>
          <a href="#" className="active">SCAMBIO</a>
          <a href="#">ARENA</a>
        </nav>
        <div className="header-icons">
          <span>🛒</span>
          <span>👤</span>
        </div>
      </header>

      <p className="scambio-instruction">Controlla le offerte e conferma quando sei pronto.</p>

      {/* AREA DELLE DUE OFFERTE */}
      <div className="scambio-offers-area">
        
        {/* La tua offerta */}
        <div className="offer-container">
          <div className="offer-header">
            <h3>LA TUA OFFERTA</h3>
            <span className="offer-count">{myOffer.length} carte</span>
          </div>
          <TradeOfferBox cards={myOffer} />
          <div className="offer-status pending">
            <span>⏳</span> Da confermare
          </div>
        </div>

        {/* Icona di scambio */}
        <div className="scambio-icon-center">
          <span>⇄</span>
        </div>

        {/* Offerta di Luca */}
        <div className="offer-container">
          <div className="offer-header">
            <h3>OFFERTA DI LUCA</h3>
            <span className="offer-count">{lucaOffer.length} carte</span>
          </div>
          <TradeOfferBox cards={lucaOffer} />
          <div className="offer-status waiting">
            L'altro giocatore sta scegliendo...
          </div>
        </div>

      </div>

      {/* BOTTONI AZIONE */}
      <div className="scambio-actions">
        <button className="btn-cancel">Annulla</button>
        <button className="btn-confirm">Conferma offerta</button>
      </div>
      <p className="scambio-note">Lo scambio avviene quando entrambi confermano.</p>

      {/* SEZIONE COLLEZIONE IN BASSO */}
      <div className="scambio-collection-section">
        <div className="collection-header-bar">
          <h3>LA TUA COLLEZIONE</h3>
          <input type="text" placeholder="Cerca..." className="search-input" />
        </div>
        
        <div className="scambio-grid">
          {mockCards.map((card) => (
            <TradeCardItem 
              key={card.id} 
              card={card} 
              isInOffer={myOffer.some(c => c.id === card.id)}
              onToggle={() => toggleCardInOffer(card)}
            />
          ))}
        </div>
      </div>

    </div>
  );
};

export default ScambioPage;