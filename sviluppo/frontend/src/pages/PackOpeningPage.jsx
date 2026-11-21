import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import PackOpening from '../components/Benvenuto/PackOpening';
import FullscreenButton from '../components/SchermoIntero/FullscreenButton';
import useFullscreen from '../components/SchermoIntero/useFullscreen';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Benvenuto/Benvenuto.css';

// Apertura di un pacchetto comprato: /negozio/apertura/{openingId} legge GET /pack-openings/{openingId}.
// Con l'id dell'apertura nell'indirizzo, ricaricare la pagina non compra un altro pacchetto.
// La pagina sta in uno schermo (intestazione compatta, carta grande quanto l'altezza permette)
// e ha lo schermo intero.
export default function PackOpeningPage({ openingId }) {
  const [opening, setOpening] = useState(null);
  const [error, setError] = useState(null);
  const fullscreen = useFullscreen();

  useEffect(() => {
    api(`/pack-openings/${openingId}`).then(setOpening).catch(setError);
  }, [openingId]);

  return (
    <div className={`opening-page${fullscreen.isFullscreen ? ' is-fullscreen' : ''}`}>
      <HomeHeader compact />

      <main className="opening-stage">
        <div className="opening-corner">
          <FullscreenButton fullscreen={fullscreen} />
        </div>

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
