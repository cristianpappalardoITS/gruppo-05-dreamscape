import { useEffect, useState } from 'react';
import { api, isLoggedIn } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import HomeFooter from '../components/home/HomeFooter';
import FactionEmblem from '../components/home/FactionEmblem';
import RuleIcon from '../components/home/RuleIcon';
import '../styles/home.css';

// Dati di riserva: si vedono finché il backend non risponde, o se non risponde
const defaultFactions = [
  { code: 'MEMORIES', name: 'Memories' },
  { code: 'DREAMS', name: 'Dreams' },
  { code: 'NIGHTMARES', name: 'Nightmares' },
];
const defaultRules = { deckSize: 20, startingLifePoints: 20 };

export default function HomePage() {
  const [factions, setFactions] = useState(defaultFactions);
  const [rules, setRules] = useState(defaultRules);

  // Pagina pubblica: se una chiamata non riesce restano i dati di riserva
  useEffect(() => {
    api('/factions').then(setFactions).catch(() => {});
    api('/rules').then(setRules).catch(() => {});
  }, []);

  const gameRules = [
    { title: `Mazzo da ${rules.deckSize} carte`, icon: 'deck' },
    { title: `${rules.startingLifePoints} punti vita`, icon: 'health' },
    { title: 'Partita automatica', icon: 'battle' },
    { title: 'Vantaggi di fazione', icon: 'factions' },
  ];

  return (
    <div className="home-page">
      <HomeHeader />

      <main>
        <section className="home-hero" aria-label="Il mondo di Dreamscape">
          <img src="/artwork/dreamscape-worlds.webp" alt="Tre mondi fantastici: oscurità, luce dorata e sogni viola" />
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
              <li>Scegli la tua prima fazione. Alla registrazione ricevi 500 Dream Credits; poi, scelta la fazione, ricevi il suo set iniziale e apri due pacchetti di benvenuto.</li>
              <li>Componi il tuo mazzo. Scegli {rules.deckSize} carte della tua collezione e valuta attacco, difesa, effetti e sinergie di fazione.</li>
              <li>Mettilo alla prova. Cerca una partita rapida, sfida un amico o entra in classifica: il gioco mostra round dopo round la tua strategia.</li>
            </ol>
          </div>
        </section>

        <section className="home-factions" id="fazioni">
          <h2>Tre fazioni oltre la soglia</h2>
          <div className="home-factions__cards">
            {factions.map(({ code, name, description }) => (
              <article className={`home-faction-card home-faction-card--${code.toLowerCase()}`} key={code}>
                <FactionEmblem faction={code.toLowerCase()} />
                <h3>{name}</h3>
                {description && <p>{description}</p>}
              </article>
            ))}
          </div>
        </section>

        <section className="home-cta" aria-labelledby="home-cta-title">
          <div className="home-cta__panel">
            {isLoggedIn() ? (
              <>
                <h2 id="home-cta-title">Le tue carte ti aspettano</h2>
                <p>Guarda la tua collezione, scopri gli effetti delle carte e preparati alla prossima sfida.</p>
                <div className="home-button-row">
                  <a className="home-button home-button--filled" href="/dashboard">Vai alla dashboard</a>
                  <a className="home-button home-button--outlined" href="/collezione">Vai alla collezione</a>
                </div>
              </>
            ) : (
              <>
                <h2 id="home-cta-title">Inizia il tuo viaggio</h2>
                <p>Crea il tuo account: ricevi 500 Dream Credits, scegli la tua fazione e apri i primi pacchetti.</p>
                <div className="home-button-row">
                  <a className="home-button home-button--filled" href="/register">Crea il tuo account</a>
                  <a className="home-button home-button--outlined" href="/login">Ho già un account</a>
                </div>
              </>
            )}
          </div>
        </section>

        <section className="home-rules" id="regolamento">
          <h2>Regolamento di gioco</h2>
          <div className="home-rules__cards">
            {gameRules.map(({ title, icon }) => (
              <article className="home-rule-card" key={icon}>
                <RuleIcon type={icon} />
                <h3>{title}</h3>
              </article>
            ))}
          </div>
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
