import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import PackOpening from '../components/Benvenuto/PackOpening';
import { isMuted, playOpen, playReveal, setMuted, unlock } from '../components/Benvenuto/slotSound.js';
import { CARD_BACK_URL } from '../components/Carta/cardBack';
import { BULK_COUNT, buyPacks, openingUrl, seriesFromLocation, seriesSummaryUrl } from '../components/Negozio/buyPacks.js';
import FullscreenButton from '../components/SchermoIntero/FullscreenButton';
import useFullscreen from '../components/SchermoIntero/useFullscreen';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Benvenuto/Benvenuto.css';

// Migliaia sempre con il punto (toLocaleString in italiano non lo mette sotto 10.000)
const credits = (value) => `${String(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.')} DC`;

// Apertura di un pacchetto comprato: /negozio/apertura/{openingId} legge GET /pack-openings/{openingId}.
// Con l'id dell'apertura nell'indirizzo, ricaricare la pagina non compra un altro pacchetto.
// Si parte dal pacchetto chiuso ("Apri il pacchetto", come la leva di una slot): il clic sblocca anche
// il suono, poi ogni carta esce con il giro dei rulli. Con più pacchetti comprati insieme
// (?serie=id1,id2,...) alla fine si passa al successivo e, dopo l'ultimo, al riepilogo.
// La pagina sta in uno schermo e ha lo schermo intero. Alla fine si può ricomprare lo stesso pacchetto.
export default function PackOpeningPage({ openingId }) {
  const [opening, setOpening] = useState(null);
  const [error, setError] = useState(null);
  // Il pacchetto in vendita con lo stesso nome (l'apertura ha solo il nome) e il saldo attuale
  const [pack, setPack] = useState(null);
  const [balance, setBalance] = useState(null);
  const [started, setStarted] = useState(false);
  const [muted, setMutedState] = useState(isMuted);
  const fullscreen = useFullscreen();

  const series = seriesFromLocation();
  const position = series.indexOf(openingId);
  const inSeries = series.length > 1 && position >= 0;
  const nextId = inSeries ? series[position + 1] : null;

  useEffect(() => {
    api(`/pack-openings/${openingId}`).then(setOpening).catch(setError);
    api('/me/summary').then((summary) => setBalance(summary.credits)).catch(() => {});
  }, [openingId]);

  // Il pacchetto si cerca tra quelli in vendita: il Welcome Pack non c'è, e allora niente ricompra
  useEffect(() => {
    if (!opening) return;
    api('/packs')
      .then((packs) => setPack(packs.find((item) => item.name === opening.packName) ?? null))
      .catch(() => {});
  }, [opening]);

  const onReveal = useCallback((card) => playReveal(card.rarity.code, card.variantType === 'FULL_ART'), []);

  const open = () => {
    unlock();
    playOpen();
    setStarted(true);
  };

  const toggleSound = () => {
    unlock();
    setMuted(!muted);
    setMutedState(!muted);
  };

  let nextLabel = 'Torna al negozio';
  let nextUrl = '/negozio';
  if (nextId) {
    nextLabel = `Pacchetto successivo (${position + 2} di ${series.length})`;
    nextUrl = `/negozio/apertura/${nextId}?serie=${series.join(',')}`;
  } else if (inSeries) {
    nextLabel = `Riepilogo dei ${series.length} pacchetti`;
    nextUrl = seriesSummaryUrl(series);
  }

  return (
    <div className={`opening-page${fullscreen.isFullscreen ? ' is-fullscreen' : ''}`}>
      <HomeHeader compact />

      <main className="opening-stage">
        <div className="opening-corner">
          <button
            type="button"
            className="sound-toggle"
            onClick={toggleSound}
            aria-pressed={!muted}
            aria-label={muted ? 'Attiva il suono' : 'Togli il suono'}
            title={muted ? 'Attiva il suono' : 'Togli il suono'}
          >
            <SoundIcon muted={muted} />
          </button>
          <FullscreenButton fullscreen={fullscreen} />
        </div>

        {error && (
          <p className="collection-message" role="alert">
            {error.code === 'PACK_OPENING_NOT_FOUND' || error.code === 'MALFORMED_REQUEST'
              ? 'Apertura non trovata.'
              : error.message}
            {' '}<a href="/negozio">Torna al negozio</a>
          </p>
        )}
        {!error && !opening && <p className="collection-message">Caricamento...</p>}

        {opening && !started && (
          <SealedPack
            name={opening.packName}
            image={pack?.imageUrl ?? CARD_BACK_URL}
            counter={inSeries ? `Pacchetto ${position + 1} di ${series.length}` : null}
            summaryUrl={inSeries ? seriesSummaryUrl(series) : null}
            onOpen={open}
          />
        )}

        {opening && started && (
          <PackOpening
            opening={opening}
            nextLabel={nextLabel}
            onDone={() => window.location.assign(nextUrl)}
            onReveal={onReveal}
            extraActions={pack && !nextId && <BuyAgain pack={pack} balance={balance} />}
          />
        )}
      </main>
    </div>
  );
}

