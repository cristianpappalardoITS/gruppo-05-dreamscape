import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import { loadAllCards } from '../components/Collezione/loadAllCards.js';
import { TradeCards, TradeWarnings } from '../components/Scambi/TradeParts.jsx';
import { hasWarnings } from '../components/Scambi/hasWarnings.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Scambi/Scambi.css';

// Una carta nella lista dello scambio: aggiunge una copia, senza superare le copie disponibili
function addCopy(entries, card, available) {
  const existing = entries.find((entry) => entry.card.id === card.id);
  if ((existing?.quantity ?? 0) >= available) return entries;
  if (existing) return entries.map((entry) => (entry.card.id === card.id ? { ...entry, quantity: entry.quantity + 1 } : entry));
  return [...entries, { card, quantity: 1 }];
}

function removeCopy(entries, card) {
  return entries
    .map((entry) => (entry.card.id === card.id ? { ...entry, quantity: entry.quantity - 1 } : entry))
    .filter((entry) => entry.quantity > 0);
}

// Griglia di una collezione per lo scambio: solo le carte con copie disponibili (availableForTrade)
function TradeCollection({ title, items, chosen, onAdd, search, onSearch }) {
  const text = search.trim().toLowerCase();
  const visible = items.filter((entry) => entry.availableForTrade > 0 && (!text || entry.card.name.toLowerCase().includes(text)));
  return (
    <section className="dashboard-panel trade-collection" aria-label={title}>
      <h2>{title}</h2>
      <input className="trade-search" type="search" value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Cerca una carta" maxLength={80} />
      {visible.length === 0 && <p className="collection-message">Nessuna carta da scambiare.</p>}
      <div className="collection-grid">
        {visible.map((entry) => {
          const inTrade = chosen.find((item) => item.card.id === entry.card.id)?.quantity ?? 0;
          const maxed = inTrade >= entry.availableForTrade;
          return (
            <div key={entry.card.id} className={`deck-tile${maxed ? ' is-maxed' : ''}`}>
              <CardGridItem entry={{ card: entry.card, quantity: entry.availableForTrade }} isSelected={inTrade > 0} onSelect={() => onAdd(entry)} />
              {inTrade > 0 && <span className="deck-tile__in-deck">Nello scambio: {inTrade}</span>}
            </div>
          );
        })}
      </div>
    </section>
  );
}

