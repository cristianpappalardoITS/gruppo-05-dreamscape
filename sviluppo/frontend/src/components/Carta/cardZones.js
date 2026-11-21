// Zone della carta in percentuale (left, top, width, height), misurate sulle 12 cornici
// di public/cards/frames. Valgono per qualsiasi dimensione della carta.

// L'illustrazione è un po' più grande della finestra: i bordi restano sotto la cornice
const ART_ZONE = { left: 7, top: 15.8, width: 86, height: 47.6 };

// Attacco e difesa: la parte scura dei due riquadri in basso, dopo la spada e lo scudo.
// L'altezza cambia da cornice a cornice (statY in FRAME_ZONES), la posizione in larghezza no.
const STAT_WIDTH = 20;
const STAT_HEIGHT = 8.4;
const ATTACK_LEFT = 15;
const DEFENSE_LEFT = 75.5;

// Full art: niente cornice, quindi niente spada e scudo. I due riquadri sono simmetrici,
// alla stessa distanza dai bordi della carta
const FULL_ART_STATS = {
  attack: { left: 6, top: 87, width: 18, height: 8 },
  defense: { left: 76, top: 87, width: 18, height: 8 },
};

// Interno della barra del nome e della pergamena del testo, cornice per cornice, e centro in
// altezza dei riquadri di attacco e difesa: le cornici non hanno tutte le parti alla stessa altezza
// (nella leggendaria di Memories, per esempio, i riquadri sono più in alto)
const FRAME_ZONES = {
  dreams: {
    COMMON: { name: [6.6, 5.6, 62.9, 7.5], text: [6.6, 65.3, 86.1, 21.4], statY: 91.0 },
    RARE: { name: [7.3, 5.3, 62.7, 7.8], text: [7.3, 65.1, 85.4, 20.8], statY: 90.5 },
    EPIC: { name: [7.7, 5.5, 62.3, 7.2], text: [8.1, 64.5, 83.5, 19.9], statY: 90.3 },
    LEGENDARY: { name: [8.1, 6.1, 61.5, 7.2], text: [8.5, 65.3, 82.3, 20.0], statY: 90.1 },
  },
  nightmares: {
    COMMON: { name: [6.9, 6.0, 64.1, 7.7], text: [6.6, 66.1, 86.1, 20.8], statY: 90.8 },
    RARE: { name: [7.7, 6.0, 63.3, 7.9], text: [7.3, 66.1, 85.3, 20.2], statY: 90.7 },
    EPIC: { name: [7.7, 6.2, 63.2, 7.9], text: [7.3, 65.9, 84.7, 19.8], statY: 90.2 },
    LEGENDARY: { name: [8.0, 7.2, 63.0, 7.5], text: [8.0, 66.0, 84.0, 19.6], statY: 90.1 },
  },
  memories: {
    COMMON: { name: [7.6, 5.5, 62.7, 8.0], text: [8.0, 65.7, 84.0, 20.7], statY: 90.6 },
    RARE: { name: [7.3, 5.5, 63.4, 8.0], text: [7.6, 65.7, 84.4, 20.4], statY: 90.5 },
    EPIC: { name: [8.0, 5.8, 62.1, 7.7], text: [8.3, 65.8, 82.6, 19.6], statY: 90.2 },
    LEGENDARY: { name: [8.3, 7.0, 61.5, 7.5], text: [8.7, 65.5, 81.9, 19.1], statY: 89.4 },
  },
};

const toZone = ([left, top, width, height]) => ({ left, top, width, height });

// Riquadri di attacco e difesa centrati in altezza su statY
function statZones(statY) {
  const top = statY - STAT_HEIGHT / 2;
  return {
    attack: { left: ATTACK_LEFT, top, width: STAT_WIDTH, height: STAT_HEIGHT },
    defense: { left: DEFENSE_LEFT, top, width: STAT_WIDTH, height: STAT_HEIGHT },
  };
}

// Tutte le zone di una carta
export function cardZones(card) {
  const frame = FRAME_ZONES[card.faction.code.toLowerCase()]?.[card.rarity.code] ?? FRAME_ZONES.dreams.COMMON;
  const stats = card.variantType === 'FULL_ART' ? FULL_ART_STATS : statZones(frame.statY);
  return { art: ART_ZONE, name: toZone(frame.name), text: toZone(frame.text), ...stats };
}

// Cornice per fazione e rarità: la versione nel nome serve per la cache del browser
const RARITY_FILES = { COMMON: 'comune', RARE: 'rara', EPIC: 'epica', LEGENDARY: 'leggendaria' };

export function frameUrl(card) {
  return `/cards/frames/cornice-${card.faction.code.toLowerCase()}-${RARITY_FILES[card.rarity.code]}-v1.webp`;
}

// Stile in linea di una zona
export function zoneStyle({ left, top, width, height }) {
  return { left: `${left}%`, top: `${top}%`, width: `${width}%`, height: `${height}%` };
}
