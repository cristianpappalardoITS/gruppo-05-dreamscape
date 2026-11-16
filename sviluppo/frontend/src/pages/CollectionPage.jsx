// src/pages/CollectionPage.jsx
import React, { useState, useEffect } from 'react';
import { api } from '../api'; // <-- Importa da src/api.js
import CardGridItem from '../components/Collezione/CardGridItem';
import SelectedCardDetail from '../components/Collezione/SelectedCardDetail';
import '../components/Collezione/Collezione.css';

const CollectionPage = () => {
  const [cards, setCards] = useState([]); // Array di { card, quantity }
  const [selectedCard, setSelectedCard] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Per la paginazione (es. pagina 0, 20 carte per pagina)
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const size = 20; // Carte per pagina

  useEffect(() => {
    const loadCollection = async () => {
      try {
        setIsLoading(true);
        // Chiamata all'endpoint corretto: GET /me/cards?page=0&size=20
        const data = await api(`/me/cards?page=${page}&size=${size}`);
        
        // data.items è un array di { card: {...}, quantity: 2 }
        setCards(data.items);
        setTotalPages(data.totalPages);
        setTotalItems(data.totalItems);
        
        // Seleziona la prima carta della pagina (se c'è)
        if (data.items.length > 0) {
          setSelectedCard(data.items[0].card);
        }
      } catch (err) {
        setError(err.message || "Impossibile caricare la collezione.");
      } finally {
        setIsLoading(false);
      }
    };
    loadCollection();
  }, [page]); // Ricarica quando cambia la pagina

  if (isLoading) return <div className="loading-state">Caricamento collezione...</div>;
  if (error) return <div className="error-state">{error}</div>;

  return (
    <div className="collection-page">
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

      <main className="collection-content">
        <aside className="left-column">
          <SelectedCardDetail card={selectedCard} />
        </aside>

        <section className="right-column">
          <div className="grid-header">
            <button className="filter-btn">filtro</button>
            <span className="counter">
              POSsedute {totalItems} / 100
            </span>
          </div>

          <div className="cards-grid">
            {cards.map((item) => (
              <CardGridItem 
                key={item.card.id} 
                card={item.card} 
                quantity={item.quantity}
                isSelected={selectedCard?.id === item.card.id}
                onSelect={setSelectedCard} 
              />
            ))}
          </div>

          {/* Paginazione */}
          {totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}>Indietro</button>
              <span>Pagina {page + 1} di {totalPages}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Avanti</button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default CollectionPage;