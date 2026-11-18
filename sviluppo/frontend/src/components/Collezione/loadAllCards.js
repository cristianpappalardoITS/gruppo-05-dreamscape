import { api } from '../../api.js';

// Tutte le voci di una collezione a pagine (GET /me/cards o GET /users/{userId}/cards),
// 100 per volta, il massimo che il backend accetta
export async function loadAllCards(path) {
  const items = [];
  for (let page = 0; ; page += 1) {
    const data = await api(`${path}?page=${page}&size=100`);
    items.push(...data.items);
    if (page + 1 >= data.totalPages) return items;
  }
}
