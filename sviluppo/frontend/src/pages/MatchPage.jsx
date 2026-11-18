import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import { buildNames, combatCards, describeRound, endReasonNames, startFields } from '../components/Partita/matchLog.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Partita/Partita.css';

// Secondi tra un round e l'altro nella riproduzione automatica
const PLAY_DELAY_MS = 1600;

const phaseNames = { START: 'Inizio', COMBAT: 'Combattimento', END: 'Fine' };

// Un lato del campo: giocatore e punti vita a fine round, la carta che ha combattuto nel round
// (segnata se è stata eliminata), mazzo e scarti
function Side({ name, state, startingLifePoints, cardId, cardName, image, isWinner }) {
  const eliminated = cardId && !state.cardId;
  const life = Math.max(state.lifePoints, 0);
  return (
    <div className={`match-side${isWinner ? ' is-winner' : ''}`}>
      <div className="match-side__player">
        <strong>{name}</strong>
        <span>{life} PV</span>
      </div>
      <div className="match-life" aria-hidden="true">
        {/* Gli effetti possono portare i punti vita sopra quelli iniziali: la barra si ferma al pieno */}
        <span style={{ width: `${Math.min(life / startingLifePoints, 1) * 100}%` }} />
      </div>
      <div className={`match-field${eliminated ? ' is-eliminated' : ''}`}>
        {cardId ? (
          <>
            <div className="match-field__image">
              {image ? <img src={image.url} alt="" style={{ objectPosition: `center ${image.focusY * 100}%` }} /> : <span className="match-field__empty" />}
              {eliminated && <span className="match-field__badge">Eliminata</span>}
            </div>
            <p className="match-field__name">{cardName}</p>
            <p className="match-field__stats">{eliminated ? 'Va negli scarti' : `ATT ${state.attack} · DIF ${state.defense}`}</p>
          </>
        ) : (
          <p className="match-field__none">Nessuna creatura in campo</p>
        )}
      </div>
      <p className="match-side__piles">Mazzo {state.cardsInDeck} · Scarti {state.cardsInDiscard}</p>
    </div>
  );
}

// Risultato e cronaca di una partita: /partite/{matchId} legge GET /matches/{matchId}.
// Ricaricare la pagina non fa giocare un'altra partita.
export default function MatchPage({ matchId }) {
  const [match, setMatch] = useState(null);
  const [me, setMe] = useState(null);
  const [images, setImages] = useState({});
  const [roundIndex, setRoundIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api(`/matches/${matchId}`)
      .then((data) => {
        setMatch(data);
        // Illustrazioni delle carte dei due mazzi (le copie dei mazzi non le hanno)
        const ids = new Set([...(data.decks?.challenger?.cards ?? []), ...(data.decks?.opponent?.cards ?? [])].map((card) => card.cardId));
        ids.forEach((cardId) => {
          api(`/cards/${cardId}`)
            .then((card) => setImages((current) => ({
              ...current,
              [cardId]: card.illustrationThumbUrl ? { url: card.illustrationThumbUrl, focusY: card.illustrationFocusY } : null,
            })))
            .catch(() => {});
        });
      })
      .catch(setError);
    api('/me').then(setMe).catch(() => {});
  }, [matchId]);

  const rounds = useMemo(() => match?.rounds ?? [], [match]);
  const names = useMemo(() => (match ? buildNames(match) : null), [match]);
  const fields = useMemo(() => startFields(rounds), [rounds]);

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
  const lines = round ? describeRound(round, names, fields[roundIndex]) : [];
  const fighters = round ? combatCards(round, fields[roundIndex]) : {};
  const imageFor = (cardId) => (cardId ? images[cardId] : null);

  return (
    <div className="match-page">
      <HomeHeader />

      <main className="decks-content match-content">
        <section className="match-result" aria-labelledby="match-title">
          <span className="dashboard-eyebrow">{match.matchType === 'RANKED' ? 'Partita classificata' : 'Partita rapida'}</span>
          <h1 id="match-title">{title}</h1>
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

        {round && (
          <>
            <div className="match-arena">
              <Side
                name={match.challenger.username}
                state={round.afterRound.challenger}
                startingLifePoints={match.startingLifePoints}
                cardId={fighters.CHALLENGER}
                cardName={names.card(fighters.CHALLENGER)}
                image={imageFor(fighters.CHALLENGER)}
                isWinner={match.winnerId === match.challenger.id && roundIndex === rounds.length - 1}
              />
              <span className="match-versus">VS</span>
              <Side
                name={match.opponent.username}
                state={round.afterRound.opponent}
                startingLifePoints={match.startingLifePoints}
                cardId={fighters.OPPONENT}
                cardName={names.card(fighters.OPPONENT)}
                image={imageFor(fighters.OPPONENT)}
                isWinner={match.winnerId === match.opponent.id && roundIndex === rounds.length - 1}
              />
            </div>

            <div className="match-controls">
              <button type="button" onClick={() => { setPlaying(false); setRoundIndex(Math.max(roundIndex - 1, 0)); }} disabled={roundIndex === 0}>
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
              <button type="button" onClick={() => { setPlaying(false); setRoundIndex(Math.min(roundIndex + 1, rounds.length - 1)); }} disabled={roundIndex + 1 >= rounds.length}>
                Avanti
              </button>
            </div>
            <label className="match-slider">
              <span>Round {round.round} di {rounds.length} · scontro {round.clash}</span>
              <input
                type="range"
                min={0}
                max={rounds.length - 1}
                value={roundIndex}
                onChange={(event) => { setPlaying(false); setRoundIndex(Number(event.target.value)); }}
              />
            </label>

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
      </main>
    </div>
  );
}
