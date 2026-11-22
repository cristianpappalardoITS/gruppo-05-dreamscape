import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import CardZoom from '../components/Carta/CardZoom';
import { seriesFromLocation } from '../components/Negozio/buyPacks.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Benvenuto/Benvenuto.css';

// Ordine del riepilogo: prima le carte più rare, e tra queste le full art
const RARITY_ORDER = ['LEGENDARY', 'EPIC', 'RARE', 'COMMON'];
const rarityNames = { LEGENDARY: 'Leggendarie', EPIC: 'Epiche', RARE: 'Rare', COMMON: 'Comuni' };

// Riepilogo di più pacchetti comprati insieme (/negozio/riepilogo?serie=id1,id2,...): legge ogni
// apertura (GET /pack-openings/{openingId}) e mostra tutte le carte uscite, dalle più rare.
// Premendo una carta si ingrandisce e si gira.
export default function PackSeriesPage() {
  const [openings, setOpenings] = useState(null);
  const [error, setError] = useState(null);
  const [zoomCard, setZoomCard] = useState(null);

  useEffect(() => {
    // Senza serie nell'indirizzo Promise.all([]) dà subito una lista vuota
    Promise.all(seriesFromLocation().map((id) => api(`/pack-openings/${id}`))).then(setOpenings).catch(setError);
  }, []);

  const cards = useMemo(() => (openings ?? [])
    .flatMap((opening) => opening.cards.map(({ card }) => card))
    .sort((a, b) => RARITY_ORDER.indexOf(a.rarity.code) - RARITY_ORDER.indexOf(b.rarity.code)
      || Number(b.variantType === 'FULL_ART') - Number(a.variantType === 'FULL_ART')
      || a.code.localeCompare(b.code)), [openings]);

  const counts = useMemo(() => RARITY_ORDER
    .map((code) => ({ code, count: cards.filter((card) => card.rarity.code === code).length }))
    .filter(({ count }) => count > 0), [cards]);
  const fullArts = cards.filter((card) => card.variantType === 'FULL_ART').length;

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content series-content">
        {error && <p className="collection-message" role="alert">{error.message} <a href="/negozio">Torna al negozio</a></p>}
        {!error && !openings && <p className="collection-message">Caricamento...</p>}

        {openings && (
          <>
            <div className="welcome-intro">
              <span className="welcome-eyebrow">{openings.length} {openings.length === 1 ? 'pacchetto aperto' : 'pacchetti aperti'}</span>
              <h1>{openings[0]?.packName ?? 'Riepilogo'}</h1>
              <p>{cards.length} carte, già nella tua collezione.</p>
            </div>

            <ul className="series-counts" aria-label="Carte per rarità">
              {counts.map(({ code, count }) => (
                <li key={code} className={`series-counts__item series-counts__item--${code.toLowerCase()}`}>
                  <strong>{count}</strong> {rarityNames[code]}
                </li>
              ))}
              {fullArts > 0 && <li className="series-counts__item series-counts__item--fullart"><strong>{fullArts}</strong> Full art</li>}
            </ul>

            <div className="series-grid">
              {cards.map((card, position) => (
                <div className="reveal-card" style={{ '--reveal-delay': `${Math.min(position, 30) * 40}ms` }} key={`${card.id}-${position}`}>
                  <CardGridItem entry={{ card }} isSelected={false} onSelect={() => setZoomCard(card)} label={`${card.name}: ingrandisci`} />
                </div>
              ))}
            </div>

            <div className="welcome-confirm">
              <div className="pack-opening__end-actions">
                <a className="home-button home-button--filled" href="/negozio">Torna al negozio</a>
                <a className="home-button home-button--outlined" href="/collezione">Vai alla collezione</a>
              </div>
            </div>
          </>
        )}

        {zoomCard && <CardZoom card={zoomCard} label={zoomCard.rarity.name} onClose={() => setZoomCard(null)} />}
      </main>
    </div>
  );
}
