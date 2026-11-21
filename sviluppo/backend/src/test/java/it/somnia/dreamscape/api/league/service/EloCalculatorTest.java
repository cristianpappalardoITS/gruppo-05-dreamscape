package it.somnia.dreamscape.api.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import it.somnia.dreamscape.api.league.service.EloCalculator.RatingChanges;

// Calcolo Elo con K = 32 (decisione 010): una variazione arrotondata per chi avvia la partita, l'opposto
// all'avversario, il pareggio vale mezzo punto e il punteggio non scende sotto 0
class EloCalculatorTest {

    @Test
    void calculate_sameRatingChallengerWins_movesHalfOfK() {
        assertThat(EloCalculator.calculate(1000, 1000, 1)).isEqualTo(new RatingChanges(16, -16));
    }

    @Test
    void calculate_sameRatingChallengerLoses_movesHalfOfK() {
        assertThat(EloCalculator.calculate(1000, 1000, 0)).isEqualTo(new RatingChanges(-16, 16));
    }

    @Test
    void calculate_sameRatingDraw_changesNothing() {
        assertThat(EloCalculator.calculate(1200, 1200, 0.5)).isEqualTo(new RatingChanges(0, 0));
    }

    @Test
    void calculate_strongerPlayerWins_gainsLittle() {
        // Atteso 0,76: 32 × 0,24 = 7,7 → 8
        assertThat(EloCalculator.calculate(1200, 1000, 1)).isEqualTo(new RatingChanges(8, -8));
    }

    @Test
    void calculate_weakerPlayerWins_gainsMuch() {
        // Atteso 0,24: 32 × 0,76 = 24,3 → 24
        assertThat(EloCalculator.calculate(1000, 1200, 1)).isEqualTo(new RatingChanges(24, -24));
    }

    @Test
    void calculate_drawBetweenDifferentRatings_weakerPlayerGains() {
        // Atteso 0,24: 32 × 0,26 = 8,3 → 8
        assertThat(EloCalculator.calculate(1000, 1200, 0.5)).isEqualTo(new RatingChanges(8, -8));
        assertThat(EloCalculator.calculate(1200, 1000, 0.5)).isEqualTo(new RatingChanges(-8, 8));
    }

    @Test
    void calculate_hugeGap_neverMovesMoreThanK() {
        RatingChanges upset = EloCalculator.calculate(0, 3000, 1);

        assertThat(upset.challenger()).isEqualTo(EloCalculator.K_FACTOR);
        assertThat(upset.opponent()).isEqualTo(-EloCalculator.K_FACTOR);
    }

    @Test
    void calculate_loserNearZero_losesOnlyWhatItHas() {
        // Il vincitore prende comunque i suoi 16 punti: è l'unico caso in cui le due variazioni non sono opposte
        assertThat(EloCalculator.calculate(10, 10, 1)).isEqualTo(new RatingChanges(16, -10));
        assertThat(EloCalculator.calculate(5, 5, 0)).isEqualTo(new RatingChanges(-5, 16));
    }

    @Test
    void calculate_scoreNotZeroHalfOrOne_throwsException() {
        assertThatThrownBy(() -> EloCalculator.calculate(1000, 1000, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_negativeRating_throwsException() {
        assertThatThrownBy(() -> EloCalculator.calculate(-1, 1000, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
