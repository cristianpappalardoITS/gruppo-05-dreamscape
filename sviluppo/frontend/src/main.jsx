import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { refresh } from './api.js'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import CollectionPage from './pages/CollectionPage.jsx'
import './styles/design-tokens.css'

// Pagine per percorso: quelle che non esistono ancora (es. /dashboard) mostrano la home
const pages = {
  '/': { title: 'Home', render: () => <HomePage /> },
  '/login': { title: 'Accedi', render: () => <LoginPage /> },
  '/register': { title: 'Registrati', render: () => <RegisterPage /> },
  '/collezione': { title: 'Collezione', render: () => <CollectionPage /> },
}

const pathname = window.location.pathname.replace(/\/$/, '') || '/'
const page = pages[pathname] ?? pages['/']
document.title = `Dreamscape | ${page.title}`

// All'avvio si chiede un token nuovo con il cookie: chi è già collegato non rifà l'accesso
refresh().then(() => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      {page.render()}
    </StrictMode>,
  )
})
