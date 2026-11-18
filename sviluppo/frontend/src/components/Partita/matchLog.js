// Frasi della cronaca di una partita (GET /matches/{matchId}). Il log ha solo dati: i nomi
// di carte ed effetti arrivano dalle copie dei mazzi (decks), i valori dagli eventi.
// Formato degli eventi: docs/database/schema.md, "Formato dei campi JSON di matches".

const statNames = { ATTACK: "l'attacco", DEFENSE: 'la difesa', LIFE_POINTS: 'i punti vita' };

const blockedNames = {
  FACTION_BONUS: 'il vantaggio di fazione',
  COMBAT_HIT: 'il colpo',
  PLAYER_DAMAGE: 'il danno al giocatore',
};

export const endReasonNames = {
  LIFE_POINTS: 'punti vita a 0',
  DECK_EXHAUSTED: 'mazzo finito',
  ROUND_LIMIT: 'limite di round',
};

// Nomi di carte ed effetti dalle copie dei due mazzi
export function buildNames(match) {
  const cards = {};
  const effects = {};
  [match.decks?.challenger, match.decks?.opponent].forEach((deck) => {
    (deck?.cards ?? []).forEach((card) => {
      cards[card.cardId] = card;
      (card.effects ?? []).forEach((effect) => { effects[effect.code] = effect.name; });
    });
  });
  return {
    card: (cardId) => cards[cardId]?.name ?? 'una carta',
    cardData: (cardId) => cards[cardId],
    effect: (code) => effects[code] ?? code,
    player: (side) => (side === 'CHALLENGER' ? match.challenger.username : match.opponent.username),
  };
}

// La frase di un evento. field: { CHALLENGER: cardId, OPPONENT: cardId } carte in campo prima dell'evento
export function describeEvent(event, names, field) {
  const other = event.side === 'CHALLENGER' ? 'OPPONENT' : 'CHALLENGER';
  switch (event.type) {
    case 'CARD_PLAYED':
      return `${names.player(event.side)} gioca ${names.card(event.cardId)} (${event.attack} ATT, ${event.defense} DIF).`;
    case 'EFFECT': {
      const effect = `${names.effect(event.code)}${event.value != null ? ` ${event.value}` : ''}`;
      const target = event.target === 'PLAYER'
        ? `i punti vita di ${names.player(event.targetSide)}`
        : `${statNames[event.stat] ?? event.stat} di ${names.card(field[event.targetSide])}`;
      // "i punti vita" è plurale: salgono / scendono
      const plural = event.target === 'PLAYER' || event.stat === 'LIFE_POINTS';
      const verb = event.after < event.before ? (plural ? 'scendono' : 'scende') : (plural ? 'salgono' : 'sale');
      return `${effect} (${names.card(event.cardId)}): ${target} ${verb} da ${event.before} a ${event.after}${event.untilEndOfRound ? ' per questo round' : ''}.`;
    }
    case 'EFFECT_BLOCKED':
      return `${names.effect(event.code)} di ${names.card(event.cardId)} blocca ${blockedNames[event.blocked] ?? names.effect(event.blocked)}.`;
    case 'ATTACK': {
      const total = event.attack + event.factionBonus + event.attackModifier;
      const parts = [`${event.attack}`];
      if (event.factionBonus) parts.push(`+${event.factionBonus} di fazione`);
      if (event.attackModifier) parts.push(`${event.attackModifier > 0 ? '+' : ''}${event.attackModifier} dagli effetti`);
      const detail = parts.length > 1 ? ` (${parts.join(' ')})` : '';
      return `${names.card(event.cardId)} colpisce con ${total}${detail}: la difesa di ${names.card(field[other])} passa da ${event.defenseBefore} a ${event.defenseAfter}.`;
    }
    case 'CARD_DIED':
      return `${names.card(event.cardId)} di ${names.player(event.side)} muore e va negli scarti.`;
    case 'PLAYER_DAMAGE':
      return `${names.player(event.side)} perde ${event.amount} punti vita (da ${event.lifePointsBefore} a ${event.lifePointsAfter}).`;
    case 'BONUS_EXPIRED':
      return `Finisce ${names.effect(event.code)} su ${names.card(event.cardId)}: ${statNames[event.stat] ?? event.stat} ${event.stat === 'LIFE_POINTS' ? 'tornano' : 'torna'} da ${event.before} a ${event.after}.`;
    case 'GAME_OVER':
      return event.winner
        ? `Fine della partita: vince ${names.player(event.winner)} (${endReasonNames[event.endReason] ?? event.endReason}).`
        : `Fine della partita: pareggio (${endReasonNames[event.endReason] ?? event.endReason}).`;
    default:
      return event.type;
  }
}

// Le frasi di un round, tenendo conto delle carte in campo. startField: carte in campo a inizio round
export function describeRound(round, names, startField) {
  const field = { ...startField };
  return round.events.map((event) => {
    if (event.type === 'CARD_PLAYED') field[event.side] = event.cardId;
    const text = describeEvent(event, names, field);
    if (event.type === 'CARD_DIED') field[event.side] = null;
    return { phase: event.phase, type: event.type, text };
  });
}

// Le due carte che combattono nel round: quelle in campo a inizio round più quelle giocate nel round
export function combatCards(round, startField) {
  const field = { ...startField };
  round.events.forEach((event) => {
    if (event.type === 'CARD_PLAYED') field[event.side] = event.cardId;
  });
  return field;
}

// Carte in campo a inizio di ogni round (dallo stato a fine del round prima)
export function startFields(rounds) {
  const fields = [];
  let current = { CHALLENGER: null, OPPONENT: null };
  rounds.forEach((round) => {
    fields.push(current);
    current = { CHALLENGER: round.afterRound.challenger.cardId, OPPONENT: round.afterRound.opponent.cardId };
  });
  return fields;
}

// Numeri di un lato nel round, per il campo: ATT e DIF della carta che ha combattuto (anche se
// è morta), danno subito dalla carta e variazione dei punti vita del giocatore.
// side: 'CHALLENGER' o 'OPPONENT'; lifeBefore: punti vita a inizio round;
// previous: lo stato del lato a fine del round prima (null nel primo round)
export function sideNumbers(round, side, cardId, lifeBefore, previous) {
  const key = side === 'CHALLENGER' ? 'challenger' : 'opponent';
  const after = round.afterRound[key];
  const alive = cardId && after.cardId === cardId;
  // Se la carta era già in campo, si parte dai suoi numeri del round prima
  const wasInField = previous?.cardId === cardId;
  let attack = alive ? after.attack : wasInField ? previous.attack : null;
  let defense = alive ? after.defense : wasInField ? previous.defense : null;
  let damageTaken = 0;

  round.events.forEach((event) => {
    if (event.type === 'CARD_PLAYED' && event.cardId === cardId && attack === null) {
      attack = event.attack;
      if (!alive) defense = event.defense;
    }
    // Il colpo dell'altro lato colpisce la carta di questo lato
    if (event.type === 'ATTACK' && event.side !== side) {
      damageTaken += event.damage;
      if (!alive) defense = event.defenseAfter;
    }
  });

  return { attack, defense, damageTaken, lifeDelta: after.lifePoints - lifeBefore };
}
