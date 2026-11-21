import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import Card from '../components/Carta/Card';
import CardZoom from '../components/Carta/CardZoom';
import { CARD_BACK_URL } from '../components/Carta/cardBack';
import FullscreenButton from '../components/SchermoIntero/FullscreenButton';
import useFullscreen from '../components/SchermoIntero/useFullscreen';
import { buildNames, combatCards, describeRound, discardTops, endReasonNames, sideNumbers, startFields } from '../components/Partita/matchLog.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Partita/Partita.css';

// Secondi tra un round e l'altro nella riproduzione automatica
const PLAY_DELAY_MS = 1600;

const phaseNames = { START: 'Inizio', COMBAT: 'Combattimento', END: 'Fine' };

// Carta sul campo o negli scarti: la carta montata se i dati completi sono arrivati
// (GET /cards/{cardId}), altrimenti un segnaposto con il nome dalla copia del mazzo
function FieldCard({ card, snapshot }) {
  if (card) return <Card card={card} />;
  return (
    <div className="arena-placeholder">
      <span>{snapshot?.name ?? 'Carta'}</span>
    </div>
  );
}

// Mazzo coperto: il retro delle carte con le carte che restano
function DeckPile({ count }) {
  return (
    <div className={`arena-pile${count === 0 ? ' is-empty' : ''}`}>
      {count > 0 ? <img src={CARD_BACK_URL} alt="" /> : <span className="arena-pile__empty">Mazzo finito</span>}
      <span className="arena-pile__count">Mazzo {count}</span>
    </div>
  );
}

// Nome e punti vita del giocatore, con la variazione del round
function PlayerPlate({ name, life, startingLifePoints, lifeDelta, isWinner }) {
  return (
    <div className={`arena-player${isWinner ? ' is-winner' : ''}`}>
      <div className="arena-player__head">
        <span className="arena-avatar" aria-hidden="true">{name.charAt(0).toUpperCase()}</span>
        <strong>{name}</strong>
      </div>
      <div className="arena-life">
        <strong>{life}</strong>
        <span>PV</span>
        {lifeDelta !== 0 && (
          <em className={lifeDelta < 0 ? 'is-loss' : 'is-gain'}>{lifeDelta > 0 ? '+' : ''}{lifeDelta}</em>
        )}
      </div>
      <div className="arena-lifebar" aria-hidden="true">
        {/* Gli effetti possono portare i punti vita sopra quelli iniziali: la barra si ferma al pieno */}
        <span style={{ width: `${Math.min(life / startingLifePoints, 1) * 100}%` }} />
      </div>
    </div>
  );
}

// Una metà del campo, come nel mockup dell'Arena: mazzo coperto, carta in gioco al centro e scarti
// con l'ultima carta girata di lato. L'avversario sta in alto, chi guarda in basso, a specchio.
function Side({ position, name, state, startingLifePoints, cardId, card, snapshot, numbers, isWinner, discard, onZoom }) {
  const eliminated = cardId && !state.cardId;
  const life = Math.max(state.lifePoints, 0);

  const deck = (
    <div className="arena-slot arena-slot--deck">
      <DeckPile count={state.cardsInDeck} />
      <PlayerPlate
        name={name}
        life={life}
        startingLifePoints={startingLifePoints}
        lifeDelta={numbers?.lifeDelta ?? 0}
        isWinner={isWinner}
      />
    </div>
  );

  const discardSlot = (
    <div className="arena-slot arena-slot--discard">
      <div className="arena-discard">
        {discard.cardId ? (
          <button
            type="button"
            className="arena-discard__card"
            onClick={() => onZoom({ cardId: discard.cardId, from: position, label: `Scartata da ${name}` })}
            aria-label={`Ultima carta negli scarti di ${name}: ${discard.snapshot?.name ?? 'carta'}. Ingrandisci`}
          >
            <FieldCard card={discard.card} snapshot={discard.snapshot} />
          </button>
        ) : (
          <span className="arena-discard__empty">Nessuno scarto</span>
        )}
      </div>
      <span className="arena-discard__count">Scarti {state.cardsInDiscard}</span>
    </div>
  );

  const active = (
    <div className="arena-slot arena-slot--active">
      {cardId ? (
        <>
          <button
            type="button"
            className={`arena-active${eliminated ? ' is-eliminated' : ''}`}
            onClick={() => onZoom({ cardId, from: position, label: `In campo per ${name}`, numbers })}
            aria-label={`Carta in campo di ${name}: ${snapshot?.name ?? card?.name ?? 'carta'}${eliminated ? ', eliminata' : ''}. Ingrandisci`}
          >
            <FieldCard card={card} snapshot={snapshot} />
            {eliminated && <span className="arena-active__stamp">Eliminata</span>}
            {numbers?.damageTaken > 0 && <span className="arena-active__damage">-{numbers.damageTaken}</span>}
          </button>
          <div className="arena-numbers" aria-label="Attacco e difesa in questo round">
            <span className="arena-number arena-number--attack"><small>ATT</small>{numbers?.attack ?? '-'}</span>
            <span className="arena-number arena-number--defense"><small>DIF</small>{numbers?.defense ?? '-'}</span>
          </div>
        </>
      ) : (
        <div className="arena-active arena-active--none">Nessuna carta in campo</div>
      )}
    </div>
  );

  // In alto: mazzo, carta, scarti. In basso, a specchio: scarti, carta, mazzo.
  return (
    <div className={`arena-row arena-row--${position}`}>
      {position === 'top' ? <>{deck}{active}{discardSlot}</> : <>{discardSlot}{active}{deck}</>}
    </div>
  );
}

