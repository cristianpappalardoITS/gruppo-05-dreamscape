package it.somnia.dreamscape.api.mission.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.mission.model.UserMission;
import jakarta.persistence.LockModeType;

public interface UserMissionRepository extends JpaRepository<UserMission, UUID> {

    // Crea la riga del periodo con avanzamento 0, se non c'è già. Con ON CONFLICT due partite nello stesso
    // momento non provano a creare due righe uguali (vincolo uq_user_missions_period)
    @Modifying
    @Query(value = """
            INSERT INTO user_missions (user_id, mission_id, period_start)
            VALUES (:userId, :missionId, :periodStart)
            ON CONFLICT (user_id, mission_id, period_start) DO NOTHING
            """, nativeQuery = true)
    void insertIfMissing(UUID userId, UUID missionId, LocalDate periodStart);

    // La riga del periodo, bloccata: due partite dello stesso giocatore la aggiornano una alla volta e il premio
    // arriva una volta sola
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT um FROM UserMission um
            WHERE um.user.id = :userId AND um.mission.id = :missionId AND um.periodStart = :periodStart
            """)
    Optional<UserMission> findForUpdate(UUID userId, UUID missionId, LocalDate periodStart);

    // Le righe di un giocatore che iniziano in uno dei giorni dati (Dashboard: oggi e il lunedì della settimana)
    @Query("""
            SELECT um FROM UserMission um
            WHERE um.user.id = :userId AND um.periodStart IN :periodStarts
            """)
    List<UserMission> findByUserAndPeriodStarts(UUID userId, Collection<LocalDate> periodStarts);
}
