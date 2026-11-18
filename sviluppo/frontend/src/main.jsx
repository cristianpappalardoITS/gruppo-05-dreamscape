import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import './styles/design-tokens.css'

const pathname = window.location.pathname.replace(/\/$/, '') || '/'
const page = pathname === '/login'
  ? <LoginPage />
  : pathname === '/register'
    ? <RegisterPage />
    : <HomePage />

document.title = pathname === '/login'
  ? 'Dreamscape | Accedi'
  : pathname === '/register'
    ? 'Dreamscape | Registrati'
    : 'Dreamscape | Home'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {page}
  </StrictMode>,
)
