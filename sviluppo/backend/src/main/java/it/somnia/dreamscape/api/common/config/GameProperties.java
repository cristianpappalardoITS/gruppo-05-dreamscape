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
 */
@ConfigurationProperties(prefix = "dreamscape.game")
public record GameProperties(int signupBonus, int dailyReward, int deckSize, ZoneId timeZone) {
}
