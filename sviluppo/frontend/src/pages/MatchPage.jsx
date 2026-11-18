import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import { buildNames, combatCards, describeRound, endReasonNames, sideNumbers, startFields } from '../components/Partita/matchLog.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Partita/Partita.css';

// Secondi tra un round e l'altro nella riproduzione automatica
const PLAY_DELAY_MS = 1600;

const phaseNames = { START: 'Inizio', COMBAT: 'Combattimento', END: 'Fine' };

const factionNames = { DREAMS: 'Dreams', NIGHTMARES: 'Nightmares', MEMORIES: 'Memories' };

// Un lato del campo: il giocatore (punti vita a fine round e variazione nel round) e la carta
// che ha combattuto nel round, come una carta vera: cornice della fazione, illustrazione,
// effetto, ATT e DIF, danno subito. Se è morta nel round è grigia con il timbro "Eliminata".
function Side({ name, state, startingLifePoints, cardId, snapshot, card, numbers, isWinner, align }) {
  const eliminated = cardId && !state.cardId;
  const life = Math.max(state.lifePoints, 0);
  const faction = (card?.faction.code ?? snapshot?.faction ?? '').toLowerCase();
  const rarityName = card?.rarity.name ?? snapshot?.rarity;
  const effect = card?.effect;

  return (
    <div className={`arena-side arena-side--${align}${isWinner ? ' is-winner' : ''}`}>
      <header className="arena-player">
        <span className="arena-avatar" aria-hidden="true">{name.charAt(0).toUpperCase()}</span>
        <div className="arena-player__name">
          <strong>{name}</strong>
          <small>Mazzo {state.cardsInDeck} · Scarti {state.cardsInDiscard}</small>
        </div>
        <div className="arena-life">
          <strong>{life}</strong>
          <span>PV</span>
          {numbers && numbers.lifeDelta !== 0 && (
            <em className={numbers.lifeDelta < 0 ? 'is-loss' : 'is-gain'}>
              {numbers.lifeDelta > 0 ? '+' : ''}{numbers.lifeDelta}
            </em>
          )}
        </div>
      </header>
      <div className="arena-lifebar" aria-hidden="true">
        {/* Gli effetti possono portare i punti vita sopra quelli iniziali: la barra si ferma al pieno */}
        <span style={{ width: `${Math.min(life / startingLifePoints, 1) * 100}%` }} />
      </div>

      {cardId ? (
        <article className={`arena-card arena-card--${faction}${eliminated ? ' is-eliminated' : ''}`}>
          <div className="arena-card__art">
            {card?.illustrationThumbUrl
              ? <img src={card.illustrationThumbUrl} alt="" style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }} />
              : <span className="arena-card__empty" />}
            {eliminated && <span className="arena-card__stamp">Eliminata</span>}
            {numbers?.damageTaken > 0 && <span className="arena-card__damage">-{numbers.damageTaken}</span>}
          </div>
          <div className="arena-card__title">
            <h3>{snapshot?.name ?? card?.name}</h3>
            <small>{rarityName}{factionNames[snapshot?.faction] ? ` · ${factionNames[snapshot.faction]}` : ''}</small>
          </div>
          <div className="arena-card__effect">
            {effect ? (
              <>
                <strong>{effect.name}</strong>
                <p>{effect.text}</p>
              </>
            ) : (
              <p className="arena-card__no-effect">Nessun effetto</p>
            )}
          </div>
          <div className="arena-card__stats">
            <span className="arena-stat arena-stat--attack"><strong>{numbers?.attack ?? '-'}</strong><small>ATT</small></span>
            <span className="arena-stat arena-stat--defense"><strong>{numbers?.defense ?? '-'}</strong><small>DIF</small></span>
          </div>
        </article>
      ) : (
        <div className="arena-card arena-card--none">Nessuna creatura in campo</div>
      )}
    </div>
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
  const numbersFor = (side, key) => (round
    ? sideNumbers(round, side, fighters[side], previous ? previous[key].lifePoints : match.startingLifePoints, previous ? previous[key] : null)
    : null);

  return (
    <div className="match-page">
      <HomeHeader />

      <main className="decks-content match-content">
        <section className="match-intro" aria-labelledby="match-title">
          <span className="dashboard-eyebrow">{match.matchType === 'RANKED' ? 'Partita classificata' : 'Partita rapida'}</span>
          <h1 id="match-title">{match.challenger.username} contro {match.opponent.username}</h1>
          <p>Guarda la partita round per round: il risultato arriva alla fine.</p>
        </section>

        {round && (
          <>
            <div className="arena" key={roundIndex}>
              <Side
                align="left"
                name={match.challenger.username}
                state={round.afterRound.challenger}
                startingLifePoints={match.startingLifePoints}
                cardId={fighters.CHALLENGER}
                snapshot={names.cardData(fighters.CHALLENGER)}
                card={cardsById[fighters.CHALLENGER]}
                numbers={numbersFor('CHALLENGER', 'challenger')}
                isWinner={isFinished && match.winnerId === match.challenger.id}
              />
              <div className="arena-center" aria-hidden="true">
                <span className="arena-center__round">Round {round.round}</span>
                <span className="arena-center__vs">VS</span>
                <span className="arena-center__clash">Scontro {round.clash}</span>
              </div>
              <Side
                align="right"
                name={match.opponent.username}
                state={round.afterRound.opponent}
                startingLifePoints={match.startingLifePoints}
                cardId={fighters.OPPONENT}
                snapshot={names.cardData(fighters.OPPONENT)}
                card={cardsById[fighters.OPPONENT]}
                numbers={numbersFor('OPPONENT', 'opponent')}
                isWinner={isFinished && match.winnerId === match.opponent.id}
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
              <button type="button" onClick={() => { setPlaying(false); setRoundIndex(rounds.length - 1); }} disabled={isFinished}>
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
                onChange={(event) => { setPlaying(false); setRoundIndex(Number(event.target.value)); }}
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
      </main>
    </div>
  );
}
