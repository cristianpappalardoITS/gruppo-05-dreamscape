import GameCard from "../Card/GameCard.jsx";

const cardTitles = [
  "Carta più utilizzata",
  "Seconda più utilizzata",
  "Terza più utilizzata",
];

export default function PlayerProfile({
  user = {},
  won = 0,
  lost = 0,
  cards = [],
  credits = 0,
  season = "Ultima stagione",
  onShop,
}) {
  // Valori calcolati dai dati ricevuti
  const played = won + lost;
  const winRate = played ? (won / played) * 100 : 0;
  const topCards = [...cards].sort((a, b) => b.duels - a.duels).slice(0, 3);

  const stats = [
    { label: "Duelli svolti", value: played },
    { label: "Duelli vinti", value: won },
    { label: "Duelli persi", value: lost },
    {
      label: "Rateo vittoria",
      value:
        winRate.toLocaleString("it-IT", {
          minimumFractionDigits: 1,
          maximumFractionDigits: 1,
        }) + "%",
    },
  ];

  return (
    <main className="profilo">
      <h1>Il tuo profilo</h1>

      <section className="user">
        <div className="avatar">{user.name?.[0]?.toUpperCase()}</div>
        <div>
          <h2 className="user-name">{user.name}</h2>
          <p className="user-email">{user.email}</p>
        </div>
      </section>

      <section>
        <div className="section-head">
          <h2>Statistiche duelli</h2>
          <span className="season">{season}</span>
        </div>
        <div className="stats">
          {stats.map((s) => (
            <div key={s.label} className="stat">
              <span className="stat-label">{s.label}</span>
              <span className="stat-value">{s.value}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="bottom">
        <section className="panel">
          <h2>Le tue carte preferite</h2>
          <p className="subtitle">Le carte che hai utilizzato più spesso</p>
          <div className="cards">
            {topCards.map((card, index) => (
              <div key={card.id} className="favorite-card">
                <GameCard card={card} />
                <p className="favorite-card-usage">
                  <span>{cardTitles[index]}</span>
                  <strong>{card.duels} duelli</strong>
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="panel">
          <h2>I tuoi crediti</h2>
          <p className="subtitle">Saldo disponibile nell’account</p>
          <div className="credits">
            <div className="coin">C</div>
            <span className="credits-value">
              {credits.toLocaleString("it-IT")}
            </span>
          </div>
          <button className="shop-btn" onClick={onShop}>
            Vai allo shop
          </button>
        </section>
      </div>
    </main>
  );
}
