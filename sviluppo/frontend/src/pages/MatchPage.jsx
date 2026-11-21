import { useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import Card from '../components/Carta/Card';
import { buildNames, combatCards, describeRound, discardTops, endReasonNames, sideNumbers, startFields } from '../components/Partita/matchLog.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Partita/Partita.css';

// Secondi tra un round e l'altro nella riproduzione automatica
const PLAY_DELAY_MS = 1600;

const phaseNames = { START: 'Inizio', COMBAT: 'Combattimento', END: 'Fine' };

// Retro delle carte (proposta 3 della presentazione, "il monolite sul lago")
const CARD_BACK_URL = '/cards/retro-monolite-v1.webp';

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

// Carta ingrandita: si apre premendo una carta sul campo o negli scarti. Quella dell'avversario
// arriva girandosi, come se la si prendesse dal suo lato del tavolo.
function CardZoom({ zoom, card, snapshot, onClose }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (zoom && dialog && !dialog.open) dialog.showModal();
  }, [zoom]);

  if (!zoom) return null;
  const { numbers } = zoom;
  const changed = numbers && card && (numbers.attack !== card.attack || numbers.defense !== card.defense);

  return (
    <dialog
      ref={dialogRef}
      className={`card-zoom card-zoom--${zoom.from}`}
      aria-label={card?.name ?? snapshot?.name ?? 'Carta'}
      onClose={onClose}
      // Un clic fuori dalla carta (sullo sfondo del dialog) lo chiude
      onClick={(event) => { if (event.target === event.currentTarget) dialogRef.current.close(); }}
    >
      <div className="card-zoom__body">
        <span className="card-zoom__label">{zoom.label}</span>
        <div className="card-zoom__card">
          {card ? <Card card={card} size="large" /> : <FieldCard snapshot={snapshot} />}
        </div>
        {changed && (
          <p className="card-zoom__numbers">In questo round: ATT {numbers.attack ?? '-'} · DIF {numbers.defense ?? '-'}</p>
        )}
        <button type="button" className="card-zoom__close" onClick={() => dialogRef.current.close()}>Chiudi</button>
      </div>
    </dialog>
  );
}

