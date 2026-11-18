import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import SelectedCardDetail from '../components/Collezione/SelectedCardDetail';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';

// 4 colonne come nel mockup: 6 righe per pagina
const PAGE_SIZE = 24;

const factionOptions = [
  { code: '', name: 'Tutte le fazioni' },
  { code: 'DREAMS', name: 'Dreams' },
  { code: 'NIGHTMARES', name: 'Nightmares' },
  { code: 'MEMORIES', name: 'Memories' },
];

const rarityOptions = [
  { code: '', name: 'Tutte le rarità' },
  { code: 'COMMON', name: 'Comune' },
  { code: 'RARE', name: 'Rara' },
  { code: 'EPIC', name: 'Epica' },
  { code: 'LEGENDARY', name: 'Leggendaria' },
];

const versionOptions = [
  { code: '', name: 'Tutte le versioni' },
  { code: 'false', name: 'Solo normali' },
  { code: 'true', name: 'Solo full art' },
];

const EMPTY_FILTERS = { faction: '', rarity: '', fullArt: '', search: '' };

// Collezione dell'utente (GET /me/cards), come nel mockup: carta grande a sinistra, a destra
// il pulsante dei filtri, quante carte del catalogo si possiedono e la griglia a pagine
export default function CollectionPage() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [collection, setCollection] = useState(null);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [owned, setOwned] = useState(null);
  const [error, setError] = useState(null);
  const detailRef = useRef(null);

  // Possedute X/Y: carte normali diverse possedute su quelle del catalogo (basta totalItems)
  useEffect(() => {
    Promise.all([api('/me/cards?fullArt=false&size=1'), api('/cards?size=1')])
      .then(([mine, catalog]) => setOwned({ count: mine.totalItems, total: catalog.totalItems }))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const params = new URLSearchParams({ page, size: PAGE_SIZE });
    Object.entries(filters).forEach(([name, value]) => {
      if (value.trim()) params.set(name, value.trim());
    });

    let ignore = false;
    api(`/me/cards?${params}`)
      .then((data) => {
        if (ignore) return;
        setCollection(data);
        setError(null);
        setSelectedEntry(data.items[0] ?? null);
      })
      .catch((err) => {
        if (!ignore) setError(err);
      });
    return () => {
      ignore = true;
    };
  }, [filters, page]);

  // Un filtro nuovo riparte dalla prima pagina
  const changeFilter = (name) => (event) => {
    setFilters((current) => ({ ...current, [name]: event.target.value }));
    setPage(0);
  };

  const activeFilters = Object.values(filters).filter((value) => value.trim()).length;

  // Sul telefono la carta grande è sopra la griglia: dopo la scelta si torna a vederla
  const selectEntry = (entry) => {
    setSelectedEntry(entry);
    if (window.matchMedia('(max-width: 800px)').matches) {
      detailRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <div className="collection-page">
      <HomeHeader />

      <main className="collection-content">
        <h1 className="visually-hidden">La tua collezione</h1>

        {error && <p className="collection-message" role="alert">{error.message}</p>}
        {!error && !collection && <p className="collection-message">Caricamento collezione...</p>}

        {!error && collection && (
          <div className="collection-columns">
            <aside className="collection-detail-column" aria-label="Carta scelta" ref={detailRef}>
              <SelectedCardDetail entry={selectedEntry} />
            </aside>

            <section className="collection-grid-column" aria-label="Carte possedute">
              <div className="collection-toolbar">
                <button
                  type="button"
                  className={`collection-filter-toggle${filtersOpen ? ' is-open' : ''}`}
                  aria-expanded={filtersOpen}
                  aria-controls="collection-filters"
                  onClick={() => setFiltersOpen(!filtersOpen)}
                >
                  Filtro{activeFilters > 0 && <span className="collection-filter-count">{activeFilters}</span>}
                </button>

                {owned && (
                  <p className="collection-owned">
                    Possedute <strong>{owned.count}/{owned.total}</strong>
                  </p>
                )}
              </div>

              {filtersOpen && (
                <form id="collection-filters" className="collection-filters" onSubmit={(event) => event.preventDefault()}>
                  <label>
                    <span>Fazione</span>
                    <select value={filters.faction} onChange={changeFilter('faction')}>
                      {factionOptions.map(({ code, name }) => <option key={code} value={code}>{name}</option>)}
                    </select>
                  </label>
                  <label>
                    <span>Rarità</span>
                    <select value={filters.rarity} onChange={changeFilter('rarity')}>
                      {rarityOptions.map(({ code, name }) => <option key={code} value={code}>{name}</option>)}
                    </select>
                  </label>
                  <label>
                    <span>Versione</span>
                    <select value={filters.fullArt} onChange={changeFilter('fullArt')}>
                      {versionOptions.map(({ code, name }) => <option key={code} value={code}>{name}</option>)}
                    </select>
                  </label>
                  <label className="collection-search">
                    <span>Nome</span>
                    <input type="search" value={filters.search} onChange={changeFilter('search')} maxLength={80} placeholder="Cerca una carta" />
                  </label>
                  {activeFilters > 0 && (
                    <button type="button" className="collection-filters-reset" onClick={() => { setFilters(EMPTY_FILTERS); setPage(0); }}>
                      Togli i filtri
                    </button>
                  )}
                </form>
              )}

              {collection.items.length === 0 ? (
                <p className="collection-message">
                  {activeFilters > 0
                    ? 'Nessuna carta trovata.'
                    : <>Non hai ancora carte: <a href="/benvenuto">scegli la tua fazione</a> e ricevi il set iniziale.</>}
                </p>
              ) : (
                <div className="collection-grid">
                  {collection.items.map((entry) => (
                    <CardGridItem
                      key={entry.card.id}
                      entry={entry}
                      isSelected={selectedEntry?.card.id === entry.card.id}
                      onSelect={selectEntry}
                    />
                  ))}
                </div>
              )}

              {collection.totalPages > 1 && (
                <nav className="collection-pagination" aria-label="Pagine della collezione">
                  <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Indietro</button>
                  <span>Pagina {page + 1} di {collection.totalPages}</span>
                  <button type="button" disabled={page + 1 >= collection.totalPages} onClick={() => setPage(page + 1)}>Avanti</button>
                </nav>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
