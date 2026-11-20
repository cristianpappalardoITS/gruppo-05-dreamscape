import dreamsCommon from "../../../../../carte/cornici/cornice_dreams_comune.png";
import dreamsRare from "../../../../../carte/cornici/cornice_dreams_rara.png";
import dreamsEpic from "../../../../../carte/cornici/cornice_dreams_epica.png";
import dreamsLegendary from "../../../../../carte/cornici/cornice_dreams_leggendaria.png";
import nightmaresCommon from "../../../../../carte/cornici/cornice_nightmares_comune.png";
import nightmaresRare from "../../../../../carte/cornici/cornice_nightmares_rara.png";
import nightmaresEpic from "../../../../../carte/cornici/cornice_nightmares_epica.png";
import nightmaresLegendary from "../../../../../carte/cornici/cornice_nightmares_leggendaria.png";
import memoriesCommon from "../../../../../carte/cornici/cornice_memories_comune.png";
import memoriesRare from "../../../../../carte/cornici/cornice_memories_rara.png";
import memoriesEpic from "../../../../../carte/cornici/cornice_memories_epica.png";
import memoriesLegendary from "../../../../../carte/cornici/cornice_memories_leggendaria.png";

const frames = {
  DREAMS: {
    COMMON: dreamsCommon,
    RARE: dreamsRare,
    EPIC: dreamsEpic,
    LEGENDARY: dreamsLegendary,
  },
  NIGHTMARES: {
    COMMON: nightmaresCommon,
    RARE: nightmaresRare,
    EPIC: nightmaresEpic,
    LEGENDARY: nightmaresLegendary,
  },
  MEMORIES: {
    COMMON: memoriesCommon,
    RARE: memoriesRare,
    EPIC: memoriesEpic,
    LEGENDARY: memoriesLegendary,
  },
};

const layouts = {
  COMMON: {
    artwork: [5.66, 12.11, 88.87, 48.7],
    name: [11.72, 3.39, 66.41, 7.03],
    description: [9.77, 64.45, 80.57, 7.94],
    effect: [9.77, 75.39, 80.57, 10.68],
    attack: [19.24, 91.93],
    defense: [91.21, 91.93],
  },
  RARE: {
    artwork: [5.66, 12.5, 88.67, 47.53],
    name: [16.6, 4.04, 62.5, 6.77],
    description: [9.77, 64.13, 80.57, 8.14],
    effect: [9.77, 75.39, 80.57, 10.42],
    attack: [19.34, 91.67],
    defense: [89.65, 91.67],
  },
  EPIC: {
    artwork: [7.62, 15.49, 84.96, 42.58],
    name: [20.99, 6.77, 58.59, 7.03],
    description: [10.74, 62.17, 78.61, 7.23],
    effect: [10.74, 72.4, 78.61, 11.07],
    attack: [20.12, 90.62],
    defense: [88.48, 90.62],
  },
  LEGENDARY: {
    artwork: [18.55, 17.06, 63.28, 41.4],
    name: [24.41, 8.85, 48.83, 6.9],
    description: [21.68, 60.22, 56.44, 8.66],
    effect: [21.68, 71.74, 56.44, 11.07],
    attack: [27.2, 88.93],
    defense: [80.37, 88.93],
  },
};

export function getCardFrame(card) {
  return frames[card.faction?.code?.toUpperCase()]?.[
    card.rarity?.code?.toUpperCase()
  ];
}

export function getCardLayout(card) {
  return layouts[card.rarity?.code?.toUpperCase()] ?? layouts.COMMON;
}
