import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import Card from '../components/Carta/Card';
import CardGridItem from '../components/Collezione/CardGridItem';
import { loadAllCards } from '../components/Collezione/loadAllCards.js';
import { TradeWarnings } from '../components/Scambi/TradeParts.jsx';
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

const countCards = (entries) => entries.reduce((sum, entry) => sum + entry.quantity, 0);
const cardsLabel = (count) => (count === 1 ? '1 carta' : `${count} carte`);

// Un lato dello scambio, come nel mockup: titolo, numero di carte e carte con la × per toglierle
function TradeSide({ title, entries, onRemove, emptyText, note }) {
  return (
    <section className="trade-side" aria-label={title}>
      <div className="trade-side__header">
        <h2>{title}</h2>
        <span className="trade-side__count">{cardsLabel(countCards(entries))}</span>
      </div>
      {entries.length === 0 ? (
        <p className="trade-side__empty">{emptyText}</p>
      ) : (
        <ul className="trade-side__cards">
          {entries.map(({ card, quantity }) => (
            <li key={card.id}>
              <Card card={card} />
              {quantity > 1 && <span className="grid-card__quantity">x{quantity}</span>}
              <button type="button" className="trade-side__remove" onClick={() => onRemove(card)} aria-label={`Togli una copia di ${card.name}`}>×</button>
            </li>
          ))}
        </ul>
      )}
      {note && <p className="trade-side__note">{note}</p>}
    </section>
  );
}

// Carte da cui scegliere (le tue o quelle dell'amico): solo quelle con copie disponibili
// (availableForTrade), con il pulsante "+" o "In offerta" sotto ogni carta
function TradePool({ items, chosen, onAdd, search, chosenText }) {
  const text = search.trim().toLowerCase();
  const visible = items.filter((entry) => entry.availableForTrade > 0 && (!text || entry.card.name.toLowerCase().includes(text)));
  if (visible.length === 0) return <p className="collection-message">Nessuna carta da scambiare.</p>;
  return (
    <div className="trade-pool__grid">
      {visible.map((entry) => {
        const inTrade = chosen.find((item) => item.card.id === entry.card.id)?.quantity ?? 0;
        const maxed = inTrade >= entry.availableForTrade;
        const add = () => onAdd(entry);
        return (
          <div key={entry.card.id} className={`trade-pool__tile${maxed ? ' is-maxed' : ''}`}>
            <CardGridItem
              entry={{ card: entry.card, quantity: entry.availableForTrade }}
              isSelected={inTrade > 0}
              onSelect={add}
              label={`Aggiungi ${entry.card.name} allo scambio (${inTrade} su ${entry.availableForTrade} copie)`}
            />
            <button type="button" className={`trade-pool__add${inTrade > 0 ? ' is-chosen' : ''}`} onClick={add} disabled={maxed} tabIndex={-1} aria-hidden="true">
              {inTrade > 0 ? `✓ ${chosenText}${entry.availableForTrade > 1 ? ` ${inTrade}/${entry.availableForTrade}` : ''}` : '+'}
            </button>
          </div>
        );
      })}
    </div>
  );
}

