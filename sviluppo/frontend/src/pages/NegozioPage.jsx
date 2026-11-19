import { useState } from "react";

// Pacchetti disponibili nel negozio
const pacchetti = [
  {
    id: 1,
    nome: "Pacchetto Memories",
    descrizione: "Carte ispirate ai ricordi del passato.",
    carte: 5,
    prezzo: 2000,
    fazione: "memories",
  },
  {
    id: 2,
    nome: "Pacchetto Dreams",
    descrizione: "Carte ispirate ai sogni e ai desideri.",
    carte: 5,
    prezzo: 2000,
    fazione: "dreams",
  },
  {
    id: 3,
    nome: "Pacchetto Nightmares",
    descrizione: "Carte ispirate agli incubi più oscuri.",
    carte: 5,
    prezzo: 2000,
    fazione: "nightmares",
  },
];

// Componente per mostrare i crediti
function CreditiDisponibili({ crediti }) {
  return (
    <div className="negozio-crediti">
      <div className="negozio-avatar">C</div>

      <div className="negozio-crediti-info">
        <span>Crediti disponibili</span>
        <strong>{crediti.toLocaleString("it-IT")}</strong>
      </div>
    </div>
  );
}

// Componente riutilizzabile per ogni pacchetto
function PacchettoCard({ pacchetto, onAcquista }) {
  return (
    <article className="negozio-card">
      <div className={`negozio-card-immagine ${pacchetto.fazione}`}>
        <span>{pacchetto.fazione.toUpperCase()}</span>
        <div className="negozio-card-simbolo">
          {pacchetto.fazione === "memories" && "✧"}
          {pacchetto.fazione === "dreams" && "☾"}
          {pacchetto.fazione === "nightmares" && "✦"}
        </div>
      </div>

      <div className="negozio-card-testo">
        <h3>{pacchetto.nome}</h3>
        <p>{pacchetto.descrizione}</p>
      </div>

      <span className="negozio-badge">{pacchetto.carte} carte</span>

      <div className="negozio-prezzo">
        <div className="negozio-moneta">C</div>
        <strong>{pacchetto.prezzo.toLocaleString("it-IT")}</strong>
      </div>

      <button
        className="negozio-btn-acquista"
        onClick={() => onAcquista(pacchetto)}
      >
        Acquista
      </button>
    </article>
  );
}

// Componente per il bundle speciale
function BundleSpeciale({ onAcquista }) {
  return (
    <section className="negozio-bundle">
      <div className="negozio-bundle-pacchetti">
        <div className="negozio-mini-card memories">
          <span>MEMORIES</span>
        </div>

        <div className="negozio-mini-card dreams">
          <span>DREAMS</span>
        </div>

        <div className="negozio-mini-card nightmares">
          <span>NIGHTMARES</span>
        </div>
      </div>

      <div className="negozio-bundle-info">
        <span className="negozio-offerta">OFFERTA DEL GIORNO</span>

        <h3>Bundle speciale</h3>

        <p>Tre pacchetti: Memories, Dreams e Nightmares.</p>

        <small>3 pacchetti · 15 carte totali</small>
      </div>

      <div className="negozio-bundle-prezzo">
        <del>6.500</del>

        <strong>5.000</strong>

        <button className="negozio-btn-bundle" onClick={onAcquista}>
          <span className="negozio-moneta">C</span>
          Approfitta dell'offerta
        </button>
      </div>
    </section>
  );
}

// Pagina principale
export default function NegozioPage() {
  const [crediti, setCrediti] = useState(12450);
  const [messaggio, setMessaggio] = useState("");

  const acquistaPacchetto = (pacchetto) => {
    if (crediti < pacchetto.prezzo) {
      setMessaggio("Non hai abbastanza crediti!");
      return;
    }

    setCrediti((precedenti) => precedenti - pacchetto.prezzo);

    setMessaggio(`Hai acquistato ${pacchetto.nome}!`);
  };

  const acquistaBundle = () => {
    if (crediti < 5000) {
      setMessaggio("Non hai abbastanza crediti!");
      return;
    }

    setCrediti((precedenti) => precedenti - 5000);

    setMessaggio("Hai acquistato il Bundle speciale!");
  };

  return (
    <main className="negozio-page">
      {/* Titolo */}
      <header className="negozio-header">
        <h1>Il tuo negozio</h1>

        <p>
          Acquista pacchetti di carte e controlla i tuoi crediti disponibili.
        </p>
      </header>

      {/* Crediti disponibili */}
      <section className="negozio-barra-crediti">
        <CreditiDisponibili crediti={crediti} />

        <div className="negozio-separatore" />

        <p className="negozio-descrizione-crediti">
          Usa i tuoi crediti per acquistare pacchetti di carte e bundle
          speciali.
        </p>

        <div className="negozio-separatore" />

        <button
          className="negozio-btn-crediti"
          onClick={() =>
            setMessaggio(
              "La funzione per acquistare crediti non è ancora disponibile.",
            )
          }
        >
          Acquista crediti
        </button>
      </section>

      {/* Pacchetti */}
      <section className="negozio-pacchetti">
        <h2>Pacchetti disponibili</h2>

        <div className="negozio-griglia">
          {pacchetti.map((pacchetto) => (
            <PacchettoCard
              key={pacchetto.id}
              pacchetto={pacchetto}
              onAcquista={acquistaPacchetto}
            />
          ))}
        </div>
      </section>

      {/* Bundle speciale */}
      <BundleSpeciale onAcquista={acquistaBundle} />

      {/* Messaggio di conferma */}
      {messaggio && (
        <div className="negozio-messaggio" role="status">
          <span>{messaggio}</span>

          <button
            onClick={() => setMessaggio("")}
            aria-label="Chiudi messaggio"
          >
            ×
          </button>
        </div>
      )}
    </main>
  );
}
