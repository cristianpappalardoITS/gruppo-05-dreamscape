import { useEffect, useState } from 'react';

// Schermo intero del browser (Fullscreen API) su tutta la pagina. Il browser lo chiude da solo con Esc
// e a ogni cambio di pagina. Su iPhone non c'è: lì supported è false e il pulsante non si mostra.
export default function useFullscreen() {
  const supported = Boolean(document.fullscreenEnabled);
  const [isFullscreen, setIsFullscreen] = useState(() => Boolean(document.fullscreenElement));

  useEffect(() => {
    const update = () => setIsFullscreen(Boolean(document.fullscreenElement));
    document.addEventListener('fullscreenchange', update);
    return () => document.removeEventListener('fullscreenchange', update);
  }, []);

  const toggle = () => {
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    else document.documentElement.requestFullscreen().catch(() => {});
  };

  return { supported, isFullscreen, toggle };
}
