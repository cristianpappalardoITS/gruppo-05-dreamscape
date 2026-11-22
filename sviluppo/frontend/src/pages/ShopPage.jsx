import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import FactionEmblem from '../components/home/FactionEmblem';
import { BULK_COUNT, buyPacks, openingUrl } from '../components/Negozio/buyPacks.js';
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

// Negozio come nel mockup (GET /packs): il saldo (GET /me/summary), i pacchetti in vendita e
// l'acquisto con conferma (POST /packs/{packId}/open), poi l'apertura su /negozio/apertura/{openingId}
export default function ShopPage() {
  const [packs, setPacks] = useState(null);
  const [balance, setBalance] = useState(null);
  const [lordImages, setLordImages] = useState({});
  // Acquisto da confermare: { packId, count } (count 1 o BULK_COUNT)
  const [confirming, setConfirming] = useState(null);
  const [buyingPackId, setBuyingPackId] = useState(null);
  // Con più pacchetti: quanti ne sono già stati aperti
  const [progress, setProgress] = useState(0);
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

  // Uno o più pacchetti uguali, uno dopo l'altro; poi la pagina dell'apertura (o della serie).
  // Se un acquisto non riesce a metà si aprono comunque quelli già comprati
  const buy = async (pack, count) => {
    setBuyingPackId(pack.id);
    setProgress(0);
    setError(null);
    const { openingIds, error: buyError } = await buyPacks(pack, count, setProgress);
    if (openingIds.length > 0) {
      window.location.assign(openingUrl(openingIds));
      return;
    }
    setError(buyError);
    setBuyingPackId(null);
    setConfirming(null);
    // Pacchetto non più in vendita: si ricarica l'elenco
    if (buyError?.code === 'PACK_NOT_ON_SALE') loadPacks();
  };

  return (
    <div className="shop-page">
      <HomeHeader />

      <main className="decks-content shop-content">
        {/* Titolo e crediti sulla stessa riga: così la prima fila di pacchetti, con "Acquista",
            si vede senza scorrere anche su un portatile */}
        <div className="shop-top">
          <div className="shop-intro">
            <h1>Il tuo negozio</h1>
            <p>Acquista pacchetti di carte e controlla i tuoi crediti disponibili.</p>
          </div>

          {/* Crediti, come nel mockup. L'acquisto di crediti con denaro vero non c'è in questa fase */}
          <section className="shop-credits" aria-label="I tuoi crediti">
            <div className="shop-credits__balance">
              <span className="dc-coin dc-coin--large" aria-hidden="true">C</span>
              <div>
                <span>Crediti disponibili</span>
                <strong>{balance !== null ? credits(balance).replace(' DC', '') : '...'}</strong>
              </div>
            </div>
            <p>Usa i Dream Credits per i pacchetti. Ogni giorno, al primo accesso, ne ricevi 50 in regalo.</p>
            <button type="button" className="home-button home-button--filled shop-credits__buy" disabled title="Arriverà in una fase successiva">
              Acquista crediti · in arrivo
            </button>
          </section>
        </div>

        <h2 className="shop-section-title">Pacchetti disponibili</h2>

        {error && <p className="collection-message shop-error" role="alert">{error.message}</p>}
        {!error && !packs && <p className="collection-message">Caricamento pacchetti...</p>}

        {packs && (
          <div className="shop-grid">
            {packs.map((pack) => {
              const lord = pack.factionCode ? lordImages[pack.factionCode] : null;
              const image = pack.imageUrl ?? lord?.illustrationThumbUrl ?? '/artwork/dreamscape-worlds.webp';
              const tooExpensive = balance !== null && balance < pack.priceCredits;
              const bulkTooExpensive = balance !== null && balance < pack.priceCredits * BULK_COUNT;
              const confirmCount = confirming?.packId === pack.id ? confirming.count : 0;
              const isBuying = buyingPackId === pack.id;
              const cardCount = pack.slots.reduce((sum, slot) => sum + slot.cardCount, 0);

              return (
                <article key={pack.id} className={`shop-pack${pack.factionCode ? ` shop-pack--${pack.factionCode.toLowerCase()}` : ''}`}>
                  {/* L'immagine del pacchetto (verticale, sfondo trasparente) si vede intera;
                      l'illustrazione del Lord, di riserva, riempie il riquadro */}
                  <div className={`shop-pack__image${pack.imageUrl ? ' shop-pack__image--pack' : ''}`}>
                    <img
                      src={image}
                      alt=""
                      style={lord && !pack.imageUrl ? { objectPosition: `center ${lord.illustrationFocusY * 100}%` } : undefined}
                    />
                    {pack.factionCode && <FactionEmblem faction={pack.factionCode.toLowerCase()} />}
                  </div>

                  <div className="shop-pack__body">
                    <h3>{pack.name}</h3>
                    <p className="shop-pack__description">{pack.description}</p>
                    <div className="shop-pack__tags">
                      <span>{cardCount} carte</span>
                      {pack.availableUntil && <span>Fino al {new Date(pack.availableUntil).toLocaleDateString('it-IT')}</span>}
                    </div>
                    <details className="shop-pack__details">
                      <summary>Cosa contiene</summary>
                      <ul>
                        {slotDescriptions(pack).map((text) => <li key={text}>{text}</li>)}
                      </ul>
                    </details>
                  </div>

                  {/* Prezzo e acquisto sulla stessa riga */}
                  <div className="shop-pack__footer">
                    <div className="shop-pack__price">
                      <span className="dc-coin" aria-hidden="true">C</span>
                      <strong>{credits(pack.priceCredits).replace(' DC', '')}</strong>
                      <span className="visually-hidden">Dream Credits</span>
                    </div>

                    <div className="shop-pack__buy">
                      {confirmCount > 0 ? (
                        <>
                          <button type="button" className="shop-buy shop-buy--confirm" onClick={() => buy(pack, confirmCount)} disabled={isBuying}>
                            {isBuying
                              ? (confirmCount > 1 ? `Apertura ${Math.min(progress + 1, confirmCount)} di ${confirmCount}...` : 'Apertura...')
                              : (confirmCount > 1 ? `Conferma ${confirmCount} · ${credits(pack.priceCredits * confirmCount)}` : 'Conferma acquisto')}
                          </button>
                          <button type="button" className="shop-cancel" onClick={() => setConfirming(null)} disabled={isBuying}>
                            Annulla
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            type="button"
                            className="shop-buy"
                            onClick={() => setConfirming({ packId: pack.id, count: 1 })}
                            disabled={tooExpensive || buyingPackId !== null}
                          >
                            {tooExpensive ? 'Crediti insufficienti' : 'Acquista'}
                          </button>
                          {/* Dieci pacchetti uguali insieme: si aprono uno dopo l'altro, poi il riepilogo */}
                          {!tooExpensive && (
                            <button
                              type="button"
                              className="shop-buy shop-buy--bulk"
                              onClick={() => setConfirming({ packId: pack.id, count: BULK_COUNT })}
                              disabled={bulkTooExpensive || buyingPackId !== null}
                              aria-label={`Acquista ${BULK_COUNT} ${pack.name}`}
                              title={bulkTooExpensive ? 'Crediti insufficienti' : `${BULK_COUNT} pacchetti · ${credits(pack.priceCredits * BULK_COUNT)}`}
                            >
                              × {BULK_COUNT}
                            </button>
                          )}
                        </>
                      )}
                    </div>
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
