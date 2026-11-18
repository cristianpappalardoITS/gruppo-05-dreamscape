import { useEffect, useMemo, useState } from 'react';
import { api, errorsByField } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import { loadAllCards } from '../components/Collezione/loadAllCards.js';
import { canAddCopy, deckProblems, rarityPlural } from '../components/Mazzi/deckRules.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';

const factionOptions = [
  { code: '', name: 'Tutte le fazioni' },
  { code: 'DREAMS', name: 'Dreams' },
  { code: 'NIGHTMARES', name: 'Nightmares' },
  { code: 'MEMORIES', name: 'Memories' },
];

// Editor del mazzo: /mazzi/editor per un mazzo nuovo, /mazzi/editor?id=... per modificarne uno.
// I contatori usano i numeri di GET /rules; si salvano solo mazzi validi (POST o PUT /decks).
export default function DeckEditorPage() {
  const deckId = new URLSearchParams(window.location.search).get('id');
  const [rules, setRules] = useState(null);
  const [collection, setCollection] = useState(null);
  const [name, setName] = useState('');
  const [deckEntries, setDeckEntries] = useState([]);
  const [filters, setFilters] = useState({ faction: '', rarity: '', search: '' });
  const [loadError, setLoadError] = useState(null);
  const [saveError, setSaveError] = useState(null);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    Promise.all([
      api('/rules'),
      loadAllCards('/me/cards'),
      deckId ? api(`/decks/${deckId}`) : Promise.resolve(null),
    ])
      .then(([rulesData, collectionItems, deck]) => {
        setRules(rulesData);
        setCollection(collectionItems);
        if (deck) {
          setName(deck.name);
          setDeckEntries(deck.cards.map(({ card, quantity }) => ({ card, quantity })));
        }
      })
      .catch(setLoadError);
  }, [deckId]);

  // Copie possedute per carta
  const owned = useMemo(
    () => Object.fromEntries((collection ?? []).map((entry) => [entry.card.id, entry.quantity])),
    [collection],
  );

  const problems = rules ? deckProblems(deckEntries, rules, owned) : [];
  const total = deckEntries.reduce((sum, entry) => sum + entry.quantity, 0);

  const addCopy = (card) => {
    if (!canAddCopy(card, deckEntries, rules, owned)) return;
    setDeckEntries((current) => {
      const existing = current.find((entry) => entry.card.id === card.id);
      if (existing) {
        return current.map((entry) => (entry.card.id === card.id ? { ...entry, quantity: entry.quantity + 1 } : entry));
      }
      return [...current, { card, quantity: 1 }].sort((a, b) => a.card.code.localeCompare(b.card.code));
    });
  };

  const removeCopy = (card) => {
    setDeckEntries((current) => current
      .map((entry) => (entry.card.id === card.id ? { ...entry, quantity: entry.quantity - 1 } : entry))
      .filter((entry) => entry.quantity > 0));
  };

  const save = async () => {
    setSending(true);
    setSaveError(null);
    const body = {
      name: name.trim(),
      cards: deckEntries.map(({ card, quantity }) => ({ cardId: card.id, quantity })),
    };
    try {
      await api(deckId ? `/decks/${deckId}` : '/decks', { method: deckId ? 'PUT' : 'POST', body });
      window.location.assign('/mazzi');
    } catch (err) {
      setSaveError(err);
      setSending(false);
    }
  };

  const visibleCollection = (collection ?? []).filter(({ card }) => (
    (!filters.faction || card.faction.code === filters.faction)
    && (!filters.rarity || card.rarity.code === filters.rarity)
    && (!filters.search.trim() || card.name.toLowerCase().includes(filters.search.trim().toLowerCase()))
  ));

  const changeFilter = (filterName) => (event) => setFilters((current) => ({ ...current, [filterName]: event.target.value }));
  const nameError = saveError ? errorsByField(saveError).name : null;

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content">
        <div className="collection-title-row">
          <h1>{deckId ? 'Modifica mazzo' : 'Nuovo mazzo'}</h1>
          <a className="deck-back" href="/mazzi">Torna ai mazzi</a>
        </div>

        {loadError && <p className="collection-message" role="alert">{loadError.message}</p>}
        {!loadError && (!rules || !collection) && <p className="collection-message">Caricamento...</p>}

        {rules && collection && (
          <div className="collection-columns">
            <aside className="collection-detail-column deck-panel">
              <label className="deck-name">
                <span>Nome del mazzo</span>
                <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  maxLength={40}
                  placeholder="Es. Incubi veloci"
                  aria-invalid={Boolean(nameError)}
                />
                {nameError && <small className="field-error">{nameError}</small>}
              </label>

              <div className="deck-counters">
                <div className={total === rules.deckSize ? 'is-ok' : ''}>
                  <span>Carte</span>
                  <strong>{total}/{rules.deckSize}</strong>
                </div>
                {rules.rarities.filter((rarity) => rarity.maxPerDeck != null).map((rarity) => {
                  const count = deckEntries
                    .filter((entry) => entry.card.rarity.code === rarity.code)
                    .reduce((sum, entry) => sum + entry.quantity, 0);
                  return (
                    <div key={rarity.code} className={count > rarity.maxPerDeck ? 'is-over' : ''}>
                      <span>{rarityPlural[rarity.code] ?? rarity.name}</span>
                      <strong>{count}/{rarity.maxPerDeck}</strong>
                    </div>
                  );
                })}
              </div>

              {deckEntries.length === 0 ? (
                <p className="collection-message">Tocca le carte della collezione per metterle nel mazzo.</p>
              ) : (
                <ul className="deck-entries">
                  {deckEntries.map(({ card, quantity }) => (
                    <li key={card.id} className={`deck-entry grid-card--${card.rarity.code.toLowerCase()}`}>
                      <span className="deck-entry__name">
                        {card.name}
                        <small>{card.rarity.name}{card.variantType === 'FULL_ART' ? ' · Full art' : ''}</small>
                      </span>
                      <span className="deck-entry__quantity">
                        <button type="button" onClick={() => removeCopy(card)} aria-label={`Togli una copia di ${card.name}`}>-</button>
                        <strong>{quantity}</strong>
                        <button
                          type="button"
                          onClick={() => addCopy(card)}
                          disabled={!canAddCopy(card, deckEntries, rules, owned)}
                          aria-label={`Aggiungi una copia di ${card.name}`}
                        >
                          +
                        </button>
                      </span>
                    </li>
                  ))}
                </ul>
              )}

              {total > 0 && problems.length > 0 && (
                <ul className="deck-problems">
                  {problems.map((problem) => <li key={problem}>{problem}</li>)}
                </ul>
              )}

              {saveError && !nameError && (
                <div className="deck-problems" role="alert">
                  <p>{saveError.message}</p>
                  {saveError.problems.length > 1 && (
                    <ul>{saveError.problems.map((problem) => <li key={problem}>{problem}</li>)}</ul>
                  )}
                </div>
              )}

              <button
                type="button"
                className="home-button home-button--filled deck-save"
                onClick={save}
                disabled={sending || problems.length > 0 || !name.trim()}
              >
                {sending ? 'Salvataggio...' : 'Salva il mazzo'}
              </button>
              {problems.length === 0 && !name.trim() && <p className="deck-hint">Dai un nome al mazzo per salvarlo.</p>}
            </aside>

            <section className="collection-grid-column" aria-label="La tua collezione">
              <form className="collection-filters" onSubmit={(event) => event.preventDefault()}>
                <label>
                  <span>Fazione</span>
                  <select value={filters.faction} onChange={changeFilter('faction')}>
                    {factionOptions.map(({ code, name: label }) => <option key={code} value={code}>{label}</option>)}
                  </select>
                </label>
                <label>
                  <span>Rarità</span>
                  <select value={filters.rarity} onChange={changeFilter('rarity')}>
                    <option value="">Tutte le rarità</option>
                    {rules.rarities.map((rarity) => <option key={rarity.code} value={rarity.code}>{rarity.name}</option>)}
                  </select>
                </label>
                <label className="collection-search">
                  <span>Nome</span>
                  <input type="search" value={filters.search} onChange={changeFilter('search')} maxLength={80} placeholder="Cerca una carta" />
                </label>
              </form>

              {collection.length === 0 && (
                <p className="collection-message">
                  Non hai ancora carte: <a href="/benvenuto">scegli la tua fazione</a> e ricevi il set iniziale.
                </p>
              )}

              <div className="collection-grid">
                {visibleCollection.map((entry) => {
                  const inDeck = deckEntries.find((item) => item.card.id === entry.card.id)?.quantity ?? 0;
                  const canAdd = canAddCopy(entry.card, deckEntries, rules, owned);
                  return (
                    <div key={entry.card.id} className={`deck-tile${canAdd ? '' : ' is-maxed'}`}>
                      <CardGridItem entry={entry} isSelected={inDeck > 0} onSelect={() => addCopy(entry.card)} />
                      {inDeck > 0 && <span className="deck-tile__in-deck">Nel mazzo: {inDeck}</span>}
                    </div>
                  );
                })}
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