// Risultato e cronaca di una partita: /partite/{matchId} legge GET /matches/{matchId}.
// Ricaricare la pagina non fa giocare un'altra partita.
export default function MatchPage({ matchId }) {
  const [match, setMatch] = useState(null);
  const [me, setMe] = useState(null);
  // Le carte complete (illustrazione ed effetto), da GET /cards/{cardId}
  const [cardsById, setCardsById] = useState({});
  const [roundIndex, setRoundIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  // Carta ingrandita: { cardId, from: 'top' | 'bottom', label, numbers }
  const [zoom, setZoom] = useState(null);
  const [error, setError] = useState(null);

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

  // Riproduzione automatica: un round dopo l'altro fino all'ultimo
  useEffect(() => {
    if (!playing) return undefined;
    const timer = setTimeout(() => {
      if (roundIndex + 1 < rounds.length) setRoundIndex(roundIndex + 1);
      else setPlaying(false);
    }, PLAY_DELAY_MS);
    return () => clearTimeout(timer);
  }, [playing, roundIndex, rounds.length]);

  if (error) {
    return (
      <div className="match-page">
        <HomeHeader />
        <main className="decks-content">
          <p className="collection-message" role="alert">
            {error.code === 'MATCH_NOT_FOUND' || error.code === 'MALFORMED_REQUEST' ? 'Partita non trovata.' : error.message}
            {' '}<a href="/sfide">Torna alle sfide</a>
          </p>
        </main>
      </div>
    );
  }

  if (!match || !names) {
    return (
      <div className="match-page">
        <HomeHeader />
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
  const isFinished = roundIndex === rounds.length - 1;
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

  const goTo = (index) => {
    setPlaying(false);
    setRoundIndex(index);
  };

  return (
    <div className="match-page">
      <HomeHeader />

      <main>
        {round ? (
          <section className="arena-table" aria-labelledby="match-title">
            <Side {...sideProps(topKey, 'top')} />

            <div className="arena-center">
              <span className="dashboard-eyebrow">{match.matchType === 'RANKED' ? 'Partita classificata' : 'Partita rapida'}</span>
              <h1 id="match-title">{match[bottomKey].username} <span>contro</span> {match[topKey].username}</h1>
              <p className="arena-center__round" key={roundIndex}>Round {round.round} di {rounds.length} · scontro {round.clash}</p>
            </div>

            <Side {...sideProps(bottomKey, 'bottom')} />
          </section>
        ) : (
          <section className="decks-content">
            <h1 id="match-title">{match.challenger.username} contro {match.opponent.username}</h1>
          </section>
        )}

        <div className="decks-content match-content">
          {round && (
            <>
              <div className="match-controls">
                <button type="button" onClick={() => goTo(Math.max(roundIndex - 1, 0))} disabled={roundIndex === 0}>
                  Indietro
                </button>
                <button
                  type="button"
                  className="match-controls__play"
                  onClick={() => {
                    if (!playing && roundIndex + 1 >= rounds.length) setRoundIndex(0);
                    setPlaying(!playing);
                  }}
                >
                  {playing ? 'Pausa' : 'Riproduci'}
                </button>
                <button type="button" onClick={() => goTo(Math.min(roundIndex + 1, rounds.length - 1))} disabled={roundIndex + 1 >= rounds.length}>
                  Avanti
                </button>
                <button type="button" onClick={() => goTo(rounds.length - 1)} disabled={isFinished}>
                  Vai alla fine
                </button>
              </div>
              <label className="match-slider">
                <span>Round {round.round} di {rounds.length} · scontro {round.clash}</span>
                <input
                  type="range"
                  min={0}
                  max={rounds.length - 1}
                  value={roundIndex}
                  onChange={(event) => goTo(Number(event.target.value))}
                />
              </label>

              {isFinished && (
                <section className="match-result" aria-labelledby="result-title">
                  <span className="dashboard-eyebrow">Fine della partita</span>
                  <h2 id="result-title">{title}</h2>
                  <p>
                    {match.challenger.username} {match.finalLifePoints.challenger} PV · {match.opponent.username} {match.finalLifePoints.opponent} PV
                    {' · '}{match.totalRounds} round · {endReasonNames[match.endReason] ?? match.endReason}
                  </p>
                  {(match.ratingChange || match.creditsEarned > 0 || match.leagueReached) && (
                    <p className="match-rewards">
                      {match.ratingChange && mySide && `Punteggio ${match.ratingChange[mySide] > 0 ? '+' : ''}${match.ratingChange[mySide]}`}
                      {match.creditsEarned > 0 && ` · +${match.creditsEarned} DC`}
                      {match.leagueReached && ` · Nuova lega: ${match.leagueReached.name}`}
                    </p>
                  )}
                </section>
              )}

              <section className="dashboard-panel match-log" aria-labelledby="log-title">
                <h2 id="log-title">Cronaca del round {round.round}</h2>
                <ol>
                  {lines.map((line, position) => (
                    <li key={position} className={`match-log__line match-log__line--${line.type.toLowerCase()}`}>
                      <span>{phaseNames[line.phase] ?? line.phase}</span>
                      {line.text}
                    </li>
                  ))}
                </ol>
              </section>
            </>
          )}

          <div className="home-button-row match-links">
            <a className="home-button home-button--filled" href="/sfide">Torna alle sfide</a>
            <a className="home-button home-button--outlined" href="/mazzi">I tuoi mazzi</a>
          </div>
        </div>
      </main>

      <CardZoom
        key={zoom ? `${zoom.cardId}-${zoom.from}` : 'none'}
        zoom={zoom}
        card={zoom ? cardsById[zoom.cardId] : null}
        snapshot={zoom ? names.cardData(zoom.cardId) : null}
        onClose={() => setZoom(null)}
      />
    </div>
  );
}
