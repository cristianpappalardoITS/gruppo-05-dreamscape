"""Crea le copie WebP delle illustrazioni, quelle che il backend serve su /images/cards/.

Per ogni carta di carte.csv prende l'illustrazione PNG da ../illustrazioni/ (e la sua
full art, se c'è) e salva due copie WebP (decisione 007):
- grande, larga 1024 px:   base-004-v1.webp,       base-004-fullart-v1.webp
- miniatura, larga 400 px: base-004-v1-thumb.webp, base-004-fullart-v1-thumb.webp

I PNG originali non vengono mai modificati. Le copie vanno nel backend
(sviluppo/backend/src/main/resources/images/cards/), che le serve con la cache per sempre.

Uso:
    python3 crea_webp.py
    python3 crea_webp.py --solo BASE-004
    python3 crea_webp.py --versione 2 --solo BASE-004

Le copie già presenti non vengono mai sovrascritte, a meno di usare --sovrascrivi.
Un'illustrazione cambiata va salvata con una versione nuova (--versione 2): il browser tiene
le immagini per un anno e non chiederebbe mai di nuovo un file con lo stesso nome.
"""
import argparse
import csv
import os
import sys

try:
    from PIL import Image
except ImportError:
    sys.exit("Manca la libreria Pillow. Installala con: python3 -m pip install pillow")

CARTELLA = os.path.dirname(os.path.abspath(__file__))
CSV_CARTE = os.path.join(CARTELLA, "carte.csv")
ILLUSTRAZIONI = os.path.normpath(os.path.join(CARTELLA, "..", "illustrazioni"))
USCITA = os.path.normpath(os.path.join(
    CARTELLA, "..", "..", "sviluppo", "backend", "src", "main", "resources", "images", "cards"))

# Larghezze delle due copie (decisione 007); l'altezza segue le proporzioni dell'originale
LARGHEZZA_GRANDE = 1024
LARGHEZZA_MINIATURA = 400

# Qualità WebP da 0 a 100: 82 tiene i dettagli delle illustrazioni con file piccoli
QUALITA = 82


def leggi_carte():
    """Codice e nome del file PNG di ogni carta del CSV."""
    with open(CSV_CARTE, encoding="utf-8", newline="") as file:
        return [(riga["codice"], riga["illustrazione"]) for riga in csv.DictReader(file, delimiter=";")]


def illustrazioni_della_carta(file_png):
    """Le illustrazioni da convertire: la normale e, se esiste, la full art (base-004-fullart.png)."""
    nome = os.path.splitext(file_png)[0]
    file_full_art = nome + "-fullart.png"
    illustrazioni = [file_png]
    if os.path.exists(os.path.join(ILLUSTRAZIONI, file_full_art)):
        illustrazioni.append(file_full_art)
    return illustrazioni


def salva_copia(immagine, larghezza, percorso):
    altezza = round(immagine.height * larghezza / immagine.width)
    copia = immagine.resize((larghezza, altezza), Image.Resampling.LANCZOS)
    copia.save(percorso, "WEBP", quality=QUALITA, method=6)


def converti(file_png, versione, cartella_uscita, sovrascrivi):
    """Salva grande e miniatura di un'illustrazione. Restituisce quante copie ha scritto."""
    nome = os.path.splitext(file_png)[0]
    copie = [
        (LARGHEZZA_GRANDE, f"{nome}-v{versione}.webp"),
        (LARGHEZZA_MINIATURA, f"{nome}-v{versione}-thumb.webp"),
    ]
    da_scrivere = [(larghezza, os.path.join(cartella_uscita, file))
                   for larghezza, file in copie
                   if sovrascrivi or not os.path.exists(os.path.join(cartella_uscita, file))]
    if not da_scrivere:
        return 0

    with Image.open(os.path.join(ILLUSTRAZIONI, file_png)) as originale:
        # WebP accetta solo RGB e RGBA: le altre modalità (es. tavolozza) si convertono
        immagine = originale if originale.mode in ("RGB", "RGBA") else originale.convert("RGB")
        if immagine.width < LARGHEZZA_GRANDE:
            print(f"  attenzione: {file_png} è larga solo {immagine.width} px, la copia grande viene ingrandita")
        for larghezza, percorso in da_scrivere:
            salva_copia(immagine, larghezza, percorso)
    return len(da_scrivere)


def main():
    parser = argparse.ArgumentParser(description="Crea le copie WebP delle illustrazioni delle carte.")
    parser.add_argument("--solo", metavar="CODICE", help="converte solo questa carta (es. BASE-004)")
    parser.add_argument("--versione", type=int, default=1, help="numero di versione nel nome dei file (di base 1)")
    parser.add_argument("--uscita", default=USCITA, help="cartella in cui salvare le copie")
    parser.add_argument("--sovrascrivi", action="store_true", help="riscrive anche le copie che esistono già")
    argomenti = parser.parse_args()

    carte = leggi_carte()
    if argomenti.solo:
        carte = [carta for carta in carte if carta[0] == argomenti.solo.upper()]
        if not carte:
            sys.exit(f"Nessuna carta con codice {argomenti.solo} in carte.csv")

    os.makedirs(argomenti.uscita, exist_ok=True)
    scritte = 0
    mancanti = []
    for codice, file_png in carte:
        if not file_png or not os.path.exists(os.path.join(ILLUSTRAZIONI, file_png)):
            mancanti.append(codice)
            continue
        for illustrazione in illustrazioni_della_carta(file_png):
            scritte += converti(illustrazione, argomenti.versione, argomenti.uscita, argomenti.sovrascrivi)

    print(f"Copie WebP scritte: {scritte} (in {argomenti.uscita})")
    if mancanti:
        print(f"Manca l'illustrazione di: {', '.join(mancanti)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
