// Suono "da slot" dell'apertura dei pacchetti, fatto con la Web Audio API (nessun file audio).
// Ogni carta: un giro di rulli (tic sempre più acuti), poi un suono che dipende dalla rarità,
// fino al "jackpot" delle Leggendarie. Il muto si ricorda nel browser.
// I browser fanno partire l'audio solo dopo un clic: per questo unlock() va chiamato da un clic
// (es. "Apri il pacchetto").

const MUTE_KEY = 'dreamscape-suono-muto';

let context = null;

function audio() {
  if (!context) {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return null;
    context = new AudioContext();
  }
  return context;
}

export function isMuted() {
  try {
    return window.localStorage.getItem(MUTE_KEY) === '1';
  } catch {
    return false;
  }
}

export function setMuted(muted) {
  try {
    window.localStorage.setItem(MUTE_KEY, muted ? '1' : '0');
  } catch {
    // Senza localStorage il muto vale solo per questa pagina
  }
}

// Da chiamare dentro un clic: sblocca l'audio del browser
export function unlock() {
  const ctx = audio();
  if (ctx && ctx.state === 'suspended') ctx.resume().catch(() => {});
}

// Suona solo con l'audio sbloccato (dopo un clic); se è ancora sospeso prova a riprenderlo
function withAudio(play) {
  if (isMuted()) return;
  const ctx = audio();
  if (!ctx) return;
  if (ctx.state === 'running') play(ctx);
  else ctx.resume().then(() => play(ctx)).catch(() => {});
}

// Una nota breve: frequenza, inizio (secondi da adesso), durata, forma dell'onda e volume
function tone(ctx, frequency, start, duration, type = 'sine', volume = 0.18) {
  const oscillator = ctx.createOscillator();
  const gain = ctx.createGain();
  const at = ctx.currentTime + start;
  oscillator.type = type;
  oscillator.frequency.setValueAtTime(frequency, at);
  gain.gain.setValueAtTime(0.0001, at);
  gain.gain.exponentialRampToValueAtTime(volume, at + 0.01);
  gain.gain.exponentialRampToValueAtTime(0.0001, at + duration);
  oscillator.connect(gain).connect(ctx.destination);
  oscillator.start(at);
  oscillator.stop(at + duration + 0.02);
}

// Giro dei rulli: tic sempre più vicini e più acuti. Restituisce quanto dura, in secondi
function spin(ctx) {
  let time = 0;
  let gap = 0.075;
  for (let tick = 0; tick < 9; tick += 1) {
    tone(ctx, 520 + tick * 45, time, 0.035, 'square', 0.05);
    time += gap;
    gap *= 0.9;
  }
  return time + 0.04;
}

// Note (in Hz) del suono finale per rarità: più è rara, più è lunga e brillante
const RESULT_NOTES = {
  COMMON: [659],
  RARE: [659, 988],
  EPIC: [523, 659, 784, 1047],
  LEGENDARY: [523, 659, 784, 1047, 1319, 1568],
};

// Pioggia di monete del jackpot: tanti tic brillanti a caso
function coins(ctx, start) {
  for (let coin = 0; coin < 14; coin += 1) {
    tone(ctx, 1800 + Math.random() * 1400, start + coin * 0.06 + Math.random() * 0.03, 0.08, 'triangle', 0.06);
  }
}

// Suono di una carta appena uscita: rarity è il codice (COMMON, RARE, EPIC, LEGENDARY)
export function playReveal(rarity, fullArt = false) {
  withAudio((ctx) => revealSound(ctx, rarity, fullArt));
}

function revealSound(ctx, rarity, fullArt) {
  const start = spin(ctx);
  const notes = RESULT_NOTES[rarity] ?? RESULT_NOTES.COMMON;
  const step = rarity === 'LEGENDARY' ? 0.09 : 0.11;
  notes.forEach((frequency, position) => {
    tone(ctx, frequency, start + position * step, rarity === 'COMMON' ? 0.3 : 0.45, 'triangle', 0.16);
  });
  if (rarity === 'LEGENDARY') coins(ctx, start + notes.length * step);
  // Full art: un ultimo scintillio in alto
  if (fullArt) tone(ctx, 2093, start + notes.length * step + 0.1, 0.6, 'sine', 0.1);
}

// Leva della slot: un colpo grave all'apertura del pacchetto
export function playOpen() {
  withAudio((ctx) => {
    tone(ctx, 140, 0, 0.18, 'sawtooth', 0.08);
    tone(ctx, 90, 0.05, 0.22, 'sine', 0.2);
  });
}
