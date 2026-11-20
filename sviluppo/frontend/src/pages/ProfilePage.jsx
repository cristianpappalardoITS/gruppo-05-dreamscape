import ProfileNavbar from "../components/Profile/ProfileNavbar.jsx";
import PlayerProfile from "../components/Profile/PlayerProfile.jsx";
import "@fontsource/inter/400.css";
import "@fontsource/inter/700.css";
import "@fontsource/cinzel/400.css";
import "./ProfilePage.css";
import refugeIllustration from "../../../../carte/illustrazioni/base-001.png";
import flightIllustration from "../../../../carte/illustrazioni/base-002.png";
import lighthouseIllustration from "../../../../carte/illustrazioni/base-003.png";

// Dati di esempio: nel gioco arriveranno dallo stato o da un'API
const player = {
  user: { name: "User", email: "users@example.com" },
  won: 162,
  lost: 86,
  credits: 12450,
  cards: [
    {
      id: 1,
      name: "Il Rifugio Sicuro",
      faction: { code: "DREAMS", name: "Dreams" },
      rarity: { code: "COMMON", name: "Comune" },
      attack: 5,
      defense: 13,
      description: "Il desiderio di un posto dove nulla può raggiungerti.",
      effect: {
        name: "Barriera",
        text: "Il primo colpo che subisce non le fa danni.",
      },
      illustrationUrl: refugeIllustration,
      illustrationThumbUrl: refugeIllustration,
      illustrationFocusY: 0.4,
      duels: 97,
    },
    {
      id: 2,
      name: "Il Volo Sopra la Città",
      faction: { code: "DREAMS", name: "Dreams" },
      rarity: { code: "RARE", name: "Rara" },
      attack: 9,
      defense: 11,
      description: "Per una notte, la gravità smette di contare.",
      effect: {
        name: "Immunità",
        text: "È immune agli effetti delle carte avversarie che colpiscono la creatura.",
      },
      illustrationUrl: flightIllustration,
      illustrationThumbUrl: flightIllustration,
      illustrationFocusY: 0.4,
      duels: 82,
    },
    {
      id: 3,
      name: "Il Faro nella Nebbia",
      faction: { code: "DREAMS", name: "Dreams" },
      rarity: { code: "EPIC", name: "Epica" },
      attack: 7,
      defense: 13,
      description: "Una luce lontana che indica sempre la strada di casa.",
      effect: {
        name: "Riposo",
        value: 4,
        text: "Quando entra, il tuo giocatore guadagna 4 punti vita.",
      },
      illustrationUrl: lighthouseIllustration,
      illustrationThumbUrl: lighthouseIllustration,
      illustrationFocusY: 0.4,
      duels: 69,
    },
  ],
};

export default function ProfilePage() {
  return (
    <div className="profile-page">
      <ProfileNavbar />
      <PlayerProfile {...player} />
    </div>
  );
}
