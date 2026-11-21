import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import '../styles/home.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Arena/Arena.css';

// Tempo minimo della ricerca dell'avversario: il server risponde subito con la partita già giocata,
// ma senza una piccola attesa il passaggio alla partita sembrerebbe un errore
const MIN_SEARCH_MS = 1800;
// Dopo "Avversario trovato" si resta un attimo sul nome prima di aprire la partita
const FOUND_PAUSE_MS = 900;

const searchMessages = {
  ranked: ['Cerco un avversario del tuo livello...', 'Allargo la ricerca...', 'Preparo il campo...'],
  casual: ['Cerco un avversario...', 'Mescolo i mazzi...', 'Preparo il campo...'],
};

const resultNames = { WIN: 'Vittoria', LOSS: 'Sconfitta', DRAW: 'Pareggio' };
const typeNames = { RANKED: 'Classificata', CASUAL: 'Rapida', CHALLENGE: 'Sfida' };

const formatDate = (iso) => new Date(iso).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' });

// Fazione più presente nel mazzo: dà il colore alla scheda del mazzo
function deckFaction(deck) {
  const counts = {};
  deck.cards.forEach(({ card, quantity }) => {
    counts[card.faction.code] = (counts[card.faction.code] ?? 0) + (quantity ?? 1);
  });
  return Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0]?.toLowerCase() ?? 'dreams';
}

// Anello del punteggio: si riempie verso la lega successiva
function RankRing({ rating, progress, leagueName }) {
  const radius = 54;
  const length = 2 * Math.PI * radius;
  return (
    <div className="arena-rank" role="img" aria-label={`${rating} punti, lega ${leagueName}`}>
      <svg viewBox="0 0 128 128" aria-hidden="true">
        <defs>
          <linearGradient id="arena-rank-gradient" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#f1cf9a" />
            <stop offset="100%" stopColor="#b8844e" />
          </linearGradient>
        </defs>
        <circle className="arena-rank__track" cx="64" cy="64" r={radius} />
        <circle
          className="arena-rank__fill"
          cx="64"
          cy="64"
          r={radius}
          stroke="url(#arena-rank-gradient)"
          strokeDasharray={length}
          strokeDashoffset={length * (1 - progress)}
        />
      </svg>
      <div className="arena-rank__value">
        <strong>{rating}</strong>
        <span>punti</span>
      </div>
    </div>
  );
}

// Scala delle leghe (GET /leagues): quelle superate piene, la propria illuminata
function LeagueLadder({ leagues, rating, currentCode }) {
  const currentIndex = leagues.findIndex((league) => league.code === currentCode);
  return (
    <ol className="arena-ladder" aria-label="Le leghe">
      {leagues.map((league, index) => (
        <li
          key={league.code}
          className={`${index < currentIndex ? 'is-passed' : ''}${index === currentIndex ? ' is-current' : ''}`}
          aria-current={index === currentIndex ? 'step' : undefined}
        >
          <span className="arena-ladder__gem" aria-hidden="true" />
          <strong>{league.name}</strong>
          <small>{league.minRating > 0 ? `da ${league.minRating}` : 'si parte da qui'}</small>
          {league.rewardCredits > 0 && <small className="arena-ladder__reward">+{league.rewardCredits} DC</small>}
          {index === currentIndex && index + 1 < leagues.length && (
            <small className="arena-ladder__missing">ne mancano {Math.max(leagues[index + 1].minRating - rating, 0)}</small>
          )}
        </li>
      ))}
    </ol>
  );
}

// Portale della ricerca: anelli che girano intorno al simbolo di Dreamscape
function SearchPortal({ search }) {
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (search.opponent) return undefined;
    const timer = setInterval(() => setStep((current) => current + 1), 900);
    return () => clearInterval(timer);
  }, [search.opponent]);

  const messages = searchMessages[search.mode];
  return (
    <div className={`arena-portal arena-portal--${search.mode}${search.opponent ? ' is-found' : ''}`} role="status" aria-live="polite">
      <div className="arena-portal__rings" aria-hidden="true">
        <span />
        <span />
        <span />
        <img src="/artwork/dreamscape-symbol.svg" alt="" />
      </div>
      <p className="arena-portal__eyebrow">{search.mode === 'ranked' ? 'Partita classificata' : 'Partita rapida'}</p>
      <p className="arena-portal__text">
        {search.opponent ? <>Avversario trovato: <strong>{search.opponent}</strong></> : messages[step % messages.length]}
      </p>
    </div>
  );
}

