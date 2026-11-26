package it.somnia.dreamscape.api.admin.dto.response;

import java.time.Instant;
import java.util.List;

import it.somnia.dreamscape.api.user.model.CreditTransactionType;

/**
 * I numeri della Dashboard admin ({@code GET /admin/stats}). "Oggi" e "ultimi 7 giorni" sono in ora italiana: gli
 * ultimi 7 giorni sono oggi e i 6 giorni prima, da mezzanotte.
 *
 * @param generatedAt quando sono stati calcolati
 * @param players     i giocatori (gli admin non contano)
 * @param cards       le carte del catalogo
 * @param packs       i pacchetti e le aperture
 * @param matches     le partite
 * @param credits     i Dream Credits dei giocatori
 */
public record AdminStatsResponse(
        Instant generatedAt,
        Players players,
        Cards cards,
        Packs packs,
        Matches matches,
        Credits credits) {

    /**
     * I giocatori.
     *
     * @param total           gli account attivi
     * @param newToday        registrati oggi
     * @param newLast7Days    registrati negli ultimi 7 giorni
     * @param activeLast7Days account attivi che hanno fatto l'accesso negli ultimi 7 giorni
     */
    public record Players(long total, long newToday, long newLast7Days, long activeLast7Days) {
    }

    /**
     * Le carte del catalogo.
     *
     * @param active   carte normali attive
     * @param fullArt  full art attive
     * @param disabled carte disattivate, normali e full art
     */
    public record Cards(long active, long fullArt, long disabled) {
    }

    /**
     * I pacchetti.
     *
     * @param onSale        pacchetti del negozio in vendita adesso
     * @param openings      aperture in tutto (acquisti, set iniziali e benvenuto)
     * @param openingsToday aperture di oggi
     */
    public record Packs(long onSale, long openings, long openingsToday) {
    }

    /**
     * Le partite, comprese le sfide tra amici (che sono rapide).
     *
     * @param ranked      classificate in tutto
     * @param rankedToday classificate di oggi
     * @param casual      rapide in tutto
     * @param casualToday rapide di oggi
     */
    public record Matches(long ranked, long rankedToday, long casual, long casualToday) {
    }

    /**
     * I Dream Credits dei giocatori.
     *
     * @param inCirculation la somma dei saldi
     * @param last7Days     i movimenti degli ultimi 7 giorni
     */
    public record Credits(long inCirculation, CreditMovements last7Days) {
    }

    /**
     * I movimenti di un periodo.
     *
     * @param earned crediti entrati in tutto (premi, bonus)
     * @param spent  crediti usciti in tutto (acquisti), come numero positivo
     * @param byType gli stessi numeri per tipo di movimento; solo i tipi con almeno un movimento
     */
    public record CreditMovements(long earned, long spent, List<CreditTypeTotals> byType) {
    }

    /**
     * I movimenti di un tipo.
     *
     * @param type         il tipo, es. PACK_PURCHASE
     * @param transactions quanti movimenti
     * @param earned       crediti entrati
     * @param spent        crediti usciti, come numero positivo
     */
    public record CreditTypeTotals(CreditTransactionType type, long transactions, long earned, long spent) {
    }
}
