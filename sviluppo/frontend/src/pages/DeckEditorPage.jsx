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

const EMPTY_FILTERS = { faction: '', rarity: '', search: '' };

// Editor del mazzo come nel mockup: /mazzi/editor per un mazzo nuovo, /mazzi/editor?id=... per
// modificarne uno. A sinistra il mazzo (tocca una carta per toglierla), a destra la collezione
// (tocca una carta per aggiungerla). I limiti usano GET /rules; si salvano solo mazzi validi.
export default function DeckEditorPage() {
  const deckId = new URLSearchParams(window.location.search).get('id');
  const [rules, setRules] = useState(null);
  const [collection, setCollection] = useState(null);
  const [name, setName] = useState('');
  const [deckEntries, setDeckEntries] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [catalogSize, setCatalogSize] = useState(null);
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
    // Totale delle carte del catalogo, per "Possedute X/Y"
    api('/cards?size=1').then((page) => setCatalogSize(page.totalItems)).catch(() => {});
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
  const activeFilters = Object.values(filters).filter((value) => value.trim()).length;
  const nameError = saveError ? errorsByField(saveError).name : null;
  // Possedute X/Y come nella Collezione: carte normali diverse su quelle del catalogo
  const ownedNormal = (collection ?? []).filter(({ card }) => card.variantType !== 'FULL_ART').length;

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content">
        {loadError && <p className="collection-message" role="alert">{loadError.message}</p>}
        {!loadError && (!rules || !collection) && <p className="collection-message">Caricamento...</p>}

        {rules && collection && (
          <div className="deck-editor">
            {/* Sinistra: il mazzo, come nel mockup, con nome, limiti e salvataggio */}
            <section className="deck-editor__deck" aria-labelledby="deck-title">
              <div className="deck-editor__heading">
                <h1 id="deck-title">{deckId ? 'Modifica il tuo mazzo' : 'Costruisci il tuo mazzo'}</h1>
                <strong className={`deck-editor__total${total === rules.deckSize ? ' is-ok' : ''}`} aria-label={`${total} carte su ${rules.deckSize}`}>
                  {total}/{rules.deckSize}
                </strong>
              </div>

              <div className="deck-editor__bar">
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

                <ul className="deck-limits" aria-label="Limiti per rarità">
                  {rules.rarities.filter((rarity) => rarity.maxPerDeck != null).map((rarity) => {
                    const count = deckEntries
                      .filter((entry) => entry.card.rarity.code === rarity.code)
                      .reduce((sum, entry) => sum + entry.quantity, 0);
                    return (
                      <li key={rarity.code} className={count > rarity.maxPerDeck ? 'is-over' : ''}>
                        {rarityPlural[rarity.code] ?? rarity.name} <strong>{count}/{rarity.maxPerDeck}</strong>
                      </li>
                    );
                  })}
                </ul>

                <div className="deck-editor__actions">
                  <a className="deck-back" href="/mazzi">Annulla</a>
                  <button
                    type="button"
                    className="home-button home-button--filled deck-save"
                    onClick={save}
                    disabled={sending || problems.length > 0 || !name.trim()}
                  >
                    {sending ? 'Salvataggio...' : 'Salva il mazzo'}
                  </button>
                </div>
              </div>

              {total > 0 && problems.length > 0 && (
                <ul className="deck-problems">
                  {problems.map((problem) => <li key={problem}>{problem}</li>)}
                </ul>
              )}
              {problems.length === 0 && !name.trim() && <p className="deck-hint">Dai un nome al mazzo per salvarlo.</p>}

              {saveError && !nameError && (
                <div className="deck-problems" role="alert">
                  <p>{saveError.message}</p>
                  {saveError.problems.length > 1 && (
                    <ul>{saveError.problems.map((problem) => <li key={problem}>{problem}</li>)}</ul>
                  )}
                </div>
              )}

              {deckEntries.length === 0 ? (
                <p className="deck-editor__empty">Tocca le carte della collezione per metterle nel mazzo.</p>
              ) : (
                <>
                  <p className="deck-hint">Tocca una carta del mazzo per toglierne una copia.</p>
                  <div className="collection-grid deck-editor__grid">
                    {deckEntries.map((entry) => (
                      <CardGridItem
                        key={entry.card.id}
                        entry={entry}
                        isSelected={false}
                        onSelect={() => removeCopy(entry.card)}
                        label={`Togli una copia di ${entry.card.name} (nel mazzo: ${entry.quantity})`}
                      />
                    ))}
                  </div>
                </>
              )}
            </section>

            {/* Destra: la collezione da cui scegliere */}
            <section className="deck-editor__collection" aria-label="La tua collezione">
              <div className="collection-toolbar">
                <button
                  type="button"
                  className={`collection-filter-toggle${filtersOpen ? ' is-open' : ''}`}
                  aria-expanded={filtersOpen}
                  aria-controls="deck-filters"
                  onClick={() => setFiltersOpen(!filtersOpen)}
                >
                  Filtro{activeFilters > 0 && <span className="collection-filter-count">{activeFilters}</span>}
                </button>
                <p className="collection-owned">
                  Possedute <strong>{ownedNormal}{catalogSize != null && `/${catalogSize}`}</strong>
                </p>
              </div>

              {filtersOpen && (
                <form id="deck-filters" className="collection-filters" onSubmit={(event) => event.preventDefault()}>
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
                  {activeFilters > 0 && (
                    <button type="button" className="collection-filters-reset" onClick={() => setFilters(EMPTY_FILTERS)}>
                      Togli i filtri
                    </button>
                  )}
                </form>
              )}

              {collection.length === 0 && (
                <p className="collection-message">
                  Non hai ancora carte: <a href="/benvenuto">scegli la tua fazione</a> e ricevi il set iniziale.
                </p>
              )}
              {collection.length > 0 && visibleCollection.length === 0 && <p className="collection-message">Nessuna carta trovata.</p>}

              <div className="collection-grid deck-editor__grid">
                {visibleCollection.map((entry) => {
                  const inDeck = deckEntries.find((item) => item.card.id === entry.card.id)?.quantity ?? 0;
                  const canAdd = canAddCopy(entry.card, deckEntries, rules, owned);
                  return (
                    <div key={entry.card.id} className={`deck-tile${canAdd ? '' : ' is-maxed'}`}>
                      <CardGridItem
                        entry={entry}
                        isSelected={inDeck > 0}
                        onSelect={() => addCopy(entry.card)}
                        badge={inDeck > 0 ? `${inDeck}/${entry.quantity}` : `x${entry.quantity}`}
                        label={`Aggiungi ${entry.card.name} al mazzo (nel mazzo ${inDeck} su ${entry.quantity} copie)`}
                      />
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
