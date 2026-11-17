package it.somnia.dreamscape.api.admin.service;

// Che immagine si carica dal backoffice: decide la cartella, il nome e quante copie WebP si creano
public enum ImageType {

    // Illustrazione orizzontale 3:2 di una carta: copia grande e miniatura in /images/cards/
    ILLUSTRATION,

    // Illustrazione verticale 2:3 della full art: copia grande e miniatura in /images/cards/
    FULL_ART,

    // Immagine di un pacchetto o di una sua copertina: una sola copia in /images/packs/
    COVER
}
