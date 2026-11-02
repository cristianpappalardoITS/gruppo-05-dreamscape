# Guida per Digital Strategy · si parte il 2 novembre

Per **Sara, Valerio e Maie**. Dal 2 novembre quello che preparate diventa parte del gioco: il catalogo delle carte finisce nel database, le illustrazioni nelle carte montate dal sito, i testi nelle pagine pubbliche. Questa guida spiega quali parole e numeri usare e come consegnare i file, così Software e Web li usano senza riscriverli e senza errori.

> Se un numero o una parola qui non vi torna, ditelo a Marco o Cristian **prima** di consegnare. Le cose ancora da decidere sono in [cose da decidere del 2/11](../riunioni/2026-11-02-cose-da-decidere.md). Quello che Software vi deve è nella [guida di Software Development](software-development.md#6-cosa-dobbiamo-agli-altri-team).

## In breve: le 8 regole da non sbagliare

1. **Si usano le parole del glossario** (sezione 2): Dreamweaver, Dream Pack, Dream Credits, Leggendaria, round. Sempre le stesse, in tutti i testi.
2. **I numeri del gioco sono quelli della sezione 3.** Se volete cambiarne uno, si decide prima con Marco, Cristian e Nicolas, poi si cambia ovunque.
3. **Il catalogo si consegna nel file `carte.csv`**, una riga per carta, con le colonne del compositore (sezione 4).
4. **Gli effetti si prendono solo dalla [lista di Nicolas](../../carte/effetti/Effetti%20Keyword%20Descrizioni.txt)**: niente effetti nuovi senza parlarne con lui.
5. **Le illustrazioni si chiamano come il codice della carta** (`base-013.png`), in PNG con i metadati originali.
6. **I colori sono quelli del design system di Gaia**: nessun colore nuovo senza dirlo a Web.
7. **Niente denaro reale in fase 1**: nei testi non si promettono acquisti, prezzi in euro o offerte.
8. **Ogni consegna passa da una issue**: avvisate Marco o Cristian quando è pronta. Le card sulla board le spostano solo loro.

---

## 1. Account GitHub e consegne

- Nel repository oggi c'è solo Valerio (`decosm`). **Sara e Maie** devono mandare il loro username a Cristian e accettare l'invito, altrimenti non si possono assegnare le loro issue.
- Le consegne vanno nel repository con una **pull request**. Non serve installare niente: si fa dal sito di GitHub.
  1. Aprite la cartella giusta nel repository (vedi la tabella qui sotto).
  2. **Add file → Upload files** e trascinate i file.
  3. In basso scegliete **Create a new branch** e chiamatelo `docs/numero-issue-descrizione`, es. `docs/74-catalogo-fase-1`.
  4. Messaggio: `docs(card): aggiunge il catalogo della fase 1 (#74)`.
  5. **Propose changes**, poi **Create pull request**, con `Closes #74` nella descrizione.
- Se non avete ancora l'account, mandate i file a Marco o Cristian, che li caricano per voi.

| Cosa | Dove va | Issue |
|------|---------|-------|
| Catalogo delle carte | `carte/compositore/carte.csv` | #74 |
| Illustrazioni | `carte/illustrazioni/` | #84 |
| Testi per generare le illustrazioni | `carte/testi-generazione/illustrazioni.md` | #84 |
| Copertine dei Dream Pack | `carte/copertine/` (proposta) | #91 |
| Testi delle pagine pubbliche | `docs/testi-sito/`, un file `.md` per pagina (proposta) | #85 |
| Identità visiva definitiva | `docs/identita/` (proposta) | #75 |

Le cartelle segnate "proposta" si confermano il 2/11.

## 2. Glossario: parole fisse

| ✅ Si scrive | ❌ Non si scrive | Note |
|--------------|-----------------|------|
| **Dreamscape** | Dream Scape, DreamScape | Il gioco |
| **Somnia** | | Il nostro gruppo |
| **Dreamweaver** | giocatore (nei testi per i giocatori), Dream Weaver | Chi gioca |
| **Dreams**, **Nightmares**, **Memories** | Sogni, Incubi, Ricordi (per ora) | Le fazioni. I nomi italiani sono ancora da decidere |
| **Lord** | Boss, Re | La Leggendaria più iconica di ogni fazione |
| **Comune, Rara, Epica, Leggendaria** | Mitica, Ultra Rara, TIER | Le quattro rarità |
| **full art** | alternate art, versione oro | Copia rara di una Leggendaria, con l'illustrazione su tutta la carta |
| **Dream Pack** | Pack Base, Pack Elite, bustina premium | L'unico pacchetto in vendita |
| **Dream Credits (DC)** | crediti oro, gemme, CR | La moneta del gioco |
| **creatura** | mostro, unità | Come nella lista degli effetti |
| **mazzo** | deck | |
| **round** | turno | Un round = uno scontro tra due creature |
| **ATT**, **DIF** | ATK, DEF, forza | Attacco e difesa |
| **modalità Base** | modalità classica | C'è una sola modalità |

## 3. Numeri già decisi

Sono quelli della presentazione al cliente e del documento di Sara confermato il 31/10. Vanno usati così come sono nei testi, nel catalogo e nel marketing.

### Fazioni e vantaggio

| Fazione | Tema | Simbolo | Lord |
|---------|------|---------|------|
| Dreams | Desideri, possibilità, meraviglia | Luna | Il Sogno Lucido (BASE-004) |
| Nightmares | Paure, ansie, perdita di controllo | Occhio | L'Uomo Senza Volto (BASE-008) |
| Memories | Passato, nostalgia, identità | Clessidra | La Custode del Tempo Fermo (BASE-012) |

**Dreams batte Memories, Memories batte Nightmares, Nightmares batte Dreams.** Chi è in vantaggio attacca con **+1 ATT**. Vale il giro del regolamento, non quello inverso.

### Dream Pack e Dream Credits

| Dream Pack | |
|------------|--|
| Prezzo | 500 DC |
| Carte | 5: 3 Comuni, 1 Rara, 1 slot speciale |
| Slot speciale | Rara 70% · Epica 25% · Leggendaria 4% · Leggendaria full art 1% |
| Copertine | Tre, una per Lord, con lo stesso contenuto |

| Come si guadagnano i DC | DC |
|-------------------------|----|
| Registrazione | 500 |
| Accesso giornaliero | 50 |
| Missione giornaliera | 100 |
| Vittoria (massimo 3 al giorno) | 30 |
| Obiettivo settimanale | 250 |

1.000 DC = 1 € serve **solo** per le proiezioni economiche: non si scrive nei testi per i giocatori.

### Dotazione iniziale

Alla registrazione il Dreamweaver riceve un **set iniziale di 20 carte** (14 Comuni e 6 Rare) e **2 pacchetti di benvenuto**, uno con un'Epica garantita.

### Regole del mazzo e della partita

| Regola | Valore |
|--------|--------|
| Carte nel mazzo | 20 |
| Copie della stessa carta | Massimo 2 (1 per le Leggendarie) |
| Limiti per rarità | Leggendarie 2 · Epiche 4 · Rare 8 · Comuni libere |
| Punti vita | 20 (da verificare con le simulazioni) |
| Fine della partita | Punti vita a 0, mazzo esaurito o 60 round (pareggio) |
| Effetti | 23, dalla lista di Nicolas |

## 4. Il catalogo delle carte (#74)

Il catalogo si scrive in [`carte/compositore/carte.csv`](../../carte/compositore/carte.csv), che si apre con Excel, Numbers o Fogli Google (separatore punto e virgola, UTF-8). Dallo stesso file SWD crea i dati del database e lo script crea le carte di prova: per questo il formato va rispettato alla lettera.

| Colonna | Regola | ✅ Esempio |
|---------|--------|-----------|
| `codice` | `BASE-` + numero a 3 cifre, senza buchi, mai riusato. Le 12 carte di prova restano `BASE-001`–`BASE-012` | `BASE-013` |
| `nome` | Unico, con l'iniziale maiuscola per le parole principali, come quelli del set | `Il Faro nella Nebbia` |
| `fazione` | Esattamente `Dreams`, `Nightmares` o `Memories` | `Memories` |
| `rarita` | Esattamente `Comune`, `Rara`, `Epica` o `Leggendaria` | `Rara` |
| `attacco`, `difesa` | Numeri interi | `4`, `5` |
| `descrizione` | Una frase breve di lore, in corsivo sulla carta | `Una luce lontana che indica sempre la strada di casa.` |
| `effetto` | Keyword della lista di Nicolas, con il valore X se serve, poi due punti e la spiegazione. Vuoto se la carta non ha effetti | `Indebolimento 2: la creatura avversaria ha 2 attacco in meno.` |
| `illustrazione` | Il codice in minuscolo, `.png` | `base-013.png` |
| `zoom` | Vuoto | |
| `centro_y` | Vuoto, oppure da 0 (alto) a 1 (basso) per spostare il ritaglio | `0,27` |

Regole del catalogo:

- **Circa 30 carte**, il numero preciso lo chiediamo al cliente il 2/11.
- **Almeno 7 Comuni e 3 Rare diverse**: servono per il set iniziale di 14 Comuni e 6 Rare con al massimo 2 copie per carta.
- Le tre fazioni con un numero simile di carte per rarità.
- Una sola Leggendaria per fazione è il Lord: le altre Leggendarie no.
- Ogni Leggendaria ha anche la sua full art.
- Gli effetti stampati oggi sulle carte di prova (es. "Guscio", "Slancio onirico") vengono dal primo catalogo: vanno sostituiti con quelli della lista di Nicolas, come indica il regolamento.
- Se un testo è troppo lungo, lo script lo rimpicciolisce: accorciatelo finché si legge bene anche sul telefono.
- Il bilanciamento di ATT e DIF lo fate con Nicolas (#66, poi #103): i numeri si confermano con le simulazioni del motore.

## 5. Le illustrazioni (#84)

- Si generano con i testi di [illustrazioni.md](../../carte/testi-generazione/illustrazioni.md) e il loro messaggio iniziale, così lo stile resta uguale a quello delle 12 carte di prova.
- **Formato:** orizzontale 3:2 per le carte con cornice, verticale 2:3 per le full art ([fullart.md](../../carte/testi-generazione/fullart.md)).
- **Solo l'illustrazione:** niente cornice, testi, numeri o simboli. Cornice e testi li aggiunge il sito.
- **Nome del file:** il codice in minuscolo, `base-013.png`; la full art `base-013-fullart.png`.
- **PNG originale, con i suoi metadati:** non convertitela, non comprimetela e non ritagliatela. Le copie leggere per il sito le crea SWD.
- Le versioni scartate vanno in `carte/illustrazioni/alternative/`, non si cancellano.
- Per ogni carta nuova aggiungete il suo testo di generazione in `illustrazioni.md`, così si può rigenerare con lo stesso stile.

## 6. Testi delle pagine pubbliche (#85)

Pagine: Home, Il gioco e lore, Regolamento per i giocatori, FAQ, Glossario.

- Un file Markdown per pagina, es. `docs/testi-sito/home.md`, con titoli e paragrafi già nell'ordine della pagina. Web li copia nell'HTML.
- Si parla al Dreamweaver con il **tu**, in modo semplice (proposta, da confermare il 2/11).
- Il regolamento per i giocatori usa **gli stessi numeri** della sezione 3 e del [regolamento](../proposte/proposta-regolamento-dreamscape.pdf). Se un valore cambia, cambia in entrambi.
- Sotto ogni titolo, una frase breve per i motori di ricerca (meta description, massimo ~155 caratteri).
- Niente dati da inventare: numero di giocatori, recensioni, premi o date di uscita.

## 7. Identità visiva definitiva (#75)

Dopo la scelta del cliente del 2/11 (anche il retro delle carte):

- Logo in **SVG** e in **PNG** con sfondo trasparente, nella versione chiara e in quella scura.
- Palette e caratteri restano quelli del design system di Gaia: Midnight `#111522`, Antique Gold `#C9955D`, Ivory `#E8D9C4`, Dream Blue `#8FC8F4`; Cinzel, Cormorant Garamond, Inter. Se un colore cambia, si decide con Gaia, perché è già nel codice del sito.
- Nomi dei file in minuscolo con i trattini: `logo-dreamscape.svg`, `retro-carte.png`.

## 8. Copertine dei Dream Pack (#91)

- Tre copertine, una per Lord, con l'illustrazione full art del Lord, come le bustine della presentazione.
- Nomi: `base-dreams.png`, `base-nightmares.png`, `base-memories.png`.
- In PNG originale: le copie per il sito le crea SWD.

## 9. A chi chiedere

| Domanda | Chi |
|---------|-----|
| Effetti, statistiche, bilanciamento | Nicolas |
| Dove mettere un file, account GitHub, numeri già decisi | Marco, Cristian |
| Colori, caratteri, come appare una pagina | Gaia |
| Decisioni del gruppo da registrare | Marco o Cristian, che le scrivono in `docs/decisioni/` |
