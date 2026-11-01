# Colloquio con il cliente · Testi delle slide

Primo incontro con il cliente: **lunedì 2 novembre 2026**. 24 slide in **9 minuti e 40 secondi** (su 10 a disposizione), poi circa 5 minuti di domande.

Presentano **Cristian** e **Marco**. Le slide usano il design system di Web Development (Midnight `#111522`, Antique Gold `#C9955D`, Ivory `#E8D9C4`, Dream Blue `#8FC8F4`; Cinzel, Cormorant Garamond, Inter) e raccolgono il lavoro dei tre team. Per ogni slide c'è cosa si vede e cosa si dice a voce.

> Regola per tutte le slide: poche parole e immagini grandi. Quello che serve spiegare si dice a voce, non si legge.

## Scaletta

| Blocco | Tempo | Durata | Contenuto | Chi presenta | Slide |
|--------|-------|--------|-----------|--------------|-------|
| A | 0:00–3:40 | 3:40 | Chi siamo, brief, concept, mercato, marketing, identità visiva, fazioni e carte | Cristian | 1–10 |
| B | 3:40–8:10 | 4:30 | Lord, economia di gioco, regole, proposta tecnica, sito e mockup | Marco | 11–21 |
| C | 8:10–9:05 | 0:55 | Metodo di lavoro e board | Cristian | 22–23 |
| D | 9:05–9:40 | 0:35 | Cosa chiediamo al cliente, chiusura | Marco | 24 |
| | 9:40–15:00 | ~5:00 | Domande del cliente | Entrambi | Si torna alle slide che servono |

| N. | Slide | Chi | Inizio | Durata |
|----|-------|-----|--------|--------|
| 1 | Titolo | Cristian | 0:00 | 10 s |
| 2 | Chi siamo | Cristian | 0:10 | 25 s |
| 3 | Cosa ci avete chiesto | Cristian | 0:35 | 30 s |
| 4 | Concept e pubblico | Cristian | 1:05 | 30 s |
| 5 | Carte e scontri automatici | Cristian | 1:35 | 20 s |
| 6 | Come ci facciamo conoscere | Cristian | 1:55 | 25 s |
| 7 | Direzione visiva | Cristian | 2:20 | 20 s |
| 8 | Tre regni, tre fazioni | Cristian | 2:40 | 25 s |
| 9 | Quattro rarità per fazione | Cristian | 3:05 | 15 s |
| 10 | Il retro delle carte | Cristian | 3:20 | 20 s |
| 11 | I Lord delle fazioni | Marco | 3:40 | 25 s |
| 12 | Dream Pack e Dream Credits | Marco | 4:05 | 40 s |
| 13 | Le regole in breve | Marco | 4:45 | 25 s |
| 14 | Un round d'esempio | Marco | 5:10 | 25 s |
| 15 | Funzionalità | Marco | 5:35 | 25 s |
| 16 | Architettura e frontend | Marco | 6:00 | 40 s |
| 17 | Mappa del sito e percorsi | Marco | 6:40 | 15 s |
| 18 | I wireframe | Marco | 6:55 | 15 s |
| 19 | Mockup: negozio e collezione | Marco | 7:10 | 20 s |
| 20 | Mockup: apertura del pacchetto | Marco | 7:30 | 20 s |
| 21 | Mockup: scambio tra amici | Marco | 7:50 | 20 s |
| 22 | Come lavoriamo | Cristian | 8:10 | 35 s |
| 23 | La nostra board | Cristian | 8:45 | 20 s |
| 24 | Cosa vi chiediamo oggi | Marco | 9:05 | 35 s |

Cambi di voce: tre (Cristian → Marco alla 11 → Cristian alla 22 → Marco alla 24). Chi passa la parola lo dice con una frase: "Ora Marco vi mostra come si gioca" (fine della 10) e "Cristian vi spiega come lavoriamo" (fine della 21).

---

