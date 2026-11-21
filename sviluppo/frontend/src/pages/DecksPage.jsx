import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import '../styles/home.css';
import '../components/Mazzi/Mazzi.css';

// Elenco dei mazzi (GET /decks): validità, problemi, modifica ed eliminazione
export default function DecksPage() {
  const [decks, setDecks] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/decks').then(setDecks).catch(setError);
  }, []);

  // Mazzo classificato: PUT /me/ranked-deck, poi si rilegge la lista (isRanked cambia su due mazzi)
  const makeRanked = async (deck) => {
    try {
      await api('/me/ranked-deck', { method: 'PUT', body: { deckId: deck.id } });
      setDecks(await api('/decks'));
    } catch (err) {
      setError(err);
    }
  };

  const deleteDeck = async (deck) => {
    if (!window.confirm(`Eliminare il mazzo "${deck.name}"? Non si può annullare.`)) return;
    try {
      await api(`/decks/${deck.id}`, { method: 'DELETE' });
      setDecks((current) => current.filter((item) => item.id !== deck.id));
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content">
        <div className="collection-title-row">
          <h1>I tuoi mazzi</h1>
          <a className="home-button home-button--filled decks-new" href="/mazzi/editor">Nuovo mazzo</a>
        </div>

        {error && <p className="collection-message" role="alert">{error.message}</p>}
        {!error && !decks && <p className="collection-message">Caricamento mazzi...</p>}

        {decks && decks.length === 0 && (
          <p className="collection-message">
            Non hai ancora mazzi. <a href="/mazzi/editor">Componi il tuo primo mazzo</a>: servono 20 carte della tua collezione.
          </p>
        )}

        {decks && decks.length > 0 && (
          <ul className="decks-list">
            {decks.map((deck) => (
              <li key={deck.id} className={`deck-row${deck.isValid ? '' : ' deck-row--invalid'}`}>
                <div className="deck-row__cards" aria-hidden="true">
                  {deck.cards.slice(0, 4).map(({ card }) => (
                    card.illustrationThumbUrl
                      ? <img key={card.id} src={card.illustrationThumbUrl} alt="" style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }} />
                      : <span key={card.id} />
                  ))}
                </div>
                <div className="deck-row__info">
                  <h2>{deck.name}</h2>
                  <p>
                    {deck.totalCards} carte
                    {deck.isRanked && ' · Mazzo classificato'}
                  </p>
                  {deck.isValid ? (
                    <span className="deck-badge deck-badge--valid">Valido</span>
                  ) : (
                    <>
                      <span className="deck-badge deck-badge--invalid">Da sistemare</span>
                      <ul className="deck-problems">
                        {deck.problems.map((problem) => <li key={problem}>{problem}</li>)}
                      </ul>
                    </>
                  )}
                </div>
                <div className="deck-row__actions">
                  {deck.isValid && !deck.isRanked && (
                    <button type="button" className="deck-ranked" onClick={() => makeRanked(deck)}>Usa per le classificate</button>
                  )}
                  <a className="home-button home-button--outlined" href={`/mazzi/editor?id=${deck.id}`}>Modifica</a>
                  <button type="button" className="deck-delete" onClick={() => deleteDeck(deck)}>Elimina</button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
