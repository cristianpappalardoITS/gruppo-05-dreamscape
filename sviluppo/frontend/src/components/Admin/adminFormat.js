// Formati e nomi italiani usati dalle pagine del backoffice

// Migliaia sempre con il punto (toLocaleString in italiano non lo mette sotto 10.000)
export const formatNumber = (value) => String(value ?? 0).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

// Nomi italiani di codici che arrivano dal backend
export const factionNames = { DREAMS: 'Dreams', NIGHTMARES: 'Nightmares', MEMORIES: 'Memories' };
export const packTypeNames = { SHOP: 'Negozio', STARTER: 'Set iniziale', WELCOME: 'Benvenuto' };
export const creditTypeNames = {
  SIGNUP_BONUS: 'Bonus di registrazione',
  DAILY_REWARD: 'Ricompensa giornaliera',
  PACK_PURCHASE: 'Acquisto di pacchetti',
  MATCH_REWARD: 'Vittorie in classificata',
  MISSION_REWARD: 'Missioni',
  LEAGUE_REWARD: 'Premi di lega',
  ADMIN_ADJUSTMENT: "Correzioni dell'admin",
};
