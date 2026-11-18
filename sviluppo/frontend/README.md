# Frontend Dreamscape

Frontend React e Vite del progetto Dreamscape. Come parlare con il backend è spiegato nella [guida per il frontend](../../docs/api/guida-frontend.md).

## Avvio

Serve il backend acceso su `http://localhost:8080` (vedi la guida, capitolo 3). Poi, da questa cartella:

```bash
npm install
npm run dev
```

Su Windows, se PowerShell blocca `npm`, si usa `npm.cmd` al suo posto.

Apri `http://localhost:5173/`. In sviluppo Vite passa al backend tutto quello che inizia con `/api` e `/images`.

## Pagine

| Percorso | Pagina | Dati |
|----------|--------|------|
| `/` | Home | `GET /factions`, `GET /rules`, `GET /cards?featured=true` |
| `/login` | Accesso | `POST /auth/login` |
| `/register` | Registrazione, poi accesso e passaggio al Benvenuto | `POST /auth/register` |
| `/dashboard` | Riepilogo: crediti, lega, cose in attesa, missioni e ultime carte (serve l'accesso) | `GET /me/summary`, `GET /me/welcome` |
| `/benvenuto` | Scelta della fazione, set iniziale e pacchetti di benvenuto (serve l'accesso) | `GET/POST /me/welcome`, `GET /factions`, `GET /cards/{cardId}` |
| `/collezione` | Collezione con filtri e dettaglio della carta (serve l'accesso) | `GET /me/cards` |
| `/mazzi` | I mazzi, con validità e problemi; eliminazione (serve l'accesso) | `GET /decks`, `DELETE /decks/{deckId}` |
| `/negozio` | Pacchetti in vendita con il saldo e l'acquisto (serve l'accesso) | `GET /packs`, `GET /me/summary`, `POST /packs/{packId}/open` |
| `/negozio/apertura/{openingId}` | Apertura del pacchetto comprato, una carta alla volta (serve l'accesso) | `GET /pack-openings/{openingId}` |
| `/amici` | Cerca un Dreamweaver, richieste ricevute e inviate, amici (serve l'accesso) | `GET /users`, `GET/POST /friend-requests`, `GET /friends`, `DELETE /friends/{userId}` |
| `/scambi` | Scambi ricevuti, inviati e storico; accetta (con gli avvisi), rifiuta, annulla (serve l'accesso) | `GET /trades`, `GET /trades/{tradeId}/deck-warnings`, `POST /trades/{tradeId}/accept`, `/reject`, `/cancel` |
| `/scambi/nuovo` | Proposta di scambio a un amico, anche `?amico=` (serve l'accesso) | `GET /me/cards`, `GET /users/{userId}/cards`, `POST /trades/preview`, `POST /trades` |
| `/mazzi/editor` | Nuovo mazzo, o modifica con `?id=` (serve l'accesso): contatori in diretta con i numeri del regolamento | `GET /rules`, `GET /me/cards`, `GET /decks/{deckId}`, `POST /decks`, `PUT /decks/{deckId}` |

I percorsi che non esistono ancora (per esempio `/admin`) mostrano la home. Dopo l'accesso il giocatore va alla Dashboard, l'admin a `/admin`.

In sviluppo usate Chrome o Firefox: Safari non salva il cookie del refresh token su `http://localhost` e a ogni cambio pagina si perde l'accesso (guida per il frontend, 4.2).

## Organizzazione

```text
public/
  artwork/                  Immagini usate dalle pagine
src/
  components/
    branding/               Simboli riutilizzati del marchio
    Benvenuto/              Apertura dei pacchetti e stili della pagina Benvenuto
    Collezione/             Carta nella griglia e dettaglio della carta
    Dashboard/              Stili della dashboard
    home/                   Intestazione, footer e icone della home
    Mazzi/                  Regole del mazzo per l'editor e stili dei mazzi
    Negozio/                Stili del negozio
    Scambi/                 Carte e avvisi degli scambi, stili di amici e scambi
    Login/                  Campi e componenti dei form di accesso
  pages/                    HomePage, LoginPage, RegisterPage, DashboardPage, WelcomePage, CollectionPage, DecksPage, DeckEditorPage, ShopPage, PackOpeningPage, FriendsPage, TradesPage, NewTradePage
  styles/                   Palette e stili della home
  api.js                    L'unico file che parla con il backend (dalla guida)
  main.jsx                  Pagina in base al percorso e rinnovo del token all'avvio
```

Il percorso `/images` è riservato alle immagini delle carte che arrivano dal backend, quindi le immagini del frontend stanno in `/artwork`.

## Verifica

```bash
npm run lint
npm run build
```
