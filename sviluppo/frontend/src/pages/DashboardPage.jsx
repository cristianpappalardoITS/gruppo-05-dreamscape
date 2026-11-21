import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import CardGridItem from '../components/Collezione/CardGridItem';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Dashboard/Dashboard.css';

// Migliaia sempre con il punto
const formatNumber = (value) => String(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

// Dashboard del Dreamweaver (GET /me/summary): crediti, punteggio e lega, missioni,
// cose in attesa e le carte dell'ultima apertura. Se la fazione non è ancora scelta
// (GET /me/welcome) invita ad andare al Benvenuto.
export default function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const [welcomeClaimed, setWelcomeClaimed] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/me/summary').then(setSummary).catch(setError);
    api('/me/welcome').then((welcome) => setWelcomeClaimed(welcome.starterFaction !== null)).catch(() => {});
  }, []);

  return (
    <div className="dashboard-page">
      <HomeHeader />

      <main className="decks-content dashboard-content">
        {error && <p className="collection-message" role="alert">{error.message}</p>}
        {!error && !summary && <p className="collection-message">Caricamento...</p>}

        {summary && (
          <>
            <div className="dashboard-hello">
              <span className="dashboard-eyebrow">Bentornato, Dreamweaver</span>
              <h1>{summary.username}</h1>
            </div>

            {!welcomeClaimed && (
              <div className="dashboard-banner">
                <p>Non hai ancora scelto la tua fazione: ricevi il set iniziale e apri i pacchetti di benvenuto.</p>
                <a className="home-button home-button--filled" href="/benvenuto">Scegli la fazione</a>
              </div>
            )}

            <div className="dashboard-stats">
              <section className="dashboard-stat">
                <span>Dream Credits</span>
                <strong>{formatNumber(summary.credits)} DC</strong>
                <a href="/negozio">Vai al negozio</a>
              </section>
              <section className="dashboard-stat">
                <span>Lega</span>
                <strong>{summary.league.name}</strong>
                <p>
                  {summary.rating} punti
                  {summary.league.nextLeagueName && (
                    <> · ne mancano {Math.max(summary.league.nextLeagueMinRating - summary.rating, 0)} per {summary.league.nextLeagueName}</>
                  )}
                </p>
              </section>
              <section className="dashboard-stat">
                <span>In attesa</span>
                <ul className="dashboard-pending">
                  <li><strong>{summary.pendingFriendRequests}</strong> richieste di amicizia</li>
                  <li><strong>{summary.pendingTrades}</strong> scambi</li>
                  <li><strong>{summary.pendingChallenges}</strong> sfide</li>
                </ul>
              </section>
            </div>

            {/* Senza mazzo classificato nessuno può sceglierti come avversario (hasRankedDeck) */}
            {welcomeClaimed && !summary.hasRankedDeck && (
              <div className="dashboard-banner">
                <p>Scegli il tuo mazzo classificato: è quello con cui giochi quando un altro Dreamweaver ti sfida in classificata.</p>
                <a className="home-button home-button--filled" href="/arena">Vai all'Arena</a>
              </div>
            )}

            <div className="dashboard-columns">
              <section className="dashboard-panel" aria-labelledby="missions-title">
                <h2 id="missions-title">Missioni</h2>
                {summary.missions.length === 0 ? (
                  <p className="collection-message">Le missioni giornaliere e settimanali arriveranno presto.</p>
                ) : (
                  <ul className="dashboard-missions">
                    {summary.missions.map((mission) => (
                      <li key={mission.code} className={mission.isCompleted ? 'is-done' : ''}>
                        <div>
                          <strong>{mission.name}</strong>
                          <p>{mission.description}</p>
                        </div>
                        <span>{mission.progress}/{mission.goalCount} · +{mission.rewardCredits} DC</span>
                        <progress value={mission.progress} max={mission.goalCount} />
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="dashboard-panel" aria-labelledby="links-title">
                <h2 id="links-title">Vai a</h2>
                <nav className="dashboard-links">
                  <a href="/arena">Arena</a>
                  <a href="/collezione">Collezione</a>
                  <a href="/mazzi">Mazzi</a>
                  <a href="/mazzi/editor">Nuovo mazzo</a>
                  <a href="/negozio">Negozio</a>
                </nav>
              </section>
            </div>

            <section className="dashboard-panel dashboard-latest" aria-labelledby="latest-title">
              <h2 id="latest-title">Le tue ultime carte</h2>
              {summary.latestCards.length === 0 ? (
                <p className="collection-message">Non hai ancora aperto pacchetti.</p>
              ) : (
                <div className="collection-grid">
                  {summary.latestCards.map(({ card }, position) => (
                    <CardGridItem key={`${card.id}-${position}`} entry={{ card }} isSelected={false} onSelect={() => window.location.assign('/collezione')} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
