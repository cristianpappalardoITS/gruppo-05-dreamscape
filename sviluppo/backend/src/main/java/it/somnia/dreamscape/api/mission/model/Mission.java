package it.somnia.dreamscape.api.mission.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.match.model.MatchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Una missione (V9, decisione 015): oggi la giornaliera "Gioca una partita" e la settimanale "Vinci 5 partite".
// Una missione nuova è una riga in più
@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "code", nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 10)
    private MissionPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private MissionGoalType goalType;

    // Quante partite (o vittorie) servono per completarla
    @Column(name = "goal_count", nullable = false)
    private int goalCount;

    @Column(name = "reward_credits", nullable = false)
    private int rewardCredits;

    // Le partite che contano: null = tutte, altrimenti solo quelle di questo tipo
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", length = 10)
    private MatchType matchType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Se una partita di questo tipo, vinta o no, fa avanzare la missione
    public boolean countsMatch(MatchType playedType, boolean won) {
        if (matchType != null && matchType != playedType) {
            return false;
        }
        return goalType == MissionGoalType.PLAY_MATCHES || won;
    }
}
