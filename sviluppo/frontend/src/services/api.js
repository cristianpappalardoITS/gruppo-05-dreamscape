// src/services/api.js

const API_BASE_URL = 'http://localhost:8080/api/v1';

/**
 * Funzione unica per fare chiamate al backend.
 * Aggiunge automaticamente il token di accesso, se presente.
 */
export const apiFetch = async (endpoint, options = {}) => {
  const token = localStorage.getItem('token'); 

  const headers = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`Errore API: ${response.status}`);
  }

  return response.json();
};

/**
 * Recupera la collezione dell'utente dal backend.
 */
export const fetchUserCollection = async () => {
  // --- VERSIONE REALE (da usare quando il backend è pronto) ---
  // return apiFetch('/user/collection');

  // --- VERSIONE FINTIA PER TEST (da rimuovere poi) ---
  return new Promise(resolve => setTimeout(() => {
    resolve({
      cards: [
        // Esempio di carta, sostituisci con i tuoi dati o importa mockCards
        { id: 'BASE-005', name: 'Il Dente Che Cade', faction: 'Nightmares', rarity: 'Comune', attack: 4, defense: 3, lore: 'Un incubo piccolo...', effect: 'Ultimo respiro 1...', owned: true },
        { id: 'BASE-006', name: 'La Caduta Senza Fine', faction: 'Nightmares', rarity: 'Rara', attack: 7, defense: 3, lore: 'La sensazione di perdere il controllo...', effect: 'Vendetta 2...', owned: true },
        // ... aggiungi altre carte ...
      ],
      stats: {
        owned_count: 32,
        total_count: 100
      }
    });
  }, 500));
};