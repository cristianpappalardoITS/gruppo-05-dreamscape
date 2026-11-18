import { useEffect, useState } from 'react';
import { api } from '../api.js';
import HomeHeader from '../components/home/HomeHeader';
import FactionEmblem from '../components/home/FactionEmblem';
import CardGridItem from '../components/Collezione/CardGridItem';
import SelectedCardDetail from '../components/Collezione/SelectedCardDetail';
import PackOpening from '../components/Benvenuto/PackOpening';
import '../styles/home.css';
import '../components/Collezione/Collezione.css';
import '../components/Benvenuto/Benvenuto.css';

// Carte uscite { card } raggruppate per carta: { card, quantity }, nell'ordine in cui compaiono
function groupCards(openedCards) {
  const entries = new Map();
  openedCards.forEach(({ card }) => {
    const entry = entries.get(card.id);
    if (entry) entry.quantity += 1;
    else entries.set(card.id, { card, quantity: 1 });
  });
  return [...entries.values()];
}

// Pagina Benvenuto (GET/POST /me/welcome): il Dreamweaver sceglie la fazione del set iniziale,
// riceve 20 carte e apre i due pacchetti di benvenuto. La scelta si fa una volta sola.
// Dopo la scelta: il set iniziale si scopre tutto insieme, poi ogni pacchetto di benvenuto
// si apre una carta alla volta, e alla fine c'è il riepilogo. Chi torna sulla pagina vede il riepilogo.
export default function WelcomePage() {
  const [welcome, setWelcome] = useState(null);
  const [factions, setFactions] = useState([]);
  const [lords, setLords] = useState({});
  const [chosenFaction, setChosenFaction] = useState(null);
  // 'starter' = set iniziale, un numero = pacchetto di benvenuto da aprire, 'summary' = riepilogo
  const [stage, setStage] = useState('summary');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/me/welcome').then(setWelcome).catch(setError);
    api('/factions')
      .then((data) => {
        setFactions(data);
        // La carta del Lord di ogni fazione, per mostrarne l'illustrazione
        data.filter((faction) => faction.lordCardId).forEach((faction) => {
          api(`/cards/${faction.lordCardId}`)
            .then((card) => setLords((current) => ({ ...current, [faction.code]: card })))
            .catch(() => {});
        });
      })
      .catch(setError);
  }, []);

  const goTo = (nextStage) => {
    setStage(nextStage);
    window.scrollTo(0, 0);
  };

  const confirmChoice = async () => {
    setSending(true);
    setError(null);
    try {
      setWelcome(await api('/me/welcome', { method: 'POST', body: { starterFaction: chosenFaction } }));
      setStage('starter');
      window.scrollTo(0, 0);
    } catch (err) {
      // Scelta già fatta (per esempio da un'altra scheda): si mostra quello che ha ricevuto
      if (err.code === 'WELCOME_ALREADY_CLAIMED') {
        api('/me/welcome').then(setWelcome).catch(setError);
      } else {
        setError(err);
      }
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="welcome-page">
      <HomeHeader />

      <main className="welcome-content">
        {error && <p className="collection-message" role="alert">{error.message}</p>}
        {!error && !welcome && <p className="collection-message">Caricamento...</p>}

        {welcome && welcome.starterFaction === null && (
          <FactionChoice
            factions={factions}
            lords={lords}
            chosenFaction={chosenFaction}
            onChoose={setChosenFaction}
            onConfirm={confirmChoice}
            sending={sending}
          />
        )}

        {welcome && welcome.starterFaction !== null && stage === 'starter' && (
          <StarterReveal
            welcome={welcome}
            factions={factions}
            onDone={() => goTo(welcome.welcomeOpenings.length > 0 ? 0 : 'summary')}
          />
        )}

        {welcome && welcome.starterFaction !== null && typeof stage === 'number' && (
          <PackOpening
            key={stage}
            opening={welcome.welcomeOpenings[stage]}
            nextLabel={stage + 1 < welcome.welcomeOpenings.length ? 'Apri il prossimo pacchetto' : 'Vedi tutte le carte ricevute'}
            onDone={() => goTo(stage + 1 < welcome.welcomeOpenings.length ? stage + 1 : 'summary')}
          />
        )}

        {welcome && welcome.starterFaction !== null && stage === 'summary' && (
          <WelcomeGifts welcome={welcome} factions={factions} />
        )}
      </main>
    </div>
  );
}

