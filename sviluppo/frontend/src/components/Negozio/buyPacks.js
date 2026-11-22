import { api } from '../../api.js';

// Quanti pacchetti si comprano con "x10"
export const BULK_COUNT = 10;

// Compra count pacchetti uguali, uno dopo l'altro (POST /packs/{packId}/open: il backend ne apre
// uno per volta). onProgress riceve quanti ne sono già stati aperti. Se un acquisto non riesce
// (es. crediti finiti) si ferma: restituisce gli id delle aperture riuscite e l'errore.
export async function buyPacks(pack, count, onProgress) {
  const openingIds = [];
  for (let bought = 0; bought < count; bought += 1) {
    try {
      const opening = await api(`/packs/${pack.id}/open`, { method: 'POST' });
      openingIds.push(opening.openingId);
      onProgress?.(openingIds.length);
    } catch (error) {
      return { openingIds, error };
    }
  }
  return { openingIds, error: null };
}

// Pagina dell'apertura: con più pacchetti la serie va nell'indirizzo (?serie=id1,id2,...),
// così ogni pacchetto si apre nella sua pagina e alla fine c'è il riepilogo
export function openingUrl(openingIds) {
  const [first] = openingIds;
  return openingIds.length > 1
    ? `/negozio/apertura/${first}?serie=${openingIds.join(',')}`
    : `/negozio/apertura/${first}`;
}

export function seriesSummaryUrl(openingIds) {
  return `/negozio/riepilogo?serie=${openingIds.join(',')}`;
}

// La serie letta dall'indirizzo della pagina (vuota se il pacchetto è uno solo)
export function seriesFromLocation() {
  const value = new URLSearchParams(window.location.search).get('serie');
  return value ? value.split(',').filter(Boolean) : [];
}