## Blocco A · Chi siamo e il gioco (Cristian, 3:40)

### Slide 1 · Titolo (10 s)

**Sulla slide:** logo di Dreamscape, "Una proposta di Somnia", "Laboratorio Integrato · Gruppo 5 · 2 novembre 2026".

**A voce:** "Buongiorno, siamo Somnia e oggi vi presentiamo Dreamscape, il gioco di carte collezionabili che vorremmo costruire per voi."

### Slide 2 · Chi siamo (25 s)

**Sulla slide:** "Somnia, dal latino «sogni»: 10 persone, 3 competenze, un'unica board".

| Team | Persone | Cosa fa |
|------|---------|---------|
| Software Development | Cristian (PL), Marco (Vice PL), Nicolas | Motore di gioco, backend, database |
| Web Development | Gaia (GL), Alessandro, Daniele, Yuri | Portale, interfaccia, frontend |
| Digital Strategy | Sara (GL), Valerio, Maie | Concept, lore, marketing, economia di gioco |

**A voce:** il nome Somnia, i tre team e cosa fa ognuno. Cristian è il Project Leader, Marco il vice; Gaia e Sara guidano Web e Digital. Lavoriamo tutti sulla stessa board.

### Slide 3 · Cosa ci avete chiesto (30 s)

**Sulla slide:** le sei richieste del brief.

1. Registrazione, crediti virtuali e pacchetti di carte casuali
2. Collezione e più mazzi, con regole di composizione
3. Scambi di carte, solo se l'altro accetta
4. Sfide simulate dal server, round per round, con storico
5. Creature con attacco, difesa e fazioni con vantaggi reciproci
6. Backoffice per gestire carte e pacchetti

In piccolo: "Tutte le richieste obbligatorie sono coperte, e ne aggiungiamo altre. Fuori dalla fase 1: acquisto di crediti con denaro reale."

**A voce:** "Le copriamo tutte e ne aggiungiamo alcune." In fase 1 niente acquisti con denaro reale.

### Slide 4 · Concept e pubblico (30 s)

**Sulla slide:** "Ogni sogno nasconde qualcosa". Un gioco di carte collezionabili digitale in cui sogni, paure e ricordi diventano creature. Pubblico 18–30 anni (gaming, fantasy, collezionismo, su smartphone e PC) e la persona-tipo: Giulia, 23 anni, studentessa e casual gamer.

**A voce:** Giulia scopre i giochi su TikTok e Reels, cerca un universo originale e non sopporta i giochi troppo complessi o la monetizzazione aggressiva.

### Slide 5 · Carte e scontri automatici (20 s)

**Sulla slide:** confronto con i competitor.

| Gioco | Dove si decide | Universo |
|-------|----------------|----------|
| Hearthstone | Costruzione dei mazzi e strategia durante i turni | Warcraft |
| Marvel Snap | Collezionismo, carte digitali e accessibilità | Marvel |
| Super Auto Pets | Sinergie tra unità e combattimenti automatici | Animali e abilità |
| **Dreamscape** | **Mazzo prima dello scontro automatico** | **Sogni, paure e ricordi quotidiani** |

**A voce:** "Dreamscape si decide nel mazzo, prima dello scontro automatico, con emozioni che tutti riconoscono."

### Slide 6 · Come ci facciamo conoscere (25 s)

**Sulla slide:** lo slogan «Entra nel sogno. Affronta l'incubo.», cinque format (Creature Reveal, Pack Opening, Dream or Nightmare?, Dreamscape Community su Discord, Enter the Dreamscape) e il lancio in tre mesi: Teasing, Engagement, Launch verso 5.000 utenti. Canali: TikTok, Instagram, YouTube, Discord, Google Ads.

**A voce:** i format e le tre fasi del lancio. Budget e costi di acquisizione arrivano con il media plan.

### Slide 7 · Direzione visiva (20 s)

