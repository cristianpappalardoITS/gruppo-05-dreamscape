package it.somnia.dreamscape.api.common.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Numeri del gioco che non stanno nelle tabelle (application.properties, prefisso dreamscape.game).
 *
 * @param signupBonus Dream Credits alla registrazione
 * @param dailyReward Dream Credits al primo accesso del giorno
 * @param deckSize    carte in un mazzo, uguale per tutte le modalità
 * @param timeZone    fuso del "giorno" di ricompense, missioni e limiti giornalieri
 * @param rankedWin   Dream Credits per una classificata vinta da chi l'ha avviata, con il limite giornaliero
 */
@ConfigurationProperties(prefix = "dreamscape.game")
public record GameProperties(int signupBonus, int dailyReward, int deckSize, ZoneId timeZone,
        RankedWinReward rankedWin) {

    /**
     * Il premio per le classificate vinte (decisione 010): niente se l'avversario è un amico.
     *
     * @param reward    Dream Credits per vittoria
     * @param maxPerDay vittorie premiate al giorno (ora italiana); dalla successiva niente crediti
     */
    public record RankedWinReward(int reward, int maxPerDay) {
    }
}
