package it.somnia.dreamscape.api.mission.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// L'avanzamento di un giocatore in una missione, per un periodo (V9). La riga nasce alla prima partita che conta
// nel periodo; quella del periodo dopo è una riga nuova, così l'avanzamento riparte da 0
@Entity
@Table(name = "user_missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    // Il primo giorno del periodo: il giorno stesso per le giornaliere, il lunedì per le settimanali
    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    @Column(name = "progress", nullable = false)
    private int progress;

    // Quando è arrivata a goal_count; null finché non è completata
    @Column(name = "completed_at")
    private Instant completedAt;

    // Il movimento del premio (MISSION_REWARD); null se non è completata o se il premio era 0
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_transaction_id")
    private CreditTransaction creditTransaction;

    public boolean isCompleted() {
        return completedAt != null;
    }

    // Una partita in più che conta. Dopo il completamento l'avanzamento resta fermo a goal_count
    public void advance() {
        if (!isCompleted()) {
            progress++;
        }
    }

    // Segna la missione come completata, con il movimento del premio (null se il premio era 0)
    public void complete(Instant when, CreditTransaction reward) {
        this.completedAt = when;
        this.creditTransaction = reward;
    }
}
