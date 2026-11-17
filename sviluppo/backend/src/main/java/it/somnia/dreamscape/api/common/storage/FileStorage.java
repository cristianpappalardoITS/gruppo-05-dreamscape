package it.somnia.dreamscape.api.common.storage;

/**
 * Salva i file del backend. È l'unico modo di scrivere file: su disco oggi, su un servizio esterno
 * (es. S3) domani, senza cambiare chi lo usa (guida SWD, "Pensato per un possibile rilascio su AWS").
 *
 * <p>I percorsi sono relativi e con le barre normali, es. {@code cards/base-004-v1.webp}.
 */
public interface FileStorage {

    /**
     * Salva un'immagine pubblica, servita su {@code /images/} + il percorso.
     *
     * @param path    il percorso relativo, es. {@code cards/base-004-v1.webp}
     * @param content i byte del file
     */
    void saveImage(String path, byte[] content);

    /**
     * Salva un'immagine pubblica solo se il percorso è ancora libero, in un'unica operazione: due salvataggi nello
     * stesso momento con lo stesso percorso non si sovrascrivono, uno dei due riceve {@code false}.
     *
     * @param path    il percorso relativo, es. {@code cards/base-004-v1.webp}
     * @param content i byte del file
     * @return {@code true} se il file è stato creato, {@code false} se c'era già
     */
    boolean createImage(String path, byte[] content);

    /**
     * Dice se un'immagine pubblica esiste già (serve per scegliere il numero di versione).
     *
     * @param path il percorso relativo
     * @return {@code true} se il file c'è
     */
    boolean imageExists(String path);

    /**
     * Salva un file originale, che non si serve al pubblico.
     *
     * @param path    il percorso relativo, es. {@code cards/base-004-v1.png}
     * @param content i byte del file
     */
    void saveOriginal(String path, byte[] content);
}
