import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import { endReasonNames } from '../components/Partita/matchLog.js';
import '../styles/home.css';
import '../components/Storico/Storico.css';

const PAGE_SIZE = 20;

const resultNames = { WIN: 'Vittoria', LOSS: 'Sconfitta', DRAW: 'Pareggio' };
const typeNames = { RANKED: 'Classificata', CASUAL: 'Rapida' };

// Nomi dei movimenti dei Dream Credits (guida per il frontend, "Crediti")
const creditNames = {
  SIGNUP_BONUS: 'Regalo di benvenuto',
  DAILY_REWARD: 'Ricompensa giornaliera',
  PACK_PURCHASE: 'Acquisto pacchetto',
  MATCH_REWARD: 'Vittoria in partita',
  MISSION_REWARD: 'Missione completata',
  LEAGUE_REWARD: 'Premio di lega',
  ADMIN_ADJUSTMENT: "Correzione dell'amministratore",
};

// Filtri delle partite: tutte, solo classificate, solo rapide (le sfide tra amici sono rapide)
const matchFilters = [
  { value: '', label: 'Tutte' },
  { value: 'RANKED', label: 'Classificate' },
  { value: 'CASUAL', label: 'Rapide e sfide' },
];

const formatDateTime = (iso) => new Date(iso).toLocaleString('it-IT', {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
});
// Migliaia sempre con il punto (toLocaleString in italiano non lo mette sotto 10.000)
const formatCredits = (value) => String(Math.abs(value)).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
const signed = (value) => `${value > 0 ? '+' : value < 0 ? '−' : ''}${formatCredits(value)}`;

// Scheda iniziale dall'indirizzo: /storico?scheda=crediti apre i Dream Credits (link dal Profilo)
const initialTab = () => (new URLSearchParams(window.location.search).get('scheda') === 'crediti' ? 'credits' : 'matches');

// Pulsanti Indietro / Avanti sotto le liste a pagine
// (cambiando pagina si torna all'inizio della lista)
function Pager({ data, page, onPage }) {
  if (!data || data.totalPages <= 1) return null;
  const go = (next) => {
    onPage(next);
    document.getElementById('history-panel')?.scrollIntoView({ block: 'start' });
  };
  return (
    <nav className="history-pager" aria-label="Pagine">
      <button type="button" disabled={page === 0} onClick={() => go(page - 1)}>Indietro</button>
      <span>Pagina {page + 1} di {data.totalPages}</span>
      <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => go(page + 1)}>Avanti</button>
    </nav>
  );
}

