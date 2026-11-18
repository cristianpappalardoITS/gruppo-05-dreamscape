import HomeHeader from '../components/home/HomeHeader';
import HomeFooter from '../components/home/HomeFooter';
import FactionEmblem from '../components/home/FactionEmblem';
import RuleIcon from '../components/home/RuleIcon';
import '../styles/home.css';

const factions = [
  { name: 'Memories', theme: 'memories' },
  { name: 'Dreams', theme: 'dreams' },
  { name: 'Nightmares', theme: 'nightmares' },
];

const gameRules = [
  { title: 'Mazzo da 20 carte', icon: 'deck' },
  { title: '20 punti vita', icon: 'health' },
  { title: 'Partita automatica', icon: 'battle' },
  { title: 'Vantaggi di fazione', icon: 'factions' },
];

export default function HomePage() {
  return (
    <div className="home-page">
      <HomeHeader />

      <main>
        <section className="home-hero" aria-label="Il mondo di Dreamscape">
          <img src="/artwork/dreamscape-worlds.png" alt="Tre mondi fantastici: oscurità, luce dorata e sogni viola" />
        </section>

        <section className="home-introduction" id="gioco">
          <div className="home-section-content">
            <h1>Dreams &amp; Nightmares</h1>
            <h2>Ogni sogno nasconde qualcosa.</h2>
            <p>Quando chiudi gli occhi, la tua mente si apre a mondi sconosciuti. In Dreamscape ogni sogno diventa una carta, ogni scelta può cambiare la tua storia. Crea il tuo account, scegli la tua fazione e preparati a sfidare altri Dreamweaver.</p>
            <div className="home-button-row">
              <a className="home-button home-button--filled" href="/login">Entra nell'arena</a>
              <a className="home-button home-button--outlined" href="#sfida">Scopri il gioco</a>
            </div>
          </div>
        </section>

        <section className="home-challenge" id="sfida">
          <div className="home-section-content home-section-content--narrow">
            <h2>La sfida comincia dal mazzo</h2>
            <ol>
              <li>Scegli la tua prima fazione. Alla registrazione ricevi 500 Dream Credits, il set iniziale e i crediti per iniziare a giocare.</li>
              <li>Componi il tuo mazzo. Scegli 20 creature tra la tua collezione, valuta attacco, difesa, effetti e sinergie di fazione.</li>
              <li>Mettilo alla prova. Cerca una partita rapida, sfida un amico o entra in classifica: il gioco mostra round dopo round la tua strategia.</li>
            </ol>
          </div>
        </section>

        <section className="home-factions" id="fazioni">
          <h2>Tre fazioni oltre la soglia</h2>
          <div className="home-factions__cards">
            {factions.map(({ name, theme }) => (
              <article className={`home-faction-card home-faction-card--${theme}`} key={name}>
                <FactionEmblem faction={theme} />
                <h3>{name}</h3>
              </article>
            ))}
          </div>
        </section>

        <section className="home-rules" id="regolamento">
          <h2>Regolamento di gioco</h2>
          <div className="home-rules__cards">
            {gameRules.map(({ title, icon }) => (
              <article className="home-rule-card" key={title}>
                <RuleIcon type={icon} />
                <h3>{title}</h3>
              </article>
            ))}
          </div>
          <a className="home-button home-button--outlined home-rules__button" href="#sfida">Leggi il regolamento</a>
        </section>

        <section className="home-final-call" aria-labelledby="home-final-call-title">
          <div className="home-final-call__panel">
            <h2 id="home-final-call-title">Pronto a sfidare l'inconscio?</h2>
            <div className="home-button-row">
              <a className="home-button home-button--filled" href="/login">Entra nell'arena</a>
              <a className="home-button home-button--outlined" href="#gioco">Esplora il mondo di Dreamscape</a>
            </div>
          </div>
        </section>
      </main>

      <HomeFooter />
    </div>
  );
}