// Nuovo scambio come nel mockup: /scambi/nuovo (anche ?amico={userId}). In alto le due offerte,
// in basso le carte da cui scegliere: le tue (GET /me/cards) e quelle dell'amico
// (GET /users/{userId}/cards). Prima di proporre si leggono gli avvisi (POST /trades/preview)
// e, se ce ne sono, si chiede conferma; poi POST /trades.
export default function NewTradePage() {
  const [friends, setFriends] = useState(null);
  const [friendId, setFriendId] = useState(new URLSearchParams(window.location.search).get('amico') ?? '');
  const [myCards, setMyCards] = useState(null);
  const [friendCards, setFriendCards] = useState(null);
  const [offered, setOffered] = useState([]);
  const [requested, setRequested] = useState([]);
  const [message, setMessage] = useState('');
  const [poolTab, setPoolTab] = useState('mine');
  const [search, setSearch] = useState('');
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
  const friendName = friend?.user.username;
  const canPropose = friendId && offered.length > 0 && requested.length > 0 && !sending;

  // Ogni modifica alle carte rende vecchi gli avvisi già letti
  const changeOffered = (next) => { setOffered(next); setWarnings(null); };
  const changeRequested = (next) => { setRequested(next); setWarnings(null); };

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content trade-new">
        <h1 className="visually-hidden">Nuovo scambio</h1>

        {friends && friends.length === 0 && (
          <p className="collection-message">Si scambia solo con gli amici: <a href="/amici">aggiungi un amico</a>.</p>
        )}

        {friends && friends.length > 0 && (
          <label className="trade-friend">
            <span>Scambia con</span>
            <select value={friendId} onChange={(event) => chooseFriend(event.target.value)}>
              <option value="">Scegli un amico</option>
              {friends.map((item) => <option key={item.user.id} value={item.user.id}>{item.user.username}</option>)}
            </select>
          </label>
        )}

        {error && <p className="collection-message shop-error" role="alert">{error.message}</p>}

        {friend && (
          <>
            <div className="trade-offers">
              <TradeSide
                title="La tua offerta"
                entries={offered}
                onRemove={(card) => changeOffered(removeCopy(offered, card))}
                emptyText="Scegli qui sotto le carte che dai."
              />
              <span className="trade-offers__swap" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M4 8h13l-3-3M20 16H7l3 3" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
              </span>
              <TradeSide
                title={`Offerta di ${friendName}`}
                entries={requested}
                onRemove={(card) => changeRequested(removeCopy(requested, card))}
                emptyText={`Scegli qui sotto le carte che chiedi a ${friendName}.`}
              />
            </div>

            <label className="deck-name trade-message">
              <span>Messaggio per {friendName} (facoltativo)</span>
              <input type="text" value={message} onChange={(event) => setMessage(event.target.value)} maxLength={255} placeholder="Ti va?" />
            </label>

            {warnings && (
              <div className="trade-confirm">
                <p>Prima di proporre:</p>
                <TradeWarnings warnings={warnings} />
              </div>
            )}

            <div className="trade-actions">
              <a className="home-button home-button--outlined" href="/scambi">Annulla</a>
              <button type="button" className="home-button home-button--filled" onClick={warnings ? propose : check} disabled={!canPropose}>
                {sending ? 'Attendi...' : warnings ? 'Proponi lo stesso' : 'Conferma offerta'}
              </button>
            </div>
            <p className="trade-actions__note">
              {canPropose || sending
                ? `Lo scambio avviene quando ${friendName} lo accetta.`
                : 'Serve almeno una carta per lato.'}
            </p>

            <section className="trade-pool" aria-label="Carte da scegliere">
              <div className="trade-pool__header">
                <div className="trade-pool__tabs" role="tablist">
                  <button type="button" role="tab" aria-selected={poolTab === 'mine'} className={poolTab === 'mine' ? 'is-active' : ''} onClick={() => setPoolTab('mine')}>
                    La tua collezione
                  </button>
                  <button type="button" role="tab" aria-selected={poolTab === 'friend'} className={poolTab === 'friend' ? 'is-active' : ''} onClick={() => setPoolTab('friend')}>
                    Le carte di {friendName}
                  </button>
                </div>
                <input className="trade-pool__search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Cerca..." maxLength={80} aria-label="Cerca una carta" />
              </div>

              {poolTab === 'mine' && myCards && (
                <TradePool
                  items={myCards}
                  chosen={offered}
                  onAdd={(entry) => changeOffered(addCopy(offered, entry.card, entry.availableForTrade))}
                  search={search}
                  chosenText="In offerta"
                />
              )}
              {poolTab === 'friend' && (friendCards ? (
                <TradePool
                  items={friendCards}
                  chosen={requested}
                  onAdd={(entry) => changeRequested(addCopy(requested, entry.card, entry.availableForTrade))}
                  search={search}
                  chosenText="Richiesta"
                />
              ) : (
                <p className="collection-message">Caricamento delle carte di {friendName}...</p>
              ))}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
