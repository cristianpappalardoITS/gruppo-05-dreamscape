# Compositore delle carte

Crea le carte finite partendo da:
- le **cornici** per fazione e rarità (`../cornici/`);
- le **illustrazioni** senza cornice né testi (`../illustrazioni/`);
- i **dati delle carte** (`carte.csv`).

Per ogni carta scrive nome, simbolo di fazione, descrizione, effetto, attacco e difesa. Le carte finite vanno in `../carte-finali/`.

Le carte già presenti non vengono mai sovrascritte, a meno di usare `--sovrascrivi`.

## Prima volta

Serve Python 3 con la libreria Pillow:

```bash
python3 -m pip install -r requirements.txt
```

## Come si usa

Dalla cartella `carte/compositore/`:

```bash
python3 componi_carte.py
```

| Comando | Cosa fa |
|---------|---------|
| `python3 componi_carte.py` | Crea le carte che hanno un'illustrazione e non esistono ancora |
| `python3 componi_carte.py --sovrascrivi` | Rigenera tutte le carte (per esempio dopo aver cambiato una statistica nel CSV) |
| `python3 componi_carte.py --solo BASE-004` | Crea solo quella carta |
| `python3 componi_carte.py --illustrazioni CARTELLA` | Prende le illustrazioni da un'altra cartella |
| `python3 componi_carte.py --uscita CARTELLA` | Salva le carte in un'altra cartella |

## Nomi delle illustrazioni

| File | Carta creata |
|------|--------------|
| `base-001.png` | `BASE-001_il-rifugio-sicuro.png` (versione con cornice) |
| `base-001-A.png`, `base-001-B.png`... | Una carta per variante (`..._A.png`, `..._B.png`), per confrontarle |
| `base-004-fullart.png` | `BASE-004_il-sogno-lucido_FULLART.png` (versione full art) |

I file nelle sottocartelle (come `illustrazioni/alternative/`) vengono ignorati.

Formato consigliato: **orizzontale 3:2** per le carte con cornice, **verticale 2:3** per le full art.

## Il file `carte.csv`

Si apre con Excel, Numbers o Fogli Google (separatore punto e virgola, codifica UTF-8).

| Colonna | Esempio | Note |
|---------|---------|------|
| `codice` | `BASE-001` | Codice univoco della carta |
| `nome` | `Il Rifugio Sicuro` | |
| `fazione` | `Dreams` | `Dreams`, `Nightmares` o `Memories` |
| `rarita` | `Comune` | `Comune`, `Rara`, `Epica` o `Leggendaria` |
| `attacco`, `difesa` | `5`, `13` | |
| `descrizione` | `Il desiderio di un posto...` | In corsivo, sopra la riga |
| `effetto` | `Barriera: il primo colpo che subisce...` | La parte prima dei due punti va in grassetto |
| `illustrazione` | `base-001.png` | Nome del file in `../illustrazioni/` |
| `zoom` | vuoto | Facoltativo. Lasciare vuoto (decisione del gruppo: niente zoom) |
| `centro_y` | `0,27` | Facoltativo. Sposta il ritaglio in verticale: 0 = alto, 1 = basso. Vuoto = 0,4 |

## Come vengono composte le carte

**Carte con cornice**
- L'illustrazione va **sotto** la cornice e si vede solo nella finestra verde (#00FF00). Nessuna decorazione viene coperta e tra illustrazione e cornice non resta spazio vuoto.
- Targhetta del nome, riquadro del testo e cerchio della fazione vengono **misurati in automatico** su ogni cornice.
- Attacco e difesa sono **nella stessa posizione in tutte le carte** (`NUMERI_FISSI`). Eccezione decisa dal gruppo: nelle cornici Dreams Comune e Rara lo scudo è disegnato più a destra, quindi lì la difesa è spostata (`NUMERI_ECCEZIONI`).

**Full art (solo Leggendarie)**
- L'illustrazione copre tutta la carta.
- Nome, simbolo, testi, attacco e difesa stanno su pannelli scuri semitrasparenti, con lo sfondo sfocato dietro per la leggibilità.
- Bordo sottile nel colore della fazione. Posizioni e trasparenza sono nella tabella `FULL_ART`.

## Copie WebP per il backend

Il sito non usa le carte finite: la carta la monta il frontend, con l'illustrazione servita dal backend su `/images/cards/` (decisione 007). `crea_webp.py` crea, per ogni carta di `carte.csv`, due copie WebP dell'illustrazione e della sua full art: grande (larga 1024 px) e miniatura (400 px). I PNG originali non vengono toccati.

```bash
python3 crea_webp.py
```

| Comando | Cosa fa |
|---------|---------|
| `python3 crea_webp.py` | Crea le copie che non esistono ancora |
| `python3 crea_webp.py --solo BASE-004` | Solo quella carta (normale e full art) |
| `python3 crea_webp.py --versione 2 --solo BASE-004` | Copie con la versione 2 nel nome, per un'illustrazione cambiata |
| `python3 crea_webp.py --sovrascrivi` | Riscrive anche le copie che esistono già |
| `python3 crea_webp.py --uscita CARTELLA` | Salva le copie in un'altra cartella |

| Originale | Copie |
|-----------|-------|
| `base-004.png` | `base-004-v1.webp`, `base-004-v1-thumb.webp` |
| `base-004-fullart.png` | `base-004-fullart-v1.webp`, `base-004-fullart-v1-thumb.webp` |

Le copie vanno in `sviluppo/backend/src/main/resources/images/cards/`, dentro il backend. Il browser tiene ogni immagine per un anno, quindi un'illustrazione cambiata non si sovrascrive: si crea con `--versione 2` e una migration nuova aggiorna gli URL della carta.

## Se qualcosa non torna

- **"manca l'illustrazione"**: il nome nella colonna `illustrazione` non corrisponde a un file in `../illustrazioni/`.
- **Testo troppo piccolo**: lo script lo rimpicciolisce finché ci sta; accorcia descrizione o effetto.
- **Font**: su Mac usa Palatino, su Windows Palatino Linotype. Per un altro font metti `serif-regular.ttf`, `serif-italic.ttf` e `serif-bold.ttf` in una cartella `font/` accanto allo script.
