import { useEffect, useState } from 'react';
import { api, errorsByField } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Scambi/Scambi.css';

// Amici (pagina nuova della guida): cerca un Dreamweaver, richieste ricevute e inviate, lista amici
export default function FriendsPage() {
  const [friends, setFriends] = useState(null);
  const [received, setReceived] = useState([]);
  const [sent, setSent] = useState([]);
  const [search, setSearch] = useState('');
  const [results, setResults] = useState([]);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const reload = () => {
    api('/friends').then(setFriends).catch(setError);
    api('/friend-requests?direction=received').then(setReceived).catch(() => {});
    api('/friend-requests?direction=sent').then(setSent).catch(() => {});
  };

  useEffect(() => {
    reload();
  }, []);

  // La ricerca parte da 2 caratteri
  useEffect(() => {
    const text = search.trim();
    if (text.length < 2) return undefined;
    let ignore = false;
    api(`/users?search=${encodeURIComponent(text)}`)
      .then((users) => { if (!ignore) setResults(users); })
      .catch(() => {});
    return () => {
      ignore = true;
    };
  }, [search]);

  // Esegue un'azione e mostra il risultato in alto
  const run = async (action, successText) => {
    setMessage(null);
    setError(null);
    try {
      await action();
      setMessage(successText);
      reload();
    } catch (err) {
      setError(err);
    }
  };

  const sendRequest = (username) => run(
    () => api('/friend-requests', { method: 'POST', body: { username } }),
    `Richiesta mandata a ${username}.`,
  );

  const removeFriend = (friend) => {
    if (!window.confirm(`Togliere ${friend.user.username} dagli amici? Si annullano anche gli scambi e le sfide in attesa tra voi.`)) return;
    run(() => api(`/friends/${friend.user.id}`, { method: 'DELETE' }), `${friend.user.username} non è più tra i tuoi amici.`);
  };

  const friendIds = new Set((friends ?? []).map((friend) => friend.user.id));
  const sentIds = new Set(sent.map((request) => request.user.id));
  const searchError = error ? errorsByField(error).username : null;
  const visibleResults = search.trim().length >= 2 ? results : [];

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content social-content">
        <div className="collection-title-row">
          <h1>Amici</h1>
          <a className="home-button home-button--filled decks-new" href="/scambi/nuovo">Nuovo scambio</a>
        </div>

        {message && <p className="social-message" role="status">{message}</p>}
        {error && <p className="collection-message shop-error" role="alert">{searchError ?? error.message}</p>}

        <div className="social-columns">
          <section className="dashboard-panel" aria-labelledby="search-title">
            <h2 id="search-title">Cerca un Dreamweaver</h2>
            <label className="deck-name">
              <span>Username</span>
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Almeno 2 caratteri"
                maxLength={30}
              />
            </label>
            {search.trim().length >= 2 && visibleResults.length === 0 && (
              <p className="collection-message">Nessun Dreamweaver trovato.</p>
            )}
            <ul className="social-list">
              {visibleResults.map((user) => (
                <li key={user.id}>
                  <span>{user.username}</span>
                  {friendIds.has(user.id) && <small>Già amici</small>}
                  {!friendIds.has(user.id) && sentIds.has(user.id) && <small>Richiesta mandata</small>}
                  {!friendIds.has(user.id) && !sentIds.has(user.id) && (
                    <button type="button" className="social-action" onClick={() => sendRequest(user.username)}>Aggiungi</button>
                  )}
                </li>
              ))}
            </ul>
          </section>

          <section className="dashboard-panel" aria-labelledby="requests-title">
            <h2 id="requests-title">Richieste</h2>
            {received.length === 0 && sent.length === 0 && <p className="collection-message">Nessuna richiesta in attesa.</p>}
            <ul className="social-list">
              {received.map((request) => (
                <li key={request.id}>
                  <span>{request.user.username} <small>vuole essere tuo amico</small></span>
                  <span className="social-buttons">
                    <button
                      type="button"
                      className="social-action"
                      onClick={() => run(() => api(`/friend-requests/${request.id}/accept`, { method: 'POST' }), `Ora tu e ${request.user.username} siete amici.`)}
                    >
                      Accetta
                    </button>
                    <button
                      type="button"
                      className="social-action social-action--quiet"
                      onClick={() => run(() => api(`/friend-requests/${request.id}`, { method: 'DELETE' }), 'Richiesta rifiutata.')}
                    >
                      Rifiuta
                    </button>
                  </span>
                </li>
              ))}
              {sent.map((request) => (
                <li key={request.id}>
                  <span>{request.user.username} <small>richiesta mandata da te</small></span>
                  <button
                    type="button"
                    className="social-action social-action--quiet"
                    onClick={() => run(() => api(`/friend-requests/${request.id}`, { method: 'DELETE' }), 'Richiesta annullata.')}
                  >
                    Annulla
                  </button>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <section className="dashboard-panel social-friends" aria-labelledby="friends-title">
          <h2 id="friends-title">I tuoi amici</h2>
          {!friends && !error && <p className="collection-message">Caricamento...</p>}
          {friends && friends.length === 0 && <p className="collection-message">Non hai ancora amici: cercali per username.</p>}
          {friends && friends.length > 0 && (
            <ul className="social-list">
              {friends.map((friend) => (
                <li key={friend.user.id}>
                  <span>
                    {friend.user.username}
                    <small>amici dal {new Date(friend.since).toLocaleDateString('it-IT')}</small>
                  </span>
                  <span className="social-buttons">
                    <a className="social-action" href={`/scambi/nuovo?amico=${friend.user.id}`}>Proponi scambio</a>
                    <a className="social-action social-action--quiet" href={`/sfide?amico=${friend.user.id}`}>Sfida</a>
                    <button type="button" className="social-action social-action--quiet" onClick={() => removeFriend(friend)}>Togli</button>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
