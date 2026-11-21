import './FullscreenButton.css';

// Pulsante dello schermo intero (stato da useFullscreen): quattro angoli verso l'esterno per entrare,
// verso l'interno per uscire. Dove il browser non lo permette (iPhone) non si mostra.
export default function FullscreenButton({ fullscreen, className = '' }) {
  if (!fullscreen.supported) return null;
  const label = fullscreen.isFullscreen ? 'Esci dallo schermo intero' : 'Schermo intero';

  return (
    <button type="button" className={`fullscreen-button ${className}`} onClick={fullscreen.toggle} aria-label={label} title={label}>
      <svg viewBox="0 0 24 24" aria-hidden="true">
        {fullscreen.isFullscreen ? (
          <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5" />
        ) : (
          <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />
        )}
      </svg>
    </button>
  );
}