// Arena: da qui si gioca. Classificata e rapida (POST /matches/ranked e /casual, il server gioca subito
// e la partita si apre su /partite/{id}), sfide tra amici (/sfide), il mazzo per giocare e il mazzo
// classificato (PUT /me/ranked-deck), punteggio e leghe (GET /me/summary, GET /leagues), ultime partite.
export default function ArenaPage() {
  const [summary, setSummary] = useState(null);
  const [leagues, setLeagues] = useState([]);
  const [decks, setDecks] = useState(null);
  const [recent, setRecent] = useState(null);
  const [deckId, setDeckId] = useState('');
  const [search, setSearch] = useState(null);
  const [savingRanked, setSavingRanked] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const loadDecks = () => api('/decks').then(setDecks).catch(setError);
  const loadSummary = () => api('/me/summary').then(setSummary).catch(setError);

  useEffect(() => {
    loadSummary();
    loadDecks();
    api('/leagues').then(setLeagues).catch(() => {});
    api('/me/matches?page=0&size=5').then((data) => setRecent(data.items)).catch(() => setRecent([]));
  }, []);

  const validDecks = useMemo(() => (decks ?? []).filter((deck) => deck.isValid), [decks]);
  const rankedDeck = (decks ?? []).find((deck) => deck.isRanked);
  // Il mazzo scelto: quello dell'utente, altrimenti il classificato se è valido, altrimenti il primo valido
  const chosenDeck = validDecks.find((deck) => deck.id === deckId)
    ?? (rankedDeck?.isValid ? rankedDeck : null)
    ?? validDecks[0];

  // Avanzamento verso la lega successiva, da 0 a 1
  const progress = useMemo(() => {
    if (!summary) return 0;
    const current = leagues.find((league) => league.code === summary.league.code);
    const nextMin = summary.league.nextLeagueMinRating;
    if (!nextMin) return 1;
    const from = current?.minRating ?? 0;
    return Math.min(Math.max((summary.rating - from) / (nextMin - from), 0), 1);
  }, [summary, leagues]);

  const play = async (mode) => {
    if (!chosenDeck || search) return;
    setError(null);
    setMessage(null);
    setSearch({ mode, opponent: null });
    const started = Date.now();
    try {
      const match = await api(`/matches/${mode}`, { method: 'POST', body: { deckId: chosenDeck.id } });
      await new Promise((resolve) => setTimeout(resolve, Math.max(MIN_SEARCH_MS - (Date.now() - started), 0)));
      setSearch({ mode, opponent: match.opponent.username });
      await new Promise((resolve) => setTimeout(resolve, FOUND_PAUSE_MS));
      window.location.assign(`/partite/${match.id}`);
    } catch (err) {
      await new Promise((resolve) => setTimeout(resolve, Math.max(900 - (Date.now() - started), 0)));
      setSearch(null);
      setError(err);
    }
  };

  const makeRanked = async (deck) => {
    setSavingRanked(true);
    setError(null);
    try {
      await api('/me/ranked-deck', { method: 'PUT', body: { deckId: deck.id } });
      setMessage(`${deck.name} è il tuo mazzo classificato: con questo gli altri ti trovano anche quando non sei collegato.`);
      await Promise.all([loadDecks(), loadSummary()]);
    } catch (err) {
      setError(err);
    } finally {
      setSavingRanked(false);
    }
  };

  const errorText = error && ({
    NO_OPPONENT_FOUND: 'Nessun avversario disponibile, riprova tra poco.',
    ENGINE_UNAVAILABLE: 'Partita non disponibile, riprova tra poco.',
  }[error.code] ?? error.message);

  const noDecks = decks && validDecks.length === 0;

  return (
    <div className="arena-page">
      <HomeHeader />

      <main>
        <section className="arena-hero" aria-labelledby="arena-title">
          <div className="arena-hero__text">
            <span className="dashboard-eyebrow">Il tavolo dei tre regni</span>
            <h1 id="arena-title">Arena</h1>
            <p>Scegli il mazzo e come sfidare gli altri Dreamweaver: la partita si gioca subito, poi la guardi round per round.</p>
          </div>

          {summary && (
            <div className="arena-hero__rank">
              <div className="arena-hero__rank-top">
                <RankRing rating={summary.rating} progress={progress} leagueName={summary.league.name} />
                <div className="arena-hero__league">
                  <span>Lega</span>
                  <strong>{summary.league.name}</strong>
                  <small>
                    {summary.league.nextLeagueName
                      ? `${Math.max(summary.league.nextLeagueMinRating - summary.rating, 0)} punti per ${summary.league.nextLeagueName}`
                      : 'Sei nella lega più alta'}
                  </small>
                </div>
              </div>
              {leagues.length > 0 && (
                <LeagueLadder leagues={leagues} rating={summary.rating} currentCode={summary.league.code} />
              )}
            </div>
          )}
        </section>

        <div className="arena-content">
          {message && <p className="arena-message" role="status">{message}</p>}
          {error && (
            <div className="arena-error" role="alert">
              {errorText}
              {error.problems?.length > 0 && <ul>{error.problems.map((problem) => <li key={problem}>{problem}</li>)}</ul>}
            </div>
          )}

          <section className="arena-decks" aria-labelledby="arena-decks-title">
            <div className="arena-section-head">
              <h2 id="arena-decks-title">Il mazzo per giocare</h2>
              <a href="/mazzi">Gestisci i mazzi</a>
            </div>

            {!decks && <p className="arena-muted">Caricamento mazzi...</p>}
            {noDecks && (
              <p className="arena-muted">
                Per giocare serve un mazzo valido di 20 carte. <a href="/mazzi/editor">Componi un mazzo</a>
              </p>
            )}

            {/* Mazzi e nota sul mazzo classificato sulla stessa riga (vanno a capo sugli schermi stretti) */}
            <div className="arena-deck-row">
              {validDecks.length > 0 && (
                <div className="arena-deck-list" role="radiogroup" aria-label="Mazzo per giocare">
                  {validDecks.map((deck) => {
                    const isChosen = chosenDeck?.id === deck.id;
                    return (
                      <button
                        key={deck.id}
                        type="button"
                        role="radio"
                        aria-checked={isChosen}
                        className={`arena-deck arena-deck--${deckFaction(deck)}${isChosen ? ' is-chosen' : ''}`}
                        onClick={() => setDeckId(deck.id)}
                      >
                        <span className="arena-deck__fan" aria-hidden="true">
                          {deck.cards.slice(0, 3).map(({ card }) => (
                            card.illustrationThumbUrl
                              ? <img key={card.id} src={card.illustrationThumbUrl} alt="" style={{ objectPosition: `center ${card.illustrationFocusY * 100}%` }} />
                              : <span key={card.id} />
                          ))}
                        </span>
                        <span className="arena-deck__name">{deck.name}</span>
                        {deck.isRanked && <span className="arena-deck__badge">Classificato</span>}
                      </button>
                    );
                  })}
                </div>
              )}

              {decks && (
                <div className="arena-ranked-note">
                  {rankedDeck && rankedDeck.isValid && (
                    <p>
                      Mazzo classificato: <strong>{rankedDeck.name}</strong>. È quello che usi quando qualcuno ti sceglie
                      come avversario, anche se non sei collegato.
                    </p>
                  )}
                  {rankedDeck && !rankedDeck.isValid && (
                    <p className="is-warning">
                      Il tuo mazzo classificato <strong>{rankedDeck.name}</strong> non è valido: nessuno può sceglierti come
                      avversario finché non lo <a href={`/mazzi/editor?id=${rankedDeck.id}`}>sistemi</a> o ne scegli un altro.
                    </p>
                  )}
                  {!rankedDeck && validDecks.length > 0 && (
                    <p className="is-warning">
                      Non hai un mazzo classificato: senza, gli altri non possono sceglierti come avversario.
                    </p>
                  )}
                  {chosenDeck && !chosenDeck.isRanked && (
                    <button type="button" className="arena-link-button" onClick={() => makeRanked(chosenDeck)} disabled={savingRanked}>
                      Usa {chosenDeck.name} come mazzo classificato
                    </button>
                  )}
                </div>
              )}
            </div>
          </section>

          <section className="arena-modes" aria-label="Modalità di gioco">
            <article className="arena-mode arena-mode--ranked">
              <div className="arena-mode__art" aria-hidden="true" />
              <div className="arena-mode__body">
                <span className="arena-mode__kicker">Rank Elo</span>
                <h2>Classificata</h2>
                <p>Un avversario con un punteggio vicino al tuo. Vinci punti per salire di lega.</p>
                <ul className="arena-mode__tags">
                  <li>+30 DC per vittoria</li>
                  <li>3 vittorie premiate al giorno</li>
                </ul>
                <button type="button" className="arena-mode__play" onClick={() => play('ranked')} disabled={!chosenDeck || Boolean(search)}>
                  Cerca una classificata
                </button>
              </div>
            </article>

            <article className="arena-mode arena-mode--casual">
              <div className="arena-mode__art" aria-hidden="true" />
              <div className="arena-mode__body">
                <span className="arena-mode__kicker">Senza rank</span>
                <h2>Rapida</h2>
                <p>Un avversario a caso, per provare un mazzo nuovo senza rischiare il punteggio.</p>
                <ul className="arena-mode__tags">
                  <li>Nessun punto in gioco</li>
                  <li>Partita immediata</li>
                </ul>
                <button type="button" className="arena-mode__play" onClick={() => play('casual')} disabled={!chosenDeck || Boolean(search)}>
                  Gioca una rapida
                </button>
              </div>
            </article>

            <article className="arena-mode arena-mode--friends">
              <div className="arena-mode__art" aria-hidden="true">
                <img src="/artwork/simbolo-dreams.webp" alt="" />
                <img src="/artwork/simbolo-nightmares.webp" alt="" />
                <img src="/artwork/simbolo-memories.webp" alt="" />
              </div>
              <div className="arena-mode__body">
                <span className="arena-mode__kicker">Tra amici</span>
                <h2>Sfida un amico</h2>
                <p>Lancia una sfida a un amico: quando l'accetta, la partita si gioca con i vostri mazzi.</p>
                <ul className="arena-mode__tags">
                  <li>Solo amici</li>
                  {summary?.pendingChallenges > 0 && <li className="is-alert">{summary.pendingChallenges} in attesa</li>}
                </ul>
                <a className="arena-mode__play" href="/sfide">Vai alle sfide</a>
              </div>
            </article>
          </section>

          <section className="arena-recent" aria-labelledby="arena-recent-title">
            <div className="arena-section-head">
              <h2 id="arena-recent-title">Ultime partite</h2>
            </div>
            {!recent && <p className="arena-muted">Caricamento...</p>}
            {recent && recent.length === 0 && <p className="arena-muted">Non hai ancora giocato: la prima partita ti aspetta.</p>}
            {recent && recent.length > 0 && (
              <ul className="arena-recent__list">
                {recent.map((match) => (
                  <li key={match.id}>
                    <a href={`/partite/${match.id}`} className={`arena-recent__row arena-recent__row--${match.result.toLowerCase()}`}>
                      <span className="arena-recent__result">{resultNames[match.result]}</span>
                      <span className="arena-recent__who">
                        <strong>{match.opponent.username}</strong>
                        <small>{typeNames[match.matchType] ?? match.matchType} · {match.totalRounds} round · {formatDate(match.playedAt)}</small>
                      </span>
                      {match.ratingChange !== null && (
                        <span className={`arena-recent__points${match.ratingChange < 0 ? ' is-loss' : ''}`}>
                          {match.ratingChange > 0 ? '+' : ''}{match.ratingChange}
                        </span>
                      )}
                      <span className="arena-recent__open" aria-hidden="true">Guarda</span>
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </main>

      {search && <SearchPortal search={search} />}
    </div>
  );
}
