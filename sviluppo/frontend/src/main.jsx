import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { refresh } from './api.js'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import CollectionPage from './pages/CollectionPage.jsx'
import WelcomePage from './pages/WelcomePage.jsx'
import DecksPage from './pages/DecksPage.jsx'
import DeckEditorPage from './pages/DeckEditorPage.jsx'
import ShopPage from './pages/ShopPage.jsx'
import PackOpeningPage from './pages/PackOpeningPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import FriendsPage from './pages/FriendsPage.jsx'
import TradesPage from './pages/TradesPage.jsx'
import NewTradePage from './pages/NewTradePage.jsx'
import ChallengesPage from './pages/ChallengesPage.jsx'
import MatchPage from './pages/MatchPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import './styles/design-tokens.css'

// Pagine per percorso: quelle che non esistono ancora (es. /admin) mostrano la home
const pages = {
  '/': { title: 'Home', render: () => <HomePage /> },
  '/login': { title: 'Accedi', render: () => <LoginPage /> },
  '/register': { title: 'Registrati', render: () => <RegisterPage /> },
  '/dashboard': { title: 'Dashboard', render: () => <DashboardPage /> },
  '/profilo': { title: 'Profilo', render: () => <ProfilePage /> },
  '/benvenuto': { title: 'Benvenuto', render: () => <WelcomePage /> },
  '/collezione': { title: 'Collezione', render: () => <CollectionPage /> },
  '/mazzi': { title: 'Mazzi', render: () => <DecksPage /> },
  '/mazzi/editor': { title: 'Editor del mazzo', render: () => <DeckEditorPage /> },
  '/negozio': { title: 'Negozio', render: () => <ShopPage /> },
  '/amici': { title: 'Amici', render: () => <FriendsPage /> },
  '/scambi': { title: 'Scambi', render: () => <TradesPage /> },
  '/scambi/nuovo': { title: 'Nuovo scambio', render: () => <NewTradePage /> },
  '/sfide': { title: 'Sfide', render: () => <ChallengesPage /> },
}

const pathname = window.location.pathname.replace(/\/$/, '') || '/'
// Pagine con l'id nell'indirizzo: /negozio/apertura/{openingId} e /partite/{matchId}
const openingMatch = pathname.match(/^\/negozio\/apertura\/([^/]+)$/)
const matchMatch = pathname.match(/^\/partite\/([^/]+)$/)
let page = pages[pathname] ?? pages['/']
if (openingMatch) page = { title: 'Apertura del pacchetto', render: () => <PackOpeningPage openingId={openingMatch[1]} /> }
if (matchMatch) page = { title: 'Partita', render: () => <MatchPage matchId={matchMatch[1]} /> }
document.title = `Dreamscape | ${page.title}`

// All'avvio si chiede un token nuovo con il cookie: chi è già collegato non rifà l'accesso
refresh().then(() => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      {page.render()}
    </StrictMode>,
  )
})
