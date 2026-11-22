import { useEffect, useState } from 'react';
import { api } from '../api.js';
import AdminLayout from '../components/Admin/AdminLayout';
import CardEditor from '../components/Admin/CardEditor';
import { factionNames } from '../components/Admin/adminFormat';

const PAGE_SIZE = 20;
const emptyFilters = { search: '', faction: '', rarity: '', active: '', fullArt: '' };

// L'indirizzo di GET /admin/cards con i filtri scelti (quelli vuoti non si mandano)
function cardsPath(filters, page) {
  const params = new URLSearchParams({ page, size: PAGE_SIZE });
  Object.entries(filters).forEach(([name, value]) => {
    if (value !== '') params.set(name, value.trim());
  });
  return `/admin/cards?${params}`;
}

// Carte del backoffice (GET /admin/cards): tutte, anche full art e disattivate, con filtri e pagine.
// A destra la scheda per creare una carta o una full art e per modificarle (CardEditor).
export default function AdminCardsPage() {
  const [filters, setFilters] = useState(emptyFilters);
  const [applied, setApplied] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [cards, setCards] = useState(null);
  const [error, setError] = useState(null);
  const [options, setOptions] = useState(null);
  // La scheda aperta: { card } per modificare, { card: null, fullArt } per una carta nuova; null se chiusa
  const [editing, setEditing] = useState(null);
  const [notice, setNotice] = useState(null);

  // Le scelte del form: fazioni, rarità, effetti, espansioni e le Leggendarie che possono avere la full art
  useEffect(() => {
    Promise.all([
      api('/factions'),
      api('/admin/rarities'),
      api('/admin/effects'),
      api('/admin/card-sets'),
      api('/admin/cards?rarity=LEGENDARY&fullArt=false&size=100'),
      api('/admin/cards?fullArt=true&size=100'),
    ])
      .then(([factions, rarities, effects, cardSets, legendaries, fullArts]) => {
        const withFullArt = new Set(fullArts.items.map((card) => card.baseCardId));
        setOptions({
          factions,
          rarities,
          effects,
          cardSets,
          legendaries: legendaries.items.filter((card) => !withFullArt.has(card.id)),
        });
      })
      .catch(setError);
  }, []);

  useEffect(() => {
    api(cardsPath(applied, page)).then(setCards).catch(setError);
  }, [applied, page]);

  const rarityNames = Object.fromEntries((options?.rarities ?? []).map((rarity) => [rarity.code, rarity.name]));

  const setFilter = (name) => (event) => setFilters((current) => ({ ...current, [name]: event.target.value }));

  const search = (event) => {
    event.preventDefault();
    setPage(0);
    setApplied(filters);
  };

  const reset = () => {
    setFilters(emptyFilters);
    setApplied(emptyFilters);
    setPage(0);
  };

  // Dopo un salvataggio si rilegge la pagina (la carta può cambiare posto o uscire dai filtri) e la scheda
  // resta aperta sulla carta salvata. Una full art nuova toglie la sua carta base dalle scelte
  const saved = async (card, message) => {
    setNotice(message);
    setEditing({ card });
    if (card.variantType === 'FULL_ART') {
      setOptions((current) => ({ ...current, legendaries: current.legendaries.filter((item) => item.id !== card.baseCardId) }));
    }
    try {
      setCards(await api(cardsPath(applied, page)));
    } catch (err) {
      setError(err);
    }
  };

  const open = (state) => {
    setNotice(null);
    setEditing(state);
  };

  const actions = (
    <>
      <button type="button" className="admin-button admin-button--filled" onClick={() => open({ card: null, fullArt: false })} disabled={!options}>Nuova carta</button>
      <button type="button" className="admin-button" onClick={() => open({ card: null, fullArt: true })} disabled={!options}>Nuova full art</button>
    </>
  );

  return (
    <AdminLayout title="Carte" actions={actions}>
      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}
      {notice && <p className="admin-message admin-message--ok" role="status">{notice}</p>}

      <form className="collection-filters" onSubmit={search}>
        <label className="collection-search">
          Nome o codice
          <input type="search" value={filters.search} onChange={setFilter('search')} maxLength={80} placeholder="Es. Sogno o BASE-013" />
        </label>
        <label>
          Fazione
          <select value={filters.faction} onChange={setFilter('faction')}>
            <option value="">Tutte</option>
            {Object.entries(factionNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
          </select>
        </label>
        <label>
          Rarità
          <select value={filters.rarity} onChange={setFilter('rarity')}>
            <option value="">Tutte</option>
            {options?.rarities.map((rarity) => <option key={rarity.code} value={rarity.code}>{rarity.name}</option>)}
          </select>
        </label>
        <label>
          Stato
          <select value={filters.active} onChange={setFilter('active')}>
            <option value="">Tutte</option>
            <option value="true">Attive</option>
            <option value="false">Disattivate</option>
          </select>
        </label>
        <label>
          Versione
          <select value={filters.fullArt} onChange={setFilter('fullArt')}>
            <option value="">Tutte</option>
            <option value="false">Normali</option>
            <option value="true">Full art</option>
          </select>
        </label>
        <div className="admin-form-actions">
          <button type="submit" className="admin-button admin-button--filled">Cerca</button>
          <button type="button" className="admin-button" onClick={reset}>Azzera</button>
        </div>
      </form>

      <div className={editing ? 'admin-columns' : undefined}>
        <section aria-label="Elenco delle carte">
          {!cards && !error && <p className="collection-message">Caricamento delle carte...</p>}
          {cards && cards.items.length === 0 && <p className="admin-empty">Nessuna carta con questi filtri.</p>}
          {cards && cards.items.length > 0 && (
            <CardsTable cards={cards.items} rarityNames={rarityNames} selectedId={editing?.card?.id} onEdit={(card) => open({ card })} />
          )}
          {cards && cards.totalPages > 1 && (
            <div className="admin-pagination">
              <button type="button" className="admin-button admin-button--small" disabled={page === 0} onClick={() => setPage(page - 1)}>Precedenti</button>
              <span>Pagina {page + 1} di {cards.totalPages} · {cards.totalItems} carte</span>
              <button type="button" className="admin-button admin-button--small" disabled={page + 1 >= cards.totalPages} onClick={() => setPage(page + 1)}>Successive</button>
            </div>
          )}
        </section>

        {editing && options && (
          <CardEditor
            key={editing.card?.id ?? (editing.fullArt ? 'new-full-art' : 'new-card')}
            card={editing.card}
            fullArt={editing.fullArt}
            options={options}
            onSaved={saved}
            onClose={() => setEditing(null)}
          />
        )}
      </div>
    </AdminLayout>
  );
}

function CardsTable({ cards, rarityNames, selectedId, onEdit }) {
  return (
    <div className="admin-table-wrapper">
      <table className="admin-table">
        <thead>
          <tr>
            <th scope="col"><span className="visually-hidden">Illustrazione</span></th>
            <th scope="col">Codice</th>
            <th scope="col">Nome</th>
            <th scope="col" className="admin-optional">Fazione</th>
            <th scope="col" className="admin-optional">Rarità</th>
            <th scope="col" className="admin-number">ATT / DIF</th>
            <th scope="col" className="admin-number">Mazzi</th>
            <th scope="col">Stato</th>
            <th scope="col"><span className="visually-hidden">Azioni</span></th>
          </tr>
        </thead>
        <tbody>
          {cards.map((card) => {
            const isFullArt = card.variantType === 'FULL_ART';
            const thumb = card.illustrationThumbUrl || card.illustrationUrl;
            return (
              <tr key={card.id} className={`${card.id === selectedId ? 'is-selected' : ''}${card.isActive ? '' : ' is-off'}`}>
                <td>
                  {thumb
                    ? <img className="admin-thumb" src={thumb} alt="" style={{ objectPosition: `50% ${Number(card.illustrationFocusY ?? 0.4) * 100}%` }} />
                    : <span className="admin-thumb" />}
                </td>
                <td className="admin-code">{card.code}</td>
                <td>{isFullArt ? <em>Full art</em> : card.name}</td>
                <td className="admin-optional">{factionNames[card.factionCode] ?? '–'}</td>
                <td className="admin-optional">{card.rarityCode ? rarityNames[card.rarityCode] ?? card.rarityCode : '–'}</td>
                <td className="admin-number">{isFullArt ? '–' : `${card.attack} / ${card.defense}`}</td>
                <td className="admin-number">{card.deckCount}</td>
                <td>
                  <span className={`admin-status ${card.isActive ? 'admin-status--on' : 'admin-status--off'}`}>
                    {card.isActive ? 'Attiva' : 'Disattivata'}
                  </span>
                </td>
                <td>
                  <button type="button" className="admin-button admin-button--small" onClick={() => onEdit(card)}>Modifica</button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
