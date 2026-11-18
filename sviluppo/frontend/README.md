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
| `/register` | Registrazione, poi accesso | `POST /auth/register` |
| `/collezione` | Collezione con filtri e dettaglio della carta (serve l'accesso) | `GET /me/cards` |

I percorsi che non esistono ancora (per esempio `/benvenuto` e `/dashboard`) mostrano la home. Finché non ci sono Benvenuto e Dashboard, dopo l'accesso e dopo la registrazione si va alla Collezione.

In sviluppo usate Chrome o Firefox: Safari non salva il cookie del refresh token su `http://localhost` e a ogni cambio pagina si perde l'accesso (guida per il frontend, 4.2).

## Organizzazione

```text
public/
  artwork/                  Immagini usate dalle pagine
src/
  components/
    branding/               Simboli riutilizzati del marchio
    Collezione/             Carta nella griglia e dettaglio della carta
    home/                   Intestazione, footer e icone della home
    Login/                  Campi e componenti dei form di accesso
  pages/                    HomePage, LoginPage, RegisterPage, CollectionPage
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
