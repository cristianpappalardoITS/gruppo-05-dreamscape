import { useEffect, useState } from 'react';
import { api, logout } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import SelectedCardDetail from '../components/Collezione/SelectedCardDetail';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';

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

// Collezione dell'utente (GET /me/cards): griglia a pagine con filtri e dettaglio della carta scelta
export default function CollectionPage() {
  const [filters, setFilters] = useState({ faction: '', rarity: '', search: '' });
  const [page, setPage] = useState(0);
  const [collection, setCollection] = useState(null);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [error, setError] = useState(null);

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

  const hasFilters = Object.values(filters).some((value) => value.trim());

  const handleLogout = async () => {
    await logout().catch(() => {});
    window.location.assign('/');
  };

  return (
    <div className="collection-page">
      <HomeHeader />

      <main className="collection-content">
        <div className="collection-title-row">
          <h1>La tua collezione</h1>
          <button type="button" className="collection-logout" onClick={handleLogout}>Esci</button>
        </div>

        <form className="collection-filters" onSubmit={(event) => event.preventDefault()}>
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
          <label className="collection-search">
            <span>Nome</span>
            <input type="search" value={filters.search} onChange={changeFilter('search')} maxLength={80} placeholder="Cerca una carta" />
          </label>
        </form>

        {error && <p className="collection-message" role="alert">{error.message}</p>}
        {!error && !collection && <p className="collection-message">Caricamento collezione...</p>}

        {!error && collection && (
          <div className="collection-columns">
            <aside className="collection-detail-column">
              <SelectedCardDetail entry={selectedEntry} />
            </aside>

            <section className="collection-grid-column" aria-label="Carte possedute">
              <p className="collection-counter">
                {collection.totalItems === 1 ? '1 carta' : `${collection.totalItems} carte`}
              </p>

              {collection.items.length === 0 ? (
                <p className="collection-message">
                  {hasFilters
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
                      onSelect={setSelectedEntry}
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
