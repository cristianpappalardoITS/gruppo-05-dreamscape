import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import PackOpening from '../components/Benvenuto/PackOpening';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Benvenuto/Benvenuto.css';

// Apertura di un pacchetto comprato: /negozio/apertura/{openingId} legge GET /pack-openings/{openingId}.
// Con l'id dell'apertura nell'indirizzo, ricaricare la pagina non compra un altro pacchetto.
export default function PackOpeningPage({ openingId }) {
  const [opening, setOpening] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api(`/pack-openings/${openingId}`).then(setOpening).catch(setError);
  }, [openingId]);

  return (
    <div className="welcome-page">
      <HomeHeader />

      <main className="welcome-content">
        {error && (
          <p className="collection-message" role="alert">
            {error.code === 'PACK_OPENING_NOT_FOUND' || error.code === 'MALFORMED_REQUEST'
              ? 'Apertura non trovata.'
              : error.message}
            {' '}<a href="/negozio">Torna al negozio</a>
          </p>
        )}
        {!error && !opening && <p className="collection-message">Caricamento...</p>}
        {opening && (
          <PackOpening
            opening={opening}
            nextLabel="Torna al negozio"
            onDone={() => window.location.assign('/negozio')}
          />
        )}
      </main>
    </div>
  );
}
