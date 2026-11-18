import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Scambi/Scambi.css';
import '../components/Partita/Partita.css';

// Sfide tra amici: lancia una sfida (POST /challenges), sfide ricevute da accettare con un proprio
// mazzo (POST /challenges/{id}/accept → /partite/{matchId}) o rifiutare, sfide inviate da annullare.
export default function ChallengesPage() {
  const [friends, setFriends] = useState(null);
  const [decks, setDecks] = useState(null);
  const [received, setReceived] = useState([]);
  const [sent, setSent] = useState([]);
  const [opponentId, setOpponentId] = useState(new URLSearchParams(window.location.search).get('amico') ?? '');
  const [deckId, setDeckId] = useState('');
  // Mazzo scelto per accettare ciascuna sfida ricevuta
  const [acceptDecks, setAcceptDecks] = useState({});
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const loadChallenges = () => {
    api('/challenges?box=received').then(setReceived).catch(() => {});
    api('/challenges?box=sent').then(setSent).catch(() => {});
  };

  useEffect(() => {
    api('/friends').then(setFriends).catch(setError);
    api('/decks').then(setDecks).catch(setError);
    loadChallenges();
  }, []);

  const validDecks = (decks ?? []).filter((deck) => deck.isValid);
  // Il primo mazzo valido è già scelto, finché l'utente non ne sceglie un altro
  const chosenDeckId = deckId || validDecks[0]?.id || '';

  const run = async (action, successText) => {
    setBusy(true);
    setMessage(null);
    setError(null);
    try {
      await action();
      setMessage(successText);
      loadChallenges();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const challenge = () => {
    const friend = friends.find((item) => item.user.id === opponentId);
    run(
      () => api('/challenges', { method: 'POST', body: { opponentId, deckId: chosenDeckId } }),
      `Sfida lanciata a ${friend?.user.username}: ora deve accettarla.`,
    );
  };

  const accept = async (item) => {
    setBusy(true);
    setMessage(null);
    setError(null);
    try {
      const result = await api(`/challenges/${item.id}/accept`, {
        method: 'POST',
        body: { deckId: acceptDecks[item.id] || validDecks[0]?.id },
      });
      window.location.assign(`/partite/${result.matchId}`);
    } catch (err) {
      setError(err);
      setBusy(false);
      loadChallenges();
    }
  };

  const errorText = error && (
    error.code === 'ENGINE_UNAVAILABLE' ? 'Partita non disponibile, riprova tra poco.' : error.message
  );

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content social-content">
        <div className="collection-title-row">
          <h1>Sfide</h1>
          <a className="home-button home-button--outlined decks-new" href="/amici">Amici</a>
        </div>

        {message && <p className="social-message" role="status">{message}</p>}
        {error && (
          <div className="collection-message shop-error" role="alert">
            {errorText}
            {error.problems.length > 1 && <ul>{error.problems.map((problem) => <li key={problem}>{problem}</li>)}</ul>}
          </div>
        )}

        <section className="dashboard-panel challenge-new" aria-labelledby="challenge-title">
          <h2 id="challenge-title">Sfida un amico</h2>
          {friends && friends.length === 0 && <p className="collection-message">Si sfidano solo gli amici: <a href="/amici">aggiungi un amico</a>.</p>}
          {decks && validDecks.length === 0 && <p className="collection-message">Serve un mazzo valido: <a href="/mazzi/editor">componi un mazzo</a>.</p>}
          {friends && friends.length > 0 && validDecks.length > 0 && (
            <div className="challenge-form">
              <label className="deck-name">
                <span>Amico</span>
                <select value={opponentId} onChange={(event) => setOpponentId(event.target.value)}>
                  <option value="">Scegli un amico</option>
                  {friends.map((friend) => <option key={friend.user.id} value={friend.user.id}>{friend.user.username}</option>)}
                </select>
              </label>
              <label className="deck-name">
                <span>Il tuo mazzo</span>
                <select value={chosenDeckId} onChange={(event) => setDeckId(event.target.value)}>
                  {validDecks.map((deck) => <option key={deck.id} value={deck.id}>{deck.name}</option>)}
                </select>
              </label>
              <button type="button" className="home-button home-button--filled challenge-button" onClick={challenge} disabled={!opponentId || busy}>
                Lancia la sfida
              </button>
            </div>
          )}
        </section>

        <div className="social-columns">
          <section className="dashboard-panel" aria-labelledby="received-title">
            <h2 id="received-title">Sfide ricevute</h2>
            {received.length === 0 && <p className="collection-message">Nessuna sfida da accettare.</p>}
            <ul className="social-list challenge-list">
              {received.map((item) => (
                <li key={item.id}>
                  <div>
                    <strong>{item.challenger.username}</strong> ti sfida
                    {item.challengerDeckName && <small>con il mazzo {item.challengerDeckName}</small>}
                    {!item.challengerDeckValid && (
                      <p className="challenge-warning">
                        Il mazzo di {item.challenger.username} non è più valido: aspetta che lo sistemi o rifiuta la sfida.
                      </p>
                    )}
                  </div>
                  <div className="social-buttons">
                    {validDecks.length > 0 ? (
                      <>
                        <select
                          className="challenge-deck"
                          value={acceptDecks[item.id] || validDecks[0].id}
                          onChange={(event) => setAcceptDecks((current) => ({ ...current, [item.id]: event.target.value }))}
                          aria-label="Mazzo con cui giocare"
                        >
                          {validDecks.map((deck) => <option key={deck.id} value={deck.id}>{deck.name}</option>)}
                        </select>
                        <button type="button" className="social-action" onClick={() => accept(item)} disabled={busy || !item.challengerDeckValid}>
                          Accetta e gioca
                        </button>
                      </>
                    ) : (
                      <a className="social-action" href="/mazzi/editor">Componi un mazzo</a>
                    )}
                    <button
                      type="button"
                      className="social-action social-action--quiet"
                      onClick={() => run(() => api(`/challenges/${item.id}/reject`, { method: 'POST' }), 'Sfida rifiutata.')}
                      disabled={busy}
                    >
                      Rifiuta
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>

          <section className="dashboard-panel" aria-labelledby="sent-title">
            <h2 id="sent-title">Sfide inviate</h2>
            {sent.length === 0 && <p className="collection-message">Nessuna sfida in attesa.</p>}
            <ul className="social-list challenge-list">
              {sent.map((item) => (
                <li key={item.id}>
                  <div>
                    Hai sfidato <strong>{item.opponent.username}</strong>
                    {item.challengerDeckName && <small>con il mazzo {item.challengerDeckName}</small>}
                    {!item.challengerDeckValid && (
                      <div className="challenge-warning">
                        {item.challengerDeckName
                          ? <>Il tuo mazzo {item.challengerDeckName} non è più valido: <a href={`/mazzi/editor?id=${item.challengerDeckId}`}>sistemalo</a>.</>
                          : 'Il mazzo della sfida è stato cancellato.'}
                        <ul>{item.challengerDeckProblems.map((problem) => <li key={problem}>{problem}</li>)}</ul>
                      </div>
                    )}
                  </div>
                  <button
                    type="button"
                    className="social-action social-action--quiet"
                    onClick={() => run(() => api(`/challenges/${item.id}/cancel`, { method: 'POST' }), 'Sfida annullata.')}
                    disabled={busy}
                  >
                    Annulla
                  </button>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </main>
    </div>
  );
}
