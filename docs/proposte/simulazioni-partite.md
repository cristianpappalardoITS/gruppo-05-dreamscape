# Simulazioni delle partite: punti vita e limite di round

> **Stato:** proposta, da confermare con Maie e Nicolas (#87).
> Dopo la loro risposta si aggiorna la [decisione 008](../decisioni/008-regole-della-partita.md) e, se il Campanile cambia, si scrive una migration nuova.

## 1. In breve

| Domanda | Proposta | Perché |
|---------|----------|--------|
| Punti vita | **Confermare 20** | Una partita dura in media 22 round e l'88% finisce per punti vita. Con 25 o più, un quarto delle partite finisce per mazzo esaurito |
| Limite di round | **Tenere 60** | Lo tocca solo lo 0,5% delle partite, quasi tutte bloccate. Con 30 si chiuderebbe in pareggio il 17% delle partite |
| Il Campanile tra le Nuvole (BASE-036) | **Permanenza 6 → 5** | Due Campanili uno contro l'altro non muoiono mai: 6 danni e 6 di difesa recuperata a ogni round. Con Permanenza 5 le partite bloccate spariscono |

## 2. Come sono fatte le simulazioni

- Le partite le gioca **il motore vero** (`engine-core`, versione 0.2.0) con le regole a–f della [decisione 029](../decisioni/029-motore-di-gioco.md) e i 19 effetti della [028](../decisioni/028-catalogo-degli-effetti.md).
- Le carte sono le **99 del catalogo vero**, lette dalla migration `V102__seed_base_set.sql`. Vantaggio di fazione: +1 ATT, come in V100.
- **5.000 partite** per ogni combinazione di regole, **15 combinazioni**: punti vita 10, 15, 20, 25 e 30, limite di round 30, 60 e 90. In tutto 75.000 partite.
- Le **stesse partite** (stessi mazzi e stesso seed) si giocano con tutte le regole: le differenze tra le righe dipendono solo dalle regole.
- Ogni mazzo ha 20 carte valide per la [decisione 017](../decisioni/017-mazzi.md), scelte a caso:

| Mazzo | Comuni | Rare | Epiche | Leggendarie | Chi lo rappresenta |
|-------|--------|------|--------|-------------|--------------------|
| Tipico | 11 | 6 | 2 | 1 | Un giocatore a metà strada (lo stesso mazzo usato per bilanciare le carte) |
| Forte | 6 | 8 | 4 | 2 | Il mazzo più forte che le regole permettono |

  Ogni mazzo è di una sola fazione oppure misto, con la stessa probabilità.

### Come rifarle

Da `sviluppo/engine`:
```bash
./gradlew :engine-simulation:run
```
Ci vogliono pochi secondi. Il resoconto completo con tutte le tabelle va in `engine-simulation/build/simulazioni/risultati.md`. Con `--args="--partite 10000 --seed 7"` si cambiano il numero di partite e il seed (di base 5.000 e 87). Con lo stesso seed i numeri sono sempre gli stessi.

## 3. Risultati

### Punti vita (limite 60)

| Punti vita | Round medi | 90% entro il round | Fine per punti vita | Mazzo esaurito | Pareggi |
|-----------:|-----------:|-------------------:|--------------------:|---------------:|--------:|
| 10 | 11,9 | 22 | 98,6% | 1,1% | 0,5% |
| 15 | 17,1 | 29 | 95,3% | 4,3% | 0,9% |
| **20** | **21,8** | **34** | **87,8%** | **11,7%** | **1,5%** |
| 25 | 25,9 | 38 | 75,6% | 23,8% | 2,5% |
| 30 | 29,1 | 40 | 58,7% | 40,6% | 3,6% |

Chi vince, sulle partite con un vincitore:

| Punti vita | Vince chi vince la moneta | Vince il mazzo Forte contro il Tipico | Vince chi ha il vantaggio di fazione | Punti vita rimasti al vincitore |
|-----------:|--------------------------:|--------------------------------------:|-------------------------------------:|-------------------------------:|
| 10 | 49,6% | 74,0% | 67,3% | 6,3 |
| 15 | 49,8% | 80,4% | 69,5% | 8,2 |
| **20** | **49,6%** | **84,8%** | **71,8%** | **10,0** |
| 25 | 50,9% | 87,4% | 74,0% | 12,0 |
| 30 | 49,8% | 90,6% | 75,7% | 14,4 |

Cosa si vede:
- **Con 10 o 15 punti vita** le partite sono corte: con 10, metà finisce entro il round 10. Conta di più il caso: il mazzo Forte vince solo il 74% delle volte contro il Tipico.
- **Con 25 o 30 punti vita** la partita smette di finire per punti vita: una su quattro (25) o due su cinque (30) finiscono perché un giocatore resta senza carte. Vince quasi sempre il mazzo migliore e i pareggi aumentano.
- **Con 20** l'88% delle partite finisce per punti vita, il mazzo migliore conta (85%) ma non decide tutto, e il vincitore resta con metà dei punti vita: partite combattute. Il valore scelto dal cliente regge.
- **La moneta non dà vantaggio:** chi la vince vince circa il 50% delle volte, con tutti i valori.

### Limite di round (20 punti vita)

| Limite | Partite che lo toccano | Pareggi |
|-------:|-----------------------:|--------:|
| 30 | 17,1% | 17,6% |
| **60** | **0,5%** | **1,5%** |
| 90 | 0,5% | 1,5% |

Quanti round dura una partita, con il limite a 90 (così il limite non taglia niente):

| Punti vita | 1-10 | 11-20 | 21-30 | 31-40 | 41-50 | 51-60 | oltre 60 |
|-----------:|-----:|------:|------:|------:|------:|------:|---------:|
| 15 | 25,2% | 45,4% | 21,0% | 6,8% | 1,1% | 0,1% | 0,4% |
| **20** | **9,6%** | **39,9%** | **33,4%** | **13,7%** | **2,5%** | **0,4%** | **0,5%** |
| 25 | 2,7% | 27,9% | 40,3% | 22,6% | 5,1% | 0,7% | 0,7% |

- Un limite di 30 chiude in pareggio partite normali, che sarebbero finite poco dopo.
- Tra 60 e 90 non cambia nulla: le partite che superano i 60 round sono bloccate e andrebbero avanti per sempre. **Il limite serve solo come rete di sicurezza**, e 60 basta.

### Partite bloccate: Il Campanile tra le Nuvole

Con 20 punti vita, 27 partite su 5.000 arrivano al limite di 60. In 26 di queste in campo ci sono **due copie di Il Campanile tra le Nuvole** (BASE-036, Epica Dreams, 6/16, Permanenza 6):

- ognuna fa 6 danni all'altra;
- a fine round Permanenza 6 restituisce 6 di difesa;
- nessuna delle due scende mai a 0 e la partita resta ferma fino al limite.

La 27ª è una partita lunga ma normale (L'Atlante delle Nuvole che recupera difesa contro un mazzo quasi finito), tagliata al round 60.

Due modi per togliere il blocco, provati con le stesse 5.000 partite:

| Campanile | Partite al limite (15 / 20 / 25 punti vita) | Pareggi con 20 punti vita |
|-----------|---------------------------------------------|--------------------------:|
| Oggi: 6/16, Permanenza 6 | 0,4% / 0,5% / 0,6% | 1,5% |
| **Permanenza 5** | **0,0% / 0,0% / 0,0%** | **1,0%** |
| Attacco 7 | 0,0% / 0,0% / 0,0% | 0,9% |

La proposta è **Permanenza 5**: cambia solo la X dell'effetto e la carta resta difensiva com'è. Con attacco 7 la carta diventerebbe più forte.

Regola generale per le carte future: **una carta con Permanenza X deve avere più di X di attacco**, altrimenti contro una copia di sé stessa la partita si blocca.

### Fazioni (20 punti vita, limite 60)

Mazzi di una sola fazione, quota di vittorie della riga contro la colonna:

| Fazione | contro Dreams | contro Nightmares | contro Memories |
|---------|--------------:|------------------:|----------------:|
| Dreams | - | 30,4% | **76,5%** |
| Nightmares | **69,6%** | - | 30,6% |
| Memories | 23,5% | **69,4%** | - |

Chi ha il vantaggio di fazione vince circa 7 partite su 10, con qualsiasi valore di punti vita. Non dipende da punti vita e limite: è la regola del +1 ATT, già segnalata con il bilanciamento del catalogo (#103). Qui la riportiamo solo come dato: la scelta resta a Maie e Nicolas.

## 4. Cosa serve da Maie e Nicolas

1. Confermare **20 punti vita** e **limite di 60 round**.
2. Approvare **Permanenza 5** per Il Campanile tra le Nuvole (oppure scegliere attacco 7).
3. Dire se vogliono rivedere il vantaggio di fazione (fuori da questa proposta).

## 5. Dopo la risposta

- [Decisione 008](../decisioni/008-regole-della-partita.md): "i punti vita restano da verificare con le simulazioni" diventa "verificati con le simulazioni (#87)", con il collegamento a questo documento.
- Se il Campanile cambia: migration nuova con un `UPDATE` di `card_effects` (V102 non si modifica) e testo dell'effetto in `carte/compositore/carte.csv`. Il sito monta la carta da solo (decisione 007): la carta finita con la cornice si rifà con `componi_carte.py` solo se serve per le presentazioni.
- Nessun cambiamento nel motore né in `game_modes` (V6): 20 e 60 sono già i valori di oggi.
