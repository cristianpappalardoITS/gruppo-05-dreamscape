// true se lo scambio ha almeno un avviso su mazzi o ultime copie
export const hasWarnings = (warnings) => warnings.deckWarnings.length > 0 || warnings.lastCopyWarnings.length > 0;
