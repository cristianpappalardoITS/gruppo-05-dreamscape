# Frontend Dreamscape

Frontend React e Vite del progetto Dreamscape.

## Avvio

```powershell
npm.cmd install
npm.cmd run dev
```

Apri `http://localhost:5173/`. La home statica è la pagina iniziale. Le pagine di accesso e registrazione sono su `/login` e `/register`.

## Organizzazione

```text
public/
  artwork/                  Immagini usate dalle pagine
src/
  components/
    branding/               Simboli riutilizzati del marchio
    home/                   Componenti della home
    Login/                  Campi e componenti dei form di accesso
  pages/                    HomePage, LoginPage, RegisterPage
  styles/                   Palette e stili della home
  api.js                    Chiamate al backend per l'autenticazione
  main.jsx                  Selezione della pagina in base al percorso
```

L'illustrazione principale si trova in `public/artwork/dreamscape-worlds.png`. Il percorso `/images` è riservato al proxy del backend, quindi gli asset statici del frontend stanno in `/artwork`.

## Verifica

```powershell
npm.cmd run lint
npm.cmd run build
```
