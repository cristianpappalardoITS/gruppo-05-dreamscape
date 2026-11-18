export default function HomeFooter() {
  return (
    <footer className="home-footer" id="chi-siamo">
      <a className="home-footer__brand" href="/" aria-label="Dreamscape, torna alla home">
        <span className="home-footer__logo" aria-hidden="true" />
      </a>
      <p>EVERY DREAM HAS A DARK SIDE. QUALE PARTE DELLA TUA MENTE SCENDERÀ IN CAMPO?</p>
      <div className="home-footer__social" aria-label="Social Dreamscape">
        <span aria-hidden="true">◎</span><span aria-hidden="true">◉</span><span aria-hidden="true">▣</span>
      </div>
      <p className="home-footer__contact" id="contatti">Il mondo di Dreamscape ti aspetta.</p>
      <small>© 2026 Dreamscape TCG. Tutti i diritti riservati.</small>
    </footer>
  );
}
