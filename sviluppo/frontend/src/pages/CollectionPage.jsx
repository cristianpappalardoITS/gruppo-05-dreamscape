// src/pages/CollectionPage.jsx
import React, { useState, useEffect } from 'react';
import { fetchUserCollection } from '../services/cardService'; // Importa il servizio
import CardGridItem from '../components/Collezione/CardGridItem';
import SelectedCardDetail from '../components/Collezione/SelectedCardDetail';
import './CollectionPage.css';

const CollectionPage = () => {
  // Stati per i dati che arrivano dal DB
  const [cards, setCards] = useState([]);
  const [stats, setStats] = useState({ owned_count: 0, total_count: 0 });
  const [selectedCard, setSelectedCard] = useState(null);
  
  // Stati per gestire il caricamento
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Al caricamento della pagina, chiamiamo il DB
  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoading(true);
        const data = await fetchUserCollection(); // Chiamata al DB
        
        setCards(data.cards);
        setStats(data.stats);
        
        // Seleziona la prima carta di default (se c'è)
        if (data.cards.length > 0) {
          setSelectedCard(data.cards[0]);
        }
      } catch (err) {
        setError("Impossibile caricare la collezione.");
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, []);

  // Se sta caricando, mostra un messaggio
  if (isLoading) {
    return <div className="loading-state">Caricamento collezione...</div>;
  }

  // Se c'è un errore, mostralo
  if (error) {
    return <div className="error-state">{error}</div>;
  }

  return (
    <div className="collection-page">
      
      {/* HEADER */}
      <header className="collection-header">
        <div className="logo-area">
          <span className="logo-icon">🏰</span>
          <h1>DREAMSCAPE</h1>
        </div>
        <nav className="nav-links">
          <a href="#" className="active">COLLEZIONE</a>
          <a href="#">MAZZI</a>
          <a href="#">SCAMBIO</a>
          <a href="#">ARENA</a>
        </nav>
        <div className="header-icons">
          <span>🛒</span>
          <span>👤</span>
        </div>
      </header>

      {/* CONTENUTO PRINCIPALE */}
      <main className="collection-content">
        
        {/* COLONNA SINISTRA */}
        <aside className="left-column">
          <SelectedCardDetail card={selectedCard} />
        </aside>

        {/* COLONNA DESTRA */}
        <section className="right-column">
          <div className="grid-header">
            <button className="filter-btn">filtro</button>
            
            {/* CONTATORE: Usa i dati del DB! */}
            <span className="counter">
              POSsedute {stats.owned_count}/{stats.total_count}
            </span>
          </div>

          {/* Griglia: mostriamo tutte le carte che il DB ci ha inviato */}
          <div className="cards-grid">
            {cards.map((card) => (
              <CardGridItem 
                key={card.id} 
                card={card} 
                isSelected={selectedCard?.id === card.id}
                onSelect={setSelectedCard} 
              />
            ))}
          </div>
        </section>

      </main>
    </div>
  );
};

export default CollectionPage;