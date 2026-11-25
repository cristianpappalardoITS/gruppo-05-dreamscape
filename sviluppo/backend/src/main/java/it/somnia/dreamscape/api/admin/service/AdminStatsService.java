package it.somnia.dreamscape.api.admin.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.admin.dto.response.AdminStatsResponse;
import it.somnia.dreamscape.api.admin.dto.response.AdminStatsResponse.CreditTypeTotals;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.repository.MatchRepository;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.CreditTransactionRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * I numeri della Dashboard admin ({@code GET /admin/stats}): giocatori, carte, pacchetti, partite e Dream Credits.
 *
 * <p>Contano solo i giocatori, non gli admin. "Oggi" parte da mezzanotte in ora italiana, come le ricompense;
 * "ultimi 7 giorni" sono oggi e i 6 giorni prima. I numeri dei crediti servono anche a Digital per le stime di
 * redditività.
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    // Oggi e i 6 giorni prima
    private static final int LAST_DAYS = 7;

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final PackRepository packRepository;
    private final PackOpeningRepository packOpeningRepository;
    private final MatchRepository matchRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final GameProperties gameProperties;
    private final Clock clock;

    /**
     * Calcola i numeri della Dashboard admin.
     *
     * @return i numeri di adesso
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        Instant now = clock.instant();
        ZoneId zone = gameProperties.timeZone();
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfLastDays = today.minusDays(LAST_DAYS - 1L).atStartOfDay(zone).toInstant();
        return new AdminStatsResponse(
                now,
                new AdminStatsResponse.Players(
                        userRepository.countByRoleAndEnabledTrue(UserRole.PLAYER),
                        userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.PLAYER, startOfToday),
                        userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.PLAYER, startOfLastDays),
                        userRepository.countByRoleAndEnabledTrueAndLastLoginAtGreaterThanEqual(UserRole.PLAYER,
                                startOfLastDays)),
                new AdminStatsResponse.Cards(
                        cardRepository.countByBaseCardIsNullAndActiveTrue(),
                        cardRepository.countByVariantTypeAndActiveTrue(CardVariantType.FULL_ART),
                        cardRepository.countByActiveFalse()),
                new AdminStatsResponse.Packs(
                        packRepository.findOnSale(PackType.SHOP, now).size(),
                        packOpeningRepository.count(),
                        packOpeningRepository.countByOpenedAtGreaterThanEqual(startOfToday)),
                new AdminStatsResponse.Matches(
                        matchRepository.countByMatchType(MatchType.RANKED),
                        matchRepository.countByMatchTypeAndPlayedAtGreaterThanEqual(MatchType.RANKED, startOfToday),
                        matchRepository.countByMatchType(MatchType.CASUAL),
                        matchRepository.countByMatchTypeAndPlayedAtGreaterThanEqual(MatchType.CASUAL, startOfToday)),
                new AdminStatsResponse.Credits(
                        userRepository.sumCreditsByRole(UserRole.PLAYER),
                        creditMovementsSince(startOfLastDays)));
    }

    private AdminStatsResponse.CreditMovements creditMovementsSince(Instant from) {
        List<CreditTypeTotals> byType = creditTransactionRepository.sumByTypeSince(UserRole.PLAYER, from).stream()
                .map(totals -> new CreditTypeTotals(totals.getTransactionType(), totals.getTransactions(),
                        totals.getEarned(), totals.getSpent()))
                .toList();
        return new AdminStatsResponse.CreditMovements(
                byType.stream().mapToLong(CreditTypeTotals::earned).sum(),
                byType.stream().mapToLong(CreditTypeTotals::spent).sum(),
                byType);
    }
}