// Nuovo scambio: /scambi/nuovo (anche ?amico={userId}). Si sceglie l'amico, le carte da dare
// (GET /me/cards) e da chiedere (GET /users/{userId}/cards). Prima di proporre si leggono gli
// avvisi (POST /trades/preview) e, se ce ne sono, si chiede conferma; poi POST /trades.
export default function NewTradePage() {
  const [friends, setFriends] = useState(null);
  const [friendId, setFriendId] = useState(new URLSearchParams(window.location.search).get('amico') ?? '');
  const [myCards, setMyCards] = useState(null);
  const [friendCards, setFriendCards] = useState(null);
  const [offered, setOffered] = useState([]);
  const [requested, setRequested] = useState([]);
  const [message, setMessage] = useState('');
  const [mySearch, setMySearch] = useState('');
  const [friendSearch, setFriendSearch] = useState('');
  const [warnings, setWarnings] = useState(null);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/friends').then(setFriends).catch(setError);
    loadAllCards('/me/cards').then(setMyCards).catch(setError);
  }, []);

  useEffect(() => {
    if (!friendId) return undefined;
    let ignore = false;
    loadAllCards(`/users/${friendId}/cards`)
      .then((items) => { if (!ignore) setFriendCards(items); })
      .catch((err) => { if (!ignore) setError(err); });
    return () => {
      ignore = true;
    };
  }, [friendId]);

  const chooseFriend = (id) => {
    setFriendId(id);
    setFriendCards(null);
    setRequested([]);
    setWarnings(null);
    setError(null);
  };

  const body = () => ({
    recipientId: friendId,
    message: message.trim() || null,
    offered: offered.map(({ card, quantity }) => ({ cardId: card.id, quantity })),
    requested: requested.map(({ card, quantity }) => ({ cardId: card.id, quantity })),
  });

  const propose = async () => {
    setSending(true);
    setError(null);
    try {
      await api('/trades', { method: 'POST', body: body() });
      window.location.assign('/scambi?box=sent');
    } catch (err) {
      setError(err);
      setSending(false);
    }
  };

  // Primo clic: avvisi; se non ce ne sono si propone subito
  const check = async () => {
    setSending(true);
    setError(null);
    try {
      const result = await api('/trades/preview', { method: 'POST', body: body() });
      if (hasWarnings(result)) {
        setWarnings(result);
        setSending(false);
      } else {
        await propose();
      }
    } catch (err) {
      setError(err);
      setSending(false);
    }
  };

  const friend = friends?.find((item) => item.user.id === friendId);
  const canPropose = friendId && offered.length > 0 && requested.length > 0 && !sending;

  // Ogni modifica alle carte rende vecchi gli avvisi già letti
  const changeOffered = (next) => { setOffered(next); setWarnings(null); };
  const changeRequested = (next) => { setRequested(next); setWarnings(null); };

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content social-content">
        <div className="collection-title-row">
          <h1>Nuovo scambio</h1>
          <a className="deck-back" href="/scambi">Torna agli scambi</a>
        </div>

        {friends && friends.length === 0 && (
          <p className="collection-message">Si scambia solo con gli amici: <a href="/amici">aggiungi un amico</a>.</p>
        )}

        {friends && friends.length > 0 && (
          <label className="deck-name trade-friend">
            <span>Con chi scambi</span>
            <select value={friendId} onChange={(event) => chooseFriend(event.target.value)}>
              <option value="">Scegli un amico</option>
              {friends.map((item) => <option key={item.user.id} value={item.user.id}>{item.user.username}</option>)}
            </select>
          </label>
        )}

        {error && <p className="collection-message shop-error" role="alert">{error.message}</p>}

        {friend && (
          <>
            <div className="dashboard-panel trade-summary">
              <div className="trade__sides">
                <div>
                  <h3>Tu dai</h3>
                  <TradeCards entries={offered} emptyText="Tocca le tue carte qui sotto." />
                  {offered.length > 0 && (
                    <div className="trade-remove">
                      {offered.map(({ card }) => (
                        <button type="button" key={card.id} onClick={() => changeOffered(removeCopy(offered, card))}>- {card.name}</button>
                      ))}
                    </div>
                  )}
                </div>
                <div>
                  <h3>Chiedi a {friend.user.username}</h3>
                  <TradeCards entries={requested} emptyText={`Tocca le carte di ${friend.user.username} qui sotto.`} />
                  {requested.length > 0 && (
                    <div className="trade-remove">
                      {requested.map(({ card }) => (
                        <button type="button" key={card.id} onClick={() => changeRequested(removeCopy(requested, card))}>- {card.name}</button>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <label className="deck-name">
                <span>Messaggio (facoltativo)</span>
                <input type="text" value={message} onChange={(event) => setMessage(event.target.value)} maxLength={255} placeholder="Ti va?" />
              </label>

              {warnings && (
                <div className="trade-confirm">
                  <p>Prima di proporre:</p>
                  <TradeWarnings warnings={warnings} />
                </div>
              )}

              <div className="social-buttons trade-propose">
                <button type="button" className="home-button home-button--filled" onClick={warnings ? propose : check} disabled={!canPropose}>
                  {sending ? 'Attendi...' : warnings ? 'Proponi lo stesso' : 'Proponi lo scambio'}
                </button>
                {!canPropose && !sending && <span className="deck-hint">Serve almeno una carta per lato.</span>}
              </div>
            </div>

            <div className="trade-collections">
              {myCards && (
                <TradeCollection
                  title="Le tue carte"
                  items={myCards}
                  chosen={offered}
                  onAdd={(entry) => changeOffered(addCopy(offered, entry.card, entry.availableForTrade))}
                  search={mySearch}
                  onSearch={setMySearch}
                />
              )}
              {friendCards ? (
                <TradeCollection
                  title={`Le carte di ${friend.user.username}`}
                  items={friendCards}
                  chosen={requested}
                  onAdd={(entry) => changeRequested(addCopy(requested, entry.card, entry.availableForTrade))}
                  search={friendSearch}
                  onSearch={setFriendSearch}
                />
              ) : (
                <p className="collection-message">Caricamento delle carte di {friend.user.username}...</p>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
