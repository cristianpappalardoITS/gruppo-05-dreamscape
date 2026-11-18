import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import { TradeCards, TradeWarnings } from '../components/Scambi/TradeParts.jsx';
import { hasWarnings } from '../components/Scambi/hasWarnings.js';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Mazzi/Mazzi.css';
import '../components/Dashboard/Dashboard.css';
import '../components/Scambi/Scambi.css';

const boxes = [
  { code: 'received', label: 'Ricevuti' },
  { code: 'sent', label: 'Inviati' },
  { code: 'history', label: 'Storico' },
];

const statusLabels = { PENDING: 'In attesa', ACCEPTED: 'Accettato', REJECTED: 'Rifiutato', CANCELLED: 'Annullato' };

// Scambi (GET /trades?box=received|sent|history): accetta, rifiuta, annulla.
// Prima di accettare si leggono gli avvisi (GET /trades/{tradeId}/deck-warnings) e si chiede conferma.
export default function TradesPage() {
  const initialBox = new URLSearchParams(window.location.search).get('box');
  const [box, setBox] = useState(boxes.some((item) => item.code === initialBox) ? initialBox : 'received');
  const [trades, setTrades] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  // Scambio che si sta per accettare, con i suoi avvisi
  const [pendingAccept, setPendingAccept] = useState(null);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    api(`/trades?box=${box}&page=${page}&size=10`)
      .then((data) => {
        if (ignore) return;
        setTrades(data.items);
        setTotalPages(data.totalPages);
      })
      .catch((err) => { if (!ignore) setError(err); });
    return () => {
      ignore = true;
    };
  }, [box, page, reloadKey]);

  const changeBox = (code) => {
    setBox(code);
    setPage(0);
    setTrades(null);
    setPendingAccept(null);
    setMessage(null);
    setError(null);
  };

  const act = async (trade, action, successText) => {
    setMessage(null);
    setError(null);
    try {
      await api(`/trades/${trade.id}/${action}`, { method: 'POST' });
      setMessage(successText);
    } catch (err) {
      setError(err);
    }
    setPendingAccept(null);
    setReloadKey((key) => key + 1);
  };

  // Accettare: prima gli avvisi; se ce ne sono si chiede conferma
  const startAccept = async (trade) => {
    setMessage(null);
    setError(null);
    try {
      const warnings = await api(`/trades/${trade.id}/deck-warnings`);
      if (hasWarnings(warnings)) setPendingAccept({ trade, warnings });
      else act(trade, 'accept', 'Scambio accettato: le carte sono già nella tua collezione.');
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="decks-page">
      <HomeHeader />

      <main className="decks-content social-content">
        <div className="collection-title-row">
          <h1>Scambi</h1>
          <span className="title-actions">
            <a className="home-button home-button--outlined decks-new" href="/amici">Amici</a>
            <a className="home-button home-button--filled decks-new" href="/scambi/nuovo">Nuovo scambio</a>
          </span>
        </div>

        <div className="trade-tabs" role="tablist">
          {boxes.map((item) => (
            <button
              type="button"
              role="tab"
              key={item.code}
              aria-selected={box === item.code}
              className={box === item.code ? 'is-active' : ''}
              onClick={() => changeBox(item.code)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {message && <p className="social-message" role="status">{message}</p>}
        {error && <p className="collection-message shop-error" role="alert">{error.message}</p>}
        {!trades && !error && <p className="collection-message">Caricamento...</p>}
        {trades && trades.length === 0 && (
          <p className="collection-message">
            {box === 'received' && 'Nessuno scambio da accettare.'}
            {box === 'sent' && 'Nessuno scambio proposto in attesa.'}
            {box === 'history' && 'Nessuno scambio chiuso.'}
          </p>
        )}

        {trades && trades.length > 0 && (
          <ul className="trade-list">
            {trades.map((trade) => (
              <li key={trade.id} className="dashboard-panel trade">
                <div className="trade__header">
                  <span>
                    <strong>{trade.proposer.username}</strong> propone a <strong>{trade.recipient.username}</strong>
                  </span>
                  <span className={`trade-status trade-status--${trade.status.toLowerCase()}`}>{statusLabels[trade.status]}</span>
                </div>
                {trade.message && <p className="trade__message">«{trade.message}»</p>}

                <div className="trade__sides">
                  <div>
                    <h3>{trade.proposer.username} dà</h3>
                    <TradeCards entries={trade.offered} />
                  </div>
                  <div>
                    <h3>{trade.proposer.username} chiede</h3>
                    <TradeCards entries={trade.requested} />
                  </div>
                </div>

                <p className="trade__date">
                  Proposto il {new Date(trade.createdAt).toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' })}
                  {trade.resolvedAt && ` · chiuso il ${new Date(trade.resolvedAt).toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' })}`}
                </p>

                {pendingAccept?.trade.id === trade.id && (
                  <div className="trade-confirm">
                    <p>Prima di accettare:</p>
                    <TradeWarnings warnings={pendingAccept.warnings} />
                    <div className="social-buttons">
                      <button type="button" className="social-action" onClick={() => act(trade, 'accept', 'Scambio accettato: le carte sono già nella tua collezione.')}>
                        Accetta lo stesso
                      </button>
                      <button type="button" className="social-action social-action--quiet" onClick={() => setPendingAccept(null)}>Annulla</button>
                    </div>
                  </div>
                )}

                {trade.status === 'PENDING' && box === 'received' && pendingAccept?.trade.id !== trade.id && (
                  <div className="social-buttons">
                    <button type="button" className="social-action" onClick={() => startAccept(trade)}>Accetta</button>
                    <button type="button" className="social-action social-action--quiet" onClick={() => act(trade, 'reject', 'Scambio rifiutato.')}>Rifiuta</button>
                  </div>
                )}
                {trade.status === 'PENDING' && box === 'sent' && (
                  <div className="social-buttons">
                    <button type="button" className="social-action social-action--quiet" onClick={() => act(trade, 'cancel', 'Scambio annullato.')}>Annulla lo scambio</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}

        {totalPages > 1 && (
          <nav className="collection-pagination" aria-label="Pagine degli scambi">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Indietro</button>
            <span>Pagina {page + 1} di {totalPages}</span>
            <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Avanti</button>
          </nav>
        )}
      </main>
    </div>
  );
}
