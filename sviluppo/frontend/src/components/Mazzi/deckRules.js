// Controlli del mazzo nell'editor, con i numeri di GET /rules. Sono gli stessi del backend
// (DeckValidator): il backend controlla comunque al salvataggio.

// Nome al plurale di ogni rarità, per i contatori e i problemi
export const rarityPlural = { COMMON: 'Comuni', RARE: 'Rare', EPIC: 'Epiche', LEGENDARY: 'Leggendarie' };

// Normale e full art della stessa carta contano insieme per le copie massime
export function copyGroupId(card) {
  return card.baseCardId ?? card.id;
}

// Problemi del mazzo, con le stesse frasi del backend. deckEntries: [{ card, quantity }],
// owned: { cardId: copie possedute }
export function deckProblems(deckEntries, rules, owned) {
  const problems = [];
  const total = deckEntries.reduce((sum, entry) => sum + entry.quantity, 0);
  if (total !== rules.deckSize) {
    problems.push(`Il mazzo ha ${total} carte: ne servono ${rules.deckSize}.`);
  }

  const rarityByCode = Object.fromEntries(rules.rarities.map((rarity) => [rarity.code, rarity]));

  // Copie per carta (normale e full art insieme)
  const groups = new Map();
  deckEntries.forEach(({ card, quantity }) => {
    const id = copyGroupId(card);
    const group = groups.get(id) ?? { name: card.name, rarity: card.rarity.code, copies: 0 };
    group.copies += quantity;
    groups.set(id, group);
  });
  groups.forEach((group) => {
    const maxCopies = rarityByCode[group.rarity]?.maxCopies;
    if (maxCopies != null && group.copies > maxCopies) {
      problems.push(`Le copie di ${group.name} sono ${group.copies}: il massimo è ${maxCopies} (normale e full art contano insieme).`);
    }
  });

  // Carte per rarità
  rules.rarities.forEach((rarity) => {
    if (rarity.maxPerDeck == null) return;
    const count = deckEntries
      .filter((entry) => entry.card.rarity.code === rarity.code)
      .reduce((sum, entry) => sum + entry.quantity, 0);
    if (count > rarity.maxPerDeck) {
      problems.push(`Le ${rarityPlural[rarity.code] ?? rarity.name} sono ${count}: il massimo è ${rarity.maxPerDeck}.`);
    }
  });

  // Copie possedute
  deckEntries.forEach(({ card, quantity }) => {
    const ownedCopies = owned[card.id] ?? 0;
    if (ownedCopies === 0) problems.push(`Non possiedi la carta ${card.name}.`);
    else if (quantity > ownedCopies) {
      problems.push(`Possiedi ${ownedCopies} ${ownedCopies === 1 ? 'copia' : 'copie'} di ${card.name}: nel mazzo ce ne sono ${quantity}.`);
    }
  });

  return problems;
}

// Si può aggiungere un'altra copia di questa carta senza superare un limite?
export function canAddCopy(card, deckEntries, rules, owned) {
  const total = deckEntries.reduce((sum, entry) => sum + entry.quantity, 0);
  if (total >= rules.deckSize) return false;

  const inDeck = deckEntries.find((entry) => entry.card.id === card.id)?.quantity ?? 0;
  if (inDeck >= (owned[card.id] ?? 0)) return false;

  const rarity = rules.rarities.find((item) => item.code === card.rarity.code);
  const groupCopies = deckEntries
    .filter((entry) => copyGroupId(entry.card) === copyGroupId(card))
    .reduce((sum, entry) => sum + entry.quantity, 0);
  if (rarity?.maxCopies != null && groupCopies >= rarity.maxCopies) return false;

  const rarityCount = deckEntries
    .filter((entry) => entry.card.rarity.code === card.rarity.code)
    .reduce((sum, entry) => sum + entry.quantity, 0);
  if (rarity?.maxPerDeck != null && rarityCount >= rarity.maxPerDeck) return false;

  return true;
}