// Riga sotto la carta ingrandita: ATT e DIF del round, se gli effetti li hanno cambiati
function zoomNote(zoom, card) {
  const { numbers } = zoom;
  if (!numbers || !card || (numbers.attack === card.attack && numbers.defense === card.defense)) return null;
  return `In questo round: ATT ${numbers.attack ?? '-'} · DIF ${numbers.defense ?? '-'}`;
}

// Icone dei comandi della riproduzione
const controlIcons = {
  start: 'M6 5v14M19 5 9 12l10 7z',
  back: 'M11 5 4 12l7 7M20 5l-7 7 7 7',
  play: 'M7 4.5v15l13-7.5z',
  pause: 'M7 5h3.5v14H7zM13.5 5H17v14h-3.5z',
  forward: 'M13 5l7 7-7 7M4 5l7 7-7 7',
  end: 'M18 5v14M5 5l10 7-10 7z',
  log: 'M5 6h14M5 12h14M5 18h9',
};

function ControlIcon({ name }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d={controlIcons[name]} />
    </svg>
  );
}

// Risultato e cronaca di una partita: /partite/{matchId} legge GET /matches/{matchId}.
// Ricaricare la pagina non fa giocare un'altra partita. Tutto sta in uno schermo: campo, comandi e
// risultato al centro del tavolo, cronaca in un pannello laterale; c'è anche lo schermo intero.
export default function MatchPage({ matchId }) {
  const [match, setMatch] = useState(null);
  const [me, setMe] = useState(null);
  // Le carte complete (illustrazione ed effetto), da GET /cards/{cardId}
  const [cardsById, setCardsById] = useState({});
  const [roundIndex, setRoundIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [logOpen, setLogOpen] = useState(false);
  // Carta ingrandita: { cardId, from: 'top' | 'bottom', label, numbers }
  const [zoom, setZoom] = useState(null);
  const [error, setError] = useState(null);
  const fullscreen = useFullscreen();

  useEffect(() => {
    api(`/matches/${matchId}`)
      .then((data) => {
        setMatch(data);
        // Illustrazioni ed effetti delle carte dei due mazzi (le copie dei mazzi hanno solo i dati di gioco)
        const ids = new Set([...(data.decks?.challenger?.cards ?? []), ...(data.decks?.opponent?.cards ?? [])].map((card) => card.cardId));
        ids.forEach((cardId) => {
          api(`/cards/${cardId}`)
            .then((card) => setCardsById((current) => ({ ...current, [cardId]: card })))
            .catch(() => {});
        });
      })
      .catch(setError);
    api('/me').then(setMe).catch(() => {});
  }, [matchId]);

  const rounds = useMemo(() => match?.rounds ?? [], [match]);
  const names = useMemo(() => (match ? buildNames(match) : null), [match]);
  const fields = useMemo(() => startFields(rounds), [rounds]);
  const discards = useMemo(() => discardTops(rounds), [rounds]);
  const lastIndex = rounds.length - 1;

  // Riproduzione automatica: un round dopo l'altro fino all'ultimo
  useEffect(() => {
    if (!playing) return undefined;
    const timer = setTimeout(() => {
      if (roundIndex < lastIndex) setRoundIndex(roundIndex + 1);
      else setPlaying(false);
    }, PLAY_DELAY_MS);
    return () => clearTimeout(timer);
  }, [playing, roundIndex, lastIndex]);

  const goTo = (index) => {
    setPlaying(false);
    setRoundIndex(Math.min(Math.max(index, 0), lastIndex));
  };

  const togglePlay = () => {
    if (!playing && roundIndex >= lastIndex) setRoundIndex(0);
    setPlaying((current) => !current);
  };

  // Tasti: spazio riproduce o mette in pausa, frecce per i round, Fine e Inizio per gli estremi.
  // Non quando si scrive o si usa un comando (lì i tasti fanno già il loro lavoro) o con la carta ingrandita.
  useEffect(() => {
    const onKey = (event) => {
      if (zoom || ['INPUT', 'BUTTON', 'SELECT', 'TEXTAREA', 'A'].includes(event.target.tagName)) return;
      if (event.key === ' ') { event.preventDefault(); togglePlay(); }
      else if (event.key === 'ArrowRight') goTo(roundIndex + 1);
      else if (event.key === 'ArrowLeft') goTo(roundIndex - 1);
      else if (event.key === 'End') goTo(lastIndex);
      else if (event.key === 'Home') goTo(0);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  if (error) {
    return (
      <div className="match-page match-page--message">
        <HomeHeader compact />
        <main className="decks-content">
          <p className="collection-message" role="alert">
            {error.code === 'MATCH_NOT_FOUND' || error.code === 'MALFORMED_REQUEST' ? 'Partita non trovata.' : error.message}
            {' '}<a href="/arena">Torna all'Arena</a>
          </p>
        </main>
      </div>
    );
  }

  if (!match || !names) {
    return (
      <div className="match-page match-page--message">
        <HomeHeader compact />
        <main className="decks-content"><p className="collection-message">Caricamento...</p></main>
      </div>
    );
  }

  const mySide = me?.id === match.challenger.id ? 'challenger' : me?.id === match.opponent.id ? 'opponent' : null;
  const myId = mySide ? match[mySide].id : null;
  let title = 'Pareggio';
  if (match.winnerId) {
    if (myId) title = match.winnerId === myId ? 'Hai vinto!' : 'Hai perso';
    else title = `Vince ${match.winnerId === match.challenger.id ? match.challenger.username : match.opponent.username}`;
  }

  const round = rounds[roundIndex];
  // Il risultato si scopre solo all'ultimo round, come alla fine di una partita vera
  const isFinished = roundIndex === lastIndex;
  const lines = round ? describeRound(round, names, fields[roundIndex]) : [];
  const fighters = round ? combatCards(round, fields[roundIndex]) : {};
  const previous = roundIndex > 0 ? rounds[roundIndex - 1].afterRound : null;

  // Chi guarda sta in basso, l'altro in alto (se non si sa chi guarda, lo sfidante in basso)
  const bottomKey = mySide ?? 'challenger';
  const topKey = bottomKey === 'challenger' ? 'opponent' : 'challenger';

  const sideProps = (key, position) => {
    const side = key === 'challenger' ? 'CHALLENGER' : 'OPPONENT';
    const discardId = discards[roundIndex]?.[side];
    return {
      position,
      name: match[key].username,
      state: round.afterRound[key],
      startingLifePoints: match.startingLifePoints,
      cardId: fighters[side],
      card: cardsById[fighters[side]],
      snapshot: names.cardData(fighters[side]),
      numbers: sideNumbers(round, side, fighters[side], previous ? previous[key].lifePoints : match.startingLifePoints, previous ? previous[key] : null),
      isWinner: isFinished && match.winnerId === match[key].id,
      discard: { cardId: discardId, card: cardsById[discardId], snapshot: names.cardData(discardId) },
      onZoom: setZoom,
    };
  };

  const rewards = (match.ratingChange && mySide) || match.creditsEarned > 0 || match.leagueReached;
  const outcome = !match.winnerId ? 'draw' : match.winnerId === myId ? 'win' : 'loss';

  return (
    <div className={`match-page${fullscreen.isFullscreen ? ' is-fullscreen' : ''}`}>
      <HomeHeader compact />

      <main className="match-stage">
        {round ? (
          <section className="arena-table" aria-labelledby="match-title">
            <div className="arena-corner">
              <button
                type="button"
                className={`match-log-toggle${logOpen ? ' is-active' : ''}`}
                onClick={() => setLogOpen(!logOpen)}
                aria-expanded={logOpen}
                aria-controls="match-log"
              >
                <ControlIcon name="log" />
                <span>Cronaca</span>
              </button>
              <FullscreenButton fullscreen={fullscreen} />
            </div>

            <Side {...sideProps(topKey, 'top')} />

            {/* A partita finita il risultato prende il posto del titolo (che resta per i lettori di schermo) */}
            <div className={`arena-center${isFinished ? ' is-finished' : ''}`}>
              <span className="dashboard-eyebrow">{match.matchType === 'RANKED' ? 'Partita classificata' : 'Partita rapida'}</span>
              <h1 id="match-title">{match[bottomKey].username} <span>contro</span> {match[topKey].username}</h1>

              {isFinished ? (
                <div className={`match-result match-result--${outcome}`} aria-live="polite">
                  <h2>{title}</h2>
                  <p>
                    {match.challenger.username} {match.finalLifePoints.challenger} PV · {match.opponent.username} {match.finalLifePoints.opponent} PV
                    {' · '}{match.totalRounds} round · {endReasonNames[match.endReason] ?? match.endReason}
                  </p>
                  {rewards && (
                    <p className="match-rewards">
                      {[
                        match.ratingChange && mySide && `Punteggio ${match.ratingChange[mySide] > 0 ? '+' : ''}${match.ratingChange[mySide]}`,
                        match.creditsEarned > 0 && `+${match.creditsEarned} DC`,
                        match.leagueReached && `Nuova lega: ${match.leagueReached.name}`,
                      ].filter(Boolean).join(' · ')}
                    </p>
                  )}
                  <div className="match-result__actions">
                    <a className="match-result__primary" href="/arena">Gioca ancora</a>
                    <button type="button" onClick={() => { setRoundIndex(0); setPlaying(true); }}>Rivedi</button>
                    <a href="/dashboard">Home</a>
                  </div>
                </div>
              ) : (
                <p className="arena-center__round" key={roundIndex}>Round {round.round} di {rounds.length} · scontro {round.clash}</p>
              )}

              <div className="match-controls" role="group" aria-label="Riproduzione della partita">
                <button type="button" onClick={() => goTo(0)} disabled={roundIndex === 0} aria-label="Primo round" title="Primo round">
                  <ControlIcon name="start" />
                </button>
                <button type="button" onClick={() => goTo(roundIndex - 1)} disabled={roundIndex === 0} aria-label="Round precedente" title="Round precedente (freccia sinistra)">
                  <ControlIcon name="back" />
                </button>
                <button type="button" className="match-controls__play" onClick={togglePlay} aria-label={playing ? 'Pausa' : 'Riproduci'} title={`${playing ? 'Pausa' : 'Riproduci'} (spazio)`}>
                  <ControlIcon name={playing ? 'pause' : 'play'} />
                </button>
                <button type="button" onClick={() => goTo(roundIndex + 1)} disabled={isFinished} aria-label="Round successivo" title="Round successivo (freccia destra)">
                  <ControlIcon name="forward" />
                </button>
                <button type="button" onClick={() => goTo(lastIndex)} disabled={isFinished} aria-label="Vai alla fine" title="Vai alla fine">
                  <ControlIcon name="end" />
                </button>
              </div>
              <input
                className="match-slider"
                type="range"
                min={0}
                max={lastIndex}
                value={roundIndex}
                onChange={(event) => goTo(Number(event.target.value))}
                aria-label={`Round ${round.round} di ${rounds.length}`}
              />
            </div>

            <Side {...sideProps(bottomKey, 'bottom')} />
          </section>
        ) : (
          <section className="decks-content">
            <h1 id="match-title">{match.challenger.username} contro {match.opponent.username}</h1>
          </section>
        )}

        {round && (
          <aside id="match-log" className={`match-drawer${logOpen ? ' is-open' : ''}`} aria-label="Cronaca della partita" aria-hidden={!logOpen}>
            <div className="match-drawer__head">
              <h2>Cronaca del round {round.round}</h2>
              <button type="button" onClick={() => setLogOpen(false)} aria-label="Chiudi la cronaca" tabIndex={logOpen ? 0 : -1}>×</button>
            </div>
            <ol className="match-log">
              {lines.map((line, position) => (
                <li key={position} className={`match-log__line match-log__line--${line.type.toLowerCase()}`}>
                  <span>{phaseNames[line.phase] ?? line.phase}</span>
                  {line.text}
                </li>
              ))}
            </ol>
            <div className="match-drawer__links">
              <a href="/dashboard" tabIndex={logOpen ? 0 : -1}>Home</a>
              <a href="/arena" tabIndex={logOpen ? 0 : -1}>Torna all'Arena</a>
              <a href="/mazzi" tabIndex={logOpen ? 0 : -1}>I tuoi mazzi</a>
            </div>
          </aside>
        )}
      </main>

      {zoom && (
        <CardZoom
          key={`${zoom.cardId}-${zoom.from}`}
          card={cardsById[zoom.cardId]}
          fallback={<FieldCard snapshot={names.cardData(zoom.cardId)} />}
          from={zoom.from}
          label={zoom.label}
          note={zoomNote(zoom, cardsById[zoom.cardId])}
          onClose={() => setZoom(null)}
        />
      )}
    </div>
  );
}
