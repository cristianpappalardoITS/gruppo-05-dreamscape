import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import FactionEmblem from '../components/home/FactionEmblem';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Negozio/Negozio.css';

const rarityNames = {
  COMMON: ['Comune', 'Comuni'],
  RARE: ['Rara', 'Rare'],
  EPIC: ['Epica', 'Epiche'],
  LEGENDARY: ['Leggendaria', 'Leggendarie'],
};

const percent = (value) => `${value.toLocaleString('it-IT', { maximumFractionDigits: 1 })}%`;
// Migliaia sempre con il punto (toLocaleString in italiano non lo mette sotto 10.000)
const credits = (value) => `${String(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.')} DC`;

// Contenuto del pacchetto dagli slot di GET /packs, es. "3 Comuni · 1 Rara · 1 carta speciale: ..."
function slotDescriptions(pack) {
  return pack.slots.map((slot) => {
    const [only] = slot.rarities;
    if (slot.rarities.length === 1 && only.chance === 100) {
      const [singular, plural] = rarityNames[only.code] ?? [only.code, only.code];
      return `${slot.cardCount} ${slot.cardCount === 1 ? singular : plural}`;
    }
    const chances = slot.rarities.map((rarity) => `${(rarityNames[rarity.code] ?? [rarity.code])[0]} ${percent(rarity.chance)}`);
    const legendary = slot.rarities.find((rarity) => rarity.code === 'LEGENDARY');
    // fullArtChance è la parte delle Leggendarie che esce in full art
    const fullArt = legendary ? ` (full art ${percent((legendary.chance * pack.fullArtChance) / 100)})` : '';
    return `${slot.cardCount} ${slot.cardCount === 1 ? 'carta speciale' : 'carte speciali'}: ${chances.join(', ')}${fullArt}`;
  });
}

// Negozio (GET /packs): i pacchetti in vendita, il saldo (GET /me/summary) e l'acquisto
// (POST /packs/{packId}/open), poi l'apertura su /negozio/apertura/{openingId}
export default function ShopPage() {
  const [packs, setPacks] = useState(null);
  const [balance, setBalance] = useState(null);
  const [lordImages, setLordImages] = useState({});
  const [confirmingPackId, setConfirmingPackId] = useState(null);
  const [buyingPackId, setBuyingPackId] = useState(null);
  const [error, setError] = useState(null);

  const loadPacks = () => api('/packs').then(setPacks).catch(setError);

  useEffect(() => {
    loadPacks();
    api('/me/summary').then((summary) => setBalance(summary.credits)).catch(() => {});
    // Finché i pacchetti non hanno un'immagine, quelli di fazione mostrano il loro Lord
    api('/factions')
      .then((factions) => factions.filter((faction) => faction.lordCardId).forEach((faction) => {
        api(`/cards/${faction.lordCardId}`)
          .then((card) => setLordImages((current) => ({ ...current, [faction.code]: card })))
          .catch(() => {});
      }))
      .catch(() => {});
  }, []);

  const buy = async (pack) => {
    setBuyingPackId(pack.id);
    setError(null);
    try {
      const opening = await api(`/packs/${pack.id}/open`, { method: 'POST' });
      window.location.assign(`/negozio/apertura/${opening.openingId}`);
    } catch (err) {
      setError(err);
      setBuyingPackId(null);
      setConfirmingPackId(null);
      // Pacchetto non più in vendita: si ricarica l'elenco
      if (err.code === 'PACK_NOT_ON_SALE') loadPacks();
    }
  };

  return (
    <div className="shop-page">
      <HomeHeader />

      <main className="decks-content shop-content">
        <div className="collection-title-row">
          <h1>Negozio</h1>
          {balance !== null && <span className="shop-balance">Saldo: <strong>{credits(balance)}</strong></span>}
        </div>

        {error && <p className="collection-message shop-error" role="alert">{error.message}</p>}
        {!error && !packs && <p className="collection-message">Caricamento pacchetti...</p>}

        {packs && (
          <div className="shop-grid">
            {packs.map((pack) => {
              const lord = pack.factionCode ? lordImages[pack.factionCode] : null;
              const image = pack.imageUrl ?? lord?.illustrationThumbUrl ?? '/artwork/dreamscape-worlds.webp';
              const tooExpensive = balance !== null && balance < pack.priceCredits;
              const isConfirming = confirmingPackId === pack.id;
              const isBuying = buyingPackId === pack.id;

              return (
                <article key={pack.id} className={`shop-pack${pack.factionCode ? ` shop-pack--${pack.factionCode.toLowerCase()}` : ''}`}>
                  <div className="shop-pack__image">
                    <img
                      src={image}
                      alt=""
                      style={lord && !pack.imageUrl ? { objectPosition: `center ${lord.illustrationFocusY * 100}%` } : undefined}
                    />
                    {pack.factionCode && <FactionEmblem faction={pack.factionCode.toLowerCase()} />}
                  </div>

                  <div className="shop-pack__body">
                    <h2>{pack.name}</h2>
                    <p className="shop-pack__description">{pack.description}</p>
                    <ul className="shop-pack__slots">
                      {slotDescriptions(pack).map((text) => <li key={text}>{text}</li>)}
                    </ul>
                    {pack.availableUntil && (
                      <p className="shop-pack__until">
                        In vendita fino al {new Date(pack.availableUntil).toLocaleDateString('it-IT')}
                      </p>
                    )}
                  </div>

                  <div className="shop-pack__buy">
                    {isConfirming ? (
                      <>
                        <button type="button" className="home-button home-button--filled" onClick={() => buy(pack)} disabled={isBuying}>
                          {isBuying ? 'Apertura...' : `Conferma · ${credits(pack.priceCredits)}`}
                        </button>
                        <button type="button" className="shop-cancel" onClick={() => setConfirmingPackId(null)} disabled={isBuying}>
                          Annulla
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="home-button home-button--filled"
                        onClick={() => setConfirmingPackId(pack.id)}
                        disabled={tooExpensive || buyingPackId !== null}
                      >
                        {tooExpensive ? `Servono ${credits(pack.priceCredits)}` : `Acquista · ${credits(pack.priceCredits)}`}
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
