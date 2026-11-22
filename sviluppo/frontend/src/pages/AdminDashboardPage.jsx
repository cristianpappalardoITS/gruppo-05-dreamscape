import { useEffect, useState } from 'react';
import { api } from '../api.js';
import AdminLayout from '../components/Admin/AdminLayout';
import { creditTypeNames, formatNumber } from '../components/Admin/adminFormat';

// Dashboard admin (GET /admin/stats): giocatori, carte, pacchetti, partite e Dream Credits.
// Contano solo i giocatori; oggi e ultimi 7 giorni sono in ora italiana (li calcola il backend).
export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/admin/stats').then(setStats).catch(setError);
  }, []);

  const load = () => {
    setError(null);
    api('/admin/stats').then(setStats).catch(setError);
  };

  const refreshButton = <button type="button" className="admin-button" onClick={load}>Aggiorna</button>;

  return (
    <AdminLayout title="Dashboard admin" actions={refreshButton}>
      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}
      {!error && !stats && <p className="collection-message">Caricamento dei numeri...</p>}
      {stats && <Stats stats={stats} />}
    </AdminLayout>
  );
}

function Stats({ stats }) {
  const { players, cards, packs, matches, credits } = stats;
  const updatedAt = new Date(stats.generatedAt).toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' });

  return (
    <>
      <div className="admin-stats">
        <section className="admin-stat">
          <span>Giocatori</span>
          <strong>{formatNumber(players.total)}</strong>
          <ul>
            <li><strong>{formatNumber(players.newToday)}</strong> nuovi oggi</li>
            <li><strong>{formatNumber(players.newLast7Days)}</strong> nuovi in 7 giorni</li>
            <li><strong>{formatNumber(players.activeLast7Days)}</strong> attivi in 7 giorni</li>
          </ul>
        </section>
        <section className="admin-stat">
          <span>Carte</span>
          <strong>{formatNumber(cards.active)}</strong>
          <ul>
            <li><strong>{formatNumber(cards.fullArt)}</strong> full art</li>
            <li><strong>{formatNumber(cards.disabled)}</strong> disattivate</li>
          </ul>
          <a href="/admin/carte">Gestisci le carte</a>
        </section>
        <section className="admin-stat">
          <span>Pacchetti</span>
          <strong>{formatNumber(packs.onSale)} in vendita</strong>
          <ul>
            <li><strong>{formatNumber(packs.openingsToday)}</strong> aperture oggi</li>
            <li><strong>{formatNumber(packs.openings)}</strong> aperture in tutto</li>
          </ul>
          <a href="/admin/pacchetti">Gestisci i pacchetti</a>
        </section>
        <section className="admin-stat">
          <span>Partite oggi</span>
          <strong>{formatNumber(matches.rankedToday + matches.casualToday)}</strong>
          <ul>
            <li><strong>{formatNumber(matches.rankedToday)}</strong> classificate ({formatNumber(matches.ranked)} in tutto)</li>
            <li><strong>{formatNumber(matches.casualToday)}</strong> rapide e sfide ({formatNumber(matches.casual)} in tutto)</li>
          </ul>
        </section>
      </div>

      <section className="admin-panel" aria-labelledby="credits-title">
        <h2 id="credits-title">Dream Credits</h2>
        <p className="admin-panel__intro">
          In circolazione, cioè sui saldi dei giocatori: <strong>{formatNumber(credits.inCirculation)} DC</strong>.
          Negli ultimi 7 giorni ne sono entrati {formatNumber(credits.last7Days.earned)} e usciti {formatNumber(credits.last7Days.spent)}.
        </p>
        {credits.last7Days.byType.length === 0 ? (
          <p className="collection-message">Nessun movimento negli ultimi 7 giorni.</p>
        ) : (
          <div className="admin-table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th scope="col">Tipo di movimento</th>
                  <th scope="col" className="admin-number">Movimenti</th>
                  <th scope="col" className="admin-number">Entrati</th>
                  <th scope="col" className="admin-number">Usciti</th>
                </tr>
              </thead>
              <tbody>
                {credits.last7Days.byType.map((row) => (
                  <tr key={row.type}>
                    <td>{creditTypeNames[row.type] ?? row.type}</td>
                    <td className="admin-number">{formatNumber(row.transactions)}</td>
                    <td className="admin-number">{row.earned ? `+${formatNumber(row.earned)}` : '–'}</td>
                    <td className="admin-number">{row.spent ? `−${formatNumber(row.spent)}` : '–'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <p className="collection-message">Numeri calcolati il {updatedAt}. Oggi e ultimi 7 giorni sono in ora italiana.</p>
    </>
  );
}