function FactionChoice({ factions, lords, chosenFaction, onChoose, onConfirm, sending }) {
  const factionName = (code) => factions.find((faction) => faction.code === code)?.name ?? code;
  const chosen = factions.find((faction) => faction.code === chosenFaction);

  return (
    <>
      <div className="welcome-intro">
        <span className="welcome-eyebrow">Benvenuto, Dreamweaver</span>
        <h1>Scegli la tua fazione</h1>
        <p>
          Riceverai il set iniziale della fazione: 20 carte con cui puoi già comporre un mazzo valido.
          Poi aprirai due pacchetti di benvenuto. La scelta si fa una volta sola.
        </p>
      </div>

      <div className="welcome-factions">
        {factions.map((faction) => {
          const lord = lords[faction.code];
          const isChosen = faction.code === chosenFaction;
          return (
            <button
              type="button"
              key={faction.code}
              className={`welcome-faction welcome-faction--${faction.code.toLowerCase()}${isChosen ? ' is-chosen' : ''}`}
              onClick={() => onChoose(faction.code)}
              aria-pressed={isChosen}
            >
              <div className="welcome-faction__lord">
                {lord?.illustrationThumbUrl && (
                  <img
                    src={lord.illustrationThumbUrl}
                    alt=""
                    style={{ objectPosition: `center ${lord.illustrationFocusY * 100}%` }}
                  />
                )}
                <FactionEmblem faction={faction.code.toLowerCase()} />
              </div>
              <h2>{faction.name}</h2>
              <p className="welcome-faction__description">{faction.description}</p>
              {lord && <p className="welcome-faction__lord-name">{faction.lordTitle}: {lord.name}</p>}
              <p className="welcome-faction__matchups">
                Forte contro {faction.strongAgainst.map(factionName).join(', ')}
                {' · '}
                Debole contro {faction.weakAgainst.map(factionName).join(', ')}
              </p>
            </button>
          );
        })}
      </div>

      <div className="welcome-confirm" aria-live="polite">
        {chosen ? (
          <>
            <p>Hai scelto <strong>{chosen.name}</strong>. Dopo la conferma non si può cambiare.</p>
            <button type="button" className="home-button home-button--filled" onClick={onConfirm} disabled={sending}>
              {sending ? 'Attendi...' : `Conferma ${chosen.name}`}
            </button>
          </>
        ) : (
          <p>Seleziona una delle tre fazioni.</p>
        )}
      </div>
    </>
  );
}

// Il set iniziale si scopre tutto insieme: le carte si girano una dopo l'altra
function StarterReveal({ welcome, factions, onDone }) {
  const cards = groupCards(welcome.starterCards);
  const factionName = factions.find((faction) => faction.code === welcome.starterFaction)?.name ?? welcome.starterFaction;

  return (
    <section aria-labelledby="starter-title">
      <div className="welcome-intro">
        <span className="welcome-eyebrow">Benvenuto tra i {factionName}</span>
        <h1 id="starter-title">Il tuo set iniziale</h1>
        <p>{welcome.starterCards.length} carte dei {factionName}: con queste puoi già comporre il tuo primo mazzo.</p>
      </div>
      <div className="starter-reveal">
        {cards.map((entry, position) => (
          <div className="reveal-card" style={{ '--reveal-delay': `${position * 70}ms` }} key={entry.card.id}>
            <CardGridItem entry={entry} isSelected={false} onSelect={() => {}} />
          </div>
        ))}
      </div>
      <div className="welcome-confirm">
        <button type="button" className="home-button home-button--filled" onClick={onDone}>
          {welcome.welcomeOpenings.length > 0 ? 'Apri i pacchetti di benvenuto' : 'Vedi tutte le carte ricevute'}
        </button>
      </div>
    </section>
  );
}

function WelcomeGifts({ welcome, factions }) {
  const sections = [
    { title: 'Il tuo set iniziale', cards: groupCards(welcome.starterCards) },
    ...welcome.welcomeOpenings.map((opening) => ({ title: opening.packName, cards: groupCards(opening.cards) })),
  ];
  const [selectedEntry, setSelectedEntry] = useState(sections[0].cards[0] ?? null);
  const factionName = factions.find((faction) => faction.code === welcome.starterFaction)?.name ?? welcome.starterFaction;

  return (
    <>
      <div className="welcome-intro">
        <span className="welcome-eyebrow">Benvenuto tra i {factionName}</span>
        <h1>Le tue prime carte</h1>
        <p>Sono già nella tua collezione. Tocca una carta per vederne i dettagli.</p>
        <div className="home-button-row">
          <a className="home-button home-button--filled" href="/collezione">Vai alla collezione</a>
        </div>
      </div>

      <div className="collection-columns">
        <aside className="collection-detail-column">
          <SelectedCardDetail entry={selectedEntry} />
        </aside>

        <div className="collection-grid-column">
          {sections.map((section) => (
            <section className="welcome-gift" key={section.title} aria-label={section.title}>
              <h2>
                {section.title}
                <span>{section.cards.reduce((total, entry) => total + entry.quantity, 0)} carte</span>
              </h2>
              <div className="collection-grid">
                {section.cards.map((entry) => (
                  <CardGridItem
                    key={entry.card.id}
                    entry={entry}
                    isSelected={selectedEntry?.card.id === entry.card.id}
                    onSelect={setSelectedEntry}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>
      </div>
    </>
  );
}