// Pacchetto ancora chiuso: si apre con un clic (o un tocco). In una serie si può saltare al riepilogo
function SealedPack({ name, image, counter, summaryUrl, onOpen }) {
  return (
    <section className="sealed-pack" aria-labelledby="sealed-pack-title">
      {counter && <span className="welcome-eyebrow">{counter}</span>}
      <h1 id="sealed-pack-title">{name}</h1>
      <button type="button" className="sealed-pack__pack" onClick={onOpen} aria-label={`Apri il ${name}`}>
        <img src={image} alt="" />
      </button>
      <button type="button" className="home-button home-button--filled" onClick={onOpen}>Apri il pacchetto</button>
      {summaryUrl && <a className="sealed-pack__skip" href={summaryUrl}>Salta e guarda tutte le carte</a>}
    </section>
  );
}

// Icona dell'altoparlante, con o senza onde
function SoundIcon({ muted }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 9h4l5-4v14l-5-4H4z" fill="currentColor" />
      {muted
        ? <path d="M16 9l5 6M21 9l-5 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        : <path d="M16 8.5a5 5 0 0 1 0 7M18.5 6a8.5 8.5 0 0 1 0 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />}
    </svg>
  );
}

// Ricompra lo stesso pacchetto, uno o dieci (POST /packs/{packId}/open), con la conferma come
// nel negozio, poi apre la pagina della nuova apertura (o della serie)
function BuyAgain({ pack, balance }) {
  // Quanti pacchetti si stanno confermando: 0 = nessuna conferma aperta
  const [confirmCount, setConfirmCount] = useState(0);
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState(null);
  const canBuy = (count) => balance === null || balance >= pack.priceCredits * count;

  const buy = async () => {
    const count = confirmCount;
    setProgress(0);
    setError(null);
    const { openingIds, error: buyError } = await buyPacks(pack, count, setProgress);
    if (openingIds.length > 0) {
      window.location.assign(openingUrl(openingIds));
      return;
    }
    setError(buyError);
    setProgress(null);
    setConfirmCount(0);
  };

  if (!canBuy(1)) {
    return (
      <button type="button" className="home-button home-button--filled" disabled title={`Ti restano ${credits(balance)}`}>
        Crediti insufficienti per un altro {pack.name}
      </button>
    );
  }

  const buying = progress !== null;

  return (
    <div className="buy-again">
      {confirmCount > 0 ? (
        <>
          <button type="button" className="home-button home-button--filled" onClick={buy} disabled={buying}>
            {buying
              ? (confirmCount > 1 ? `Apertura ${progress + 1} di ${confirmCount}...` : 'Apertura...')
              : `Conferma ${confirmCount > 1 ? `${confirmCount} × ` : ''}· ${credits(pack.priceCredits * confirmCount)}`}
          </button>
          <button type="button" className="buy-again__cancel" onClick={() => setConfirmCount(0)} disabled={buying}>Annulla</button>
        </>
      ) : (
        <>
          <button type="button" className="home-button home-button--filled" onClick={() => setConfirmCount(1)}>
            Apri un altro {pack.name} · {credits(pack.priceCredits)}
          </button>
          <button
            type="button"
            className="home-button home-button--outlined buy-again__bulk"
            onClick={() => setConfirmCount(BULK_COUNT)}
            disabled={!canBuy(BULK_COUNT)}
            title={canBuy(BULK_COUNT) ? `${BULK_COUNT} pacchetti · ${credits(pack.priceCredits * BULK_COUNT)}` : 'Crediti insufficienti'}
          >
            × {BULK_COUNT}
          </button>
        </>
      )}
      {balance !== null && <span className="buy-again__balance">Ti restano {credits(balance)}</span>}
      {error && <span className="buy-again__error" role="alert">{error.message}</span>}
    </div>
  );
}