**Sulla slide:** "Fantasy, onirica e misteriosa: una base scura e tre atmosfere". La palette di otto colori, i tre caratteri (Cinzel per i titoli, Cormorant per lore e sottotitoli, Inter per l'interfaccia).

**A voce:** oro per Memories, azzurro per Dreams, viola per Nightmares. È il design system di Web Development, lo stesso delle schermate e di questa presentazione.

### Slide 8 · Tre regni, tre fazioni (25 s)

**Sulla slide:** le tre fazioni con simbolo e cornice.

| Fazione | Tema | Simbolo |
|---------|------|---------|
| Dreams | Desideri, possibilità, meraviglia | Luna |
| Nightmares | Paure, ansie, perdita di controllo | Occhio |
| Memories | Passato, nostalgia, identità | Clessidra |

In basso: "Vantaggio +1 ATT · Dreams batte Memories · Memories batte Nightmares · Nightmares batte Dreams".

**A voce:** chi è in vantaggio di fazione attacca con +1.

### Slide 9 · Quattro rarità per fazione (15 s)

**Sulla slide:** le 12 cornici in griglia (3 fazioni × Comune, Rara, Epica, Leggendaria).

**A voce:** "La rarità si riconosce a colpo d'occhio dalla cornice."

### Slide 10 · Il retro delle carte (20 s)

**Sulla slide:** tre proposte: il doppio logo, il vortice, il monolite sul lago.

**A voce:** la scelta del retro è una delle domande che facciamo alla fine. Poi: "Ora Marco vi mostra come si gioca."

---

## Blocco B · Economia, regole e proposta tecnica (Marco, 4:30)

### Slide 11 · I Lord delle fazioni (25 s)

**Sulla slide:** le tre full art: Il Sogno Lucido (Dreams), L'Uomo Senza Volto (Nightmares), La Custode del Tempo Fermo (Memories).

**A voce:** ogni fazione ha un Lord, la sua Leggendaria più iconica. Queste sono le versioni full art, copie rare delle Leggendarie, e sono anche le copertine dei pacchetti.

### Slide 12 · Dream Pack e Dream Credits (40 s)

**Sulla slide:** le tre bustine, "5 carte per pacchetto · 500 DC · 3 Comuni · 1 Rara · 1 slot speciale".

| Slot speciale | Probabilità |
|---------------|-------------|
| Rara | 70% |
| Epica | 25% |
| Leggendaria | 4% |
| Leggendaria full art | 1% |

| I crediti si guadagnano giocando | DC |
|----------------------------------|----|
| Registrazione | 500 |
| Accesso giornaliero | 50 |
| Missione giornaliera | 100 |
| Vittoria (max 3 al giorno) | 30 |
| Obiettivo settimanale | 250 |

In piccolo: "In fase 1 nessun acquisto con denaro reale. 1.000 DC = 1 € solo come riferimento per le proiezioni economiche."

**A voce:** il contenuto del Dream Pack è sempre lo stesso, cambia solo il Lord in copertina. I crediti si guadagnano giocando.

### Slide 13 · Le regole in breve (25 s)

**Sulla slide:** "Una sola modalità, la Base, con gli effetti delle carte".

- 20 carte nel mazzo
- 2 copie al massimo (1 per le Leggendarie)
- 20 punti vita (da verificare)
- +1 attacco con il vantaggio di fazione
- 60 round al massimo, poi pareggio
- Limiti per rarità: Leggendarie 2 · Epiche 4 · Rare 8 · Comuni libere
- 23 effetti: Protezione, Castigo, Miasma, Ritorno…

**A voce:** la partita finisce a punti vita zero, a mazzo esaurito o dopo 60 round. "La strategia sta nella costruzione del mazzo: il server simula la partita, il giocatore la rivede round per round."

### Slide 14 · Un round d'esempio (25 s)

**Sulla slide:** il round 3 della partita di prova del regolamento. Il Faro nella Nebbia (giocatore A, Dreams, Epica, 4/7) contro L'Uomo Senza Volto (giocatore B, Nightmares, Leggendaria, 8/5), in quattro passi:

1. Indebolimento 2: il Faro attacca con 2, non 4
2. Nightmares batte Dreams: l'Uomo attacca con 8 + 1 = 9
3. Colpi contemporanei: il Faro muore, l'Uomo scende a 3
4. Danno al giocatore: 9 − 7 = 2, A passa da 20 a 18

**A voce:** seguire i passi senza leggere tutti i numeri.

### Slide 15 · Funzionalità (25 s)

**Sulla slide:** due colonne.

| Richieste dal brief | In più, proposte da Somnia |
|---------------------|----------------------------|
| Registrazione e accesso | Lista amici: scambi tra amici, in quantità libere |
| Crediti e pacchetti casuali | Effetti delle carte già nella modalità Base |
| Collezione e mazzi | Lord di fazione e copertine dei pacchetti a scelta |
| Scambi | Full art: copia rara da collezionare |
| Sfide, risultato round per round, storico | Set iniziale e pacchetti di benvenuto |
| Backoffice | Rank con leghe a tema |

**A voce:** "A sinistra le richieste del brief: le facciamo tutte. A destra quello che aggiungiamo noi."

### Slide 16 · Architettura e frontend (40 s)

**Sulla slide:** Frontend (HTML, CSS, JavaScript) → API REST (accesso con token JWT) → Spring Boot 4 (Java + motore di gioco) → PostgreSQL 18 (transazioni, Flyway). Sotto, quattro punti:

- **Motore di gioco:** simula le partite sul server; con migliaia di partite di prova bilanciamo le carte
- **Tutto lato server:** pacchetti, partite e scambi in transazioni, nessuno può barare
- **Frontend leggero:** senza framework, responsive per smartphone, tablet e PC, accessibile e indicizzato
- **La carta la monta il frontend:** il backend manda solo i dati, se cambia una statistica le carte si aggiornano da sole

In piccolo: "Password cifrate e token JWT · tutto in Docker · test con JUnit e Testcontainers · codice e board su GitHub".

**A voce:** un passaggio per blocco, poi si passa alle pagine del sito.

### Slide 17 · Mappa del sito e percorsi (15 s)

**Sulla slide:** "Sei aree: ognuna con il percorso che fa il Dreamweaver".

| Area | Si parte da | Percorso |
|------|-------------|----------|
| Pubblica | Landing page | Lore e ambientazione → regolamento base → vetrina delle carte → FAQ, contatti, privacy |
| Accesso | Registrazione | Controllo dei dati sul server → accesso con token JWT → dashboard del Dreamweaver |
| Negozio | Vetrina dei pacchetti | Controllo del saldo → estrazione delle carte sul server → apertura animata |
| Collezione e mazzi | Collezione con filtri | Deck builder → controllo delle regole del mazzo → mazzi salvati |
| Scambi | Proposta di scambio | L'altro accetta o rifiuta → scambio in un'unica transazione → carte nella nuova collezione |
| Sfide | Sfida con un mazzo valido | Partita simulata sul server → risultato e premio → replay nello storico |

In piccolo: "In più il backoffice per l'admin: carte, pacchetti e configurazione."

**A voce:** tutto quello che conta passa dal server.

### Slide 18 · I wireframe (15 s)

**Sulla slide:** quattro wireframe di Web Development da smartphone: negozio, collezione, apertura, apertura con swipe.

**A voce:** "Da qui sono nati i mockup che vediamo adesso."

### Slide 19 · Mockup: negozio e collezione (20 s)

**Sulla slide:** due schermate da smartphone. "Si sceglie il Dream Pack con il Lord preferito, poi si cercano le carte": tre copertine full art con lo stesso contenuto, acquisto con 500 Dream Credits, collezione con ricerca, filtri e copie possedute.

### Slide 20 · Mockup: apertura del pacchetto (20 s)

**Sulla slide:** due schermate da smartphone. "Le carte escono una alla volta, come da una bustina vera": la luce intorno alla carta ha il colore della fazione, con uno swipe arriva la carta successiva, alla fine tutte e 5 vanno nella collezione.

### Slide 21 · Mockup: scambio tra amici (20 s)

**Sulla slide:** la stessa pagina su desktop e smartphone. "Lo scambio avviene solo quando entrambi confermano": ognuno vede l'offerta dell'altro, la propria collezione resta privata.

**A voce:** alla fine, "Cristian vi spiega come lavoriamo."

---

## Blocco C · Come lavoriamo (Cristian, 0:55)

### Slide 22 · Come lavoriamo (35 s)

**Sulla slide:** "Metodo agile con board Kanban su GitHub Projects e sprint di una settimana". 73 issue sulla board (SWD 25, WMD 23, DGS 14, comuni 11), ognuna con responsabile, priorità e sprint. Una issue per attività, un branch e una pull request rivista per ognuna.

| Sprint | Date | Obiettivo |
|--------|------|-----------|
| 1–2 | 19/10 – 01/11 | Fondamenta: concept, regole, carte, database |
| 3 | 02/11 – 08/11 | Basi: progetti, accesso, motore |
| 4 | 09/11 – 15/11 | Catalogo: pacchetti, collezione, mazzi |
| 5 | 16/11 – 22/11 | Effetti: effetti, test, editor; **SAL il 23/11** |
| 6 | 23/11 – 29/11 | Scambi: amici, scambi, sfide e rank |
| 7 | 30/11 – 05/12 | Rifinitura: backoffice, Swagger, deploy |
| 8 | 06/12 – 10/12 | Consegna: prove, demo, **consegna il 10/12** |

**A voce:** ogni attività ha il suo branch e una pull request rivista da un'altra persona. Gli sprint vanno fino al SAL del 23 novembre e alla consegna del 10 dicembre.

### Slide 23 · La nostra board (20 s)

**Sulla slide:** screenshot della board vera, con le colonne Backlog, Todo, In Progress, Review e Done. "73 issue in 8 sprint: ognuna ha team, responsabile, priorità e sprint".

**A voce:** "Potete seguire l'avanzamento in ogni momento."

---

## Blocco D · Chiusura (Marco, 0:35)

### Slide 24 · Cosa vi chiediamo oggi (35 s)

**Sulla slide:** quattro domande e "Grazie. Ora le vostre domande."

1. Quale proposta di retro delle carte preferite?
2. Partite più tattiche (20 punti vita) o più rapide (10)?
3. Quante carte deve avere il catalogo alla consegna?
4. Le ricompense in Dream Credits vi sembrano giuste?

**A voce:** "Le vostre risposte ci servono per partire con lo sviluppo." Leggere le domande e lasciare rispondere durante le domande; il note-taker segna le risposte. Poi: "Grazie: ora siamo a disposizione per le vostre domande."

Se il cliente chiede la nostra proposta:

| Domanda | La nostra proposta |
|---------|--------------------|
| Retro delle carte | Una delle tre proposte di Digital |
| Punti vita | Oggi 20, da confermare con le simulazioni |
| Carte alla consegna | Circa 30 |
| Ricompense | Quelle della slide 12 |

---

## Durante le domande

Non ci sono più slide di riserva: marketing, mappa del sito e wireframe sono entrati nella scaletta (slide 6, 17 e 18). Se una domanda lo richiede, il driver torna alla slide giusta. Per le domande più tecniche si tengono aperti:

- il regolamento completo: [proposta-regolamento-dreamscape.pdf](../proposte/proposta-regolamento-dreamscape.pdf)
- lo schema del database: [schema.md](../database/schema.md)
- l'ipotesi di rilascio su AWS: [rilascio-aws.md](../proposte/rilascio-aws.md)
- le altre domande preparate: [domande-colloquio.md](domande-colloquio.md)
