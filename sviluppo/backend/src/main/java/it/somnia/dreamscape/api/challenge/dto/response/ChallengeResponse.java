package it.somnia.dreamscape.api.challenge.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;

/**
 * Una sfida in attesa ({@code GET /challenges}, {@code POST /challenges}).
 *
 * <p>{@code challengerDeckValid} e {@code challengerDeckProblems} si calcolano a ogni lettura: un mazzo valido
 * quando è stata lanciata la sfida può non esserlo più (scambio, carta disattivata, mazzo cancellato). Chi ha
 * sfidato lo vede tra le inviate e deve sistemarlo; chi è stato sfidato lo vede tra le ricevute prima di accettare.
 *
 * @param id                     l'id della sfida
 * @param challenger             chi ha lanciato la sfida
 * @param opponent               l'amico sfidato
 * @param challengerDeckId       il mazzo di chi ha sfidato; null se è stato cancellato
 * @param challengerDeckName     il suo nome; null se è stato cancellato
 * @param challengerDeckValid    se il mazzo di chi ha sfidato si può giocare adesso
 * @param challengerDeckProblems perché non si può giocare; lista vuota se è valido
 * @param createdAt              quando è stata lanciata
 */
public record ChallengeResponse(
        UUID id,
        UserSummaryResponse challenger,
        UserSummaryResponse opponent,
        UUID challengerDeckId,
        String challengerDeckName,
        boolean challengerDeckValid,
        List<String> challengerDeckProblems,
        Instant createdAt) {
}