// Tutte le partite (GET /me/matches, a pagine e con il filtro per tipo), con il link al replay
function MatchHistory() {
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const query = new URLSearchParams({ page, size: PAGE_SIZE });
    if (type) query.set('type', type);
    api(`/me/matches?${query}`).then((result) => { setData(result); setError(null); }).catch(setError);
  }, [type, page]);

  const chooseType = (value) => {
    setType(value);
    setPage(0);
    setData(null);
  };

  return (
    <>
      <div className="history-toolbar">
        <div className="history-filters" role="group" aria-label="Tipo di partita">
          {matchFilters.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={type === filter.value ? 'is-active' : ''}
              aria-pressed={type === filter.value}
              onClick={() => chooseType(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>
        {data && <span className="history-count">{data.totalItems} {data.totalItems === 1 ? 'partita' : 'partite'}</span>}
      </div>

      {error && <p className="history-message history-message--error" role="alert">{error.message}</p>}
      {!error && !data && <p className="history-message">Caricamento...</p>}
      {data && data.items.length === 0 && (
        <div className="history-empty">
          <p>{type ? 'Nessuna partita di questo tipo.' : 'Non hai ancora giocato: la prima partita ti aspetta.'}</p>
          <a className="home-button home-button--filled" href="/arena">Vai all'Arena</a>
        </div>
      )}

      {data && data.items.length > 0 && (
        <ul className="history-list">
          {data.items.map((match) => (
            <li key={match.id}>
              <a href={`/partite/${match.id}`} className={`history-match history-match--${match.result.toLowerCase()}`}>
                <span className="history-match__result">{resultNames[match.result] ?? match.result}</span>
                <span className="history-match__who">
                  <strong>contro {match.opponent.username}</strong>
                  <small>
                    {typeNames[match.matchType] ?? match.matchType}
                    {match.matchType === 'CASUAL' && !match.startedByMe ? ' (accettata)' : ''}
                    {' · '}{match.totalRounds} round · {endReasonNames[match.endReason] ?? match.endReason}
                  </small>
                </span>
                <time className="history-match__date" dateTime={match.playedAt}>{formatDateTime(match.playedAt)}</time>
                <span className={`history-match__points${match.ratingChange < 0 ? ' is-loss' : ''}`}>
                  {match.ratingChange === null ? '' : `${match.ratingChange > 0 ? '+' : ''}${match.ratingChange} punti`}
                </span>
                <span className="history-match__open" aria-hidden="true">Rivedi</span>
              </a>
            </li>
          ))}
        </ul>
      )}

      <Pager data={data} page={page} onPage={setPage} />
    </>
  );
}

// Movimenti dei Dream Credits (GET /me/credit-transactions), dal più recente, con il saldo attuale
function CreditHistory() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [balance, setBalance] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/me/summary').then((summary) => setBalance(summary.credits)).catch(() => {});
  }, []);

  useEffect(() => {
    api(`/me/credit-transactions?page=${page}&size=${PAGE_SIZE}`)
      .then((result) => { setData(result); setError(null); })
      .catch(setError);
  }, [page]);

  return (
    <>
      <div className="history-toolbar">
        <div className="history-balance">
          <span className="dc-coin" aria-hidden="true">C</span>
          <span>Saldo attuale <strong>{balance === null ? '...' : `${formatCredits(balance)} DC`}</strong></span>
        </div>
        <a className="history-shop-link" href="/negozio">Vai al negozio</a>
      </div>

      {error && <p className="history-message history-message--error" role="alert">{error.message}</p>}
      {!error && !data && <p className="history-message">Caricamento...</p>}
      {data && data.items.length === 0 && <p className="history-message">Nessun movimento.</p>}

      {data && data.items.length > 0 && (
        <ul className="history-list">
          {data.items.map((item, position) => (
            <li key={`${item.createdAt}-${position}`} className={`history-credit${item.amount < 0 ? ' is-spent' : ''}`}>
              <span className="history-credit__what">
                <strong>{creditNames[item.type] ?? item.type}</strong>
                <time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time>
              </span>
              <span className="history-credit__amount">{signed(item.amount)} DC</span>
              <span className="history-credit__after">saldo {formatCredits(item.balanceAfter)}</span>
            </li>
          ))}
        </ul>
      )}

      <Pager data={data} page={page} onPage={setPage} />
    </>
  );
}

// Storico del Dreamweaver: due schede, le partite giocate e i movimenti dei Dream Credits
export default function HistoryPage() {
  const [tab, setTab] = useState(initialTab);

  const chooseTab = (value) => {
    setTab(value);
    // L'indirizzo ricorda la scheda: ricaricando o tornando indietro si resta sulla stessa
    const url = value === 'credits' ? '/storico?scheda=crediti' : '/storico';
    window.history.replaceState(null, '', url);
  };

  return (
    <div className="history-page">
      <HomeHeader />

      <main className="history-content">
        <div className="history-intro">
          <h1>Storico</h1>
          <p>Le partite che hai giocato e dove sono andati i tuoi Dream Credits.</p>
        </div>

        <div className="history-tabs" role="tablist" aria-label="Storico">
          <button type="button" role="tab" id="history-tab-matches" aria-selected={tab === 'matches'} aria-controls="history-panel" onClick={() => chooseTab('matches')}>
            Partite
          </button>
          <button type="button" role="tab" id="history-tab-credits" aria-selected={tab === 'credits'} aria-controls="history-panel" onClick={() => chooseTab('credits')}>
            Dream Credits
          </button>
        </div>

        <section className="history-panel" id="history-panel" role="tabpanel" aria-labelledby={tab === 'credits' ? 'history-tab-credits' : 'history-tab-matches'}>
          {tab === 'credits' ? <CreditHistory /> : <MatchHistory />}
        </section>
      </main>
    </div>
  );
}
