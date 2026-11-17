package it.somnia.dreamscape.api.friend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.friend.model.Friendship;
import it.somnia.dreamscape.api.friend.model.FriendshipStatus;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // La riga della coppia, in qualunque direzione e con qualunque stato
    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requester.id = :firstUserId AND f.addressee.id = :secondUserId)
               OR (f.requester.id = :secondUserId AND f.addressee.id = :firstUserId)
            """)
    Optional<Friendship> findBetween(UUID firstUserId, UUID secondUserId);

    // Le righe di un utente con un certo stato, in tutte e due le direzioni, con i due giocatori già caricati
    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            JOIN FETCH f.addressee
            WHERE f.status = :status AND (f.requester.id = :userId OR f.addressee.id = :userId)
            """)
    List<Friendship> findAllOfUser(UUID userId, FriendshipStatus status);

    // Richieste ricevute (addressee) o mandate (requester), dalla più recente
    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            WHERE f.addressee.id = :userId AND f.status = it.somnia.dreamscape.api.friend.model.FriendshipStatus.PENDING
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findReceivedRequests(UUID userId);

    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.addressee
            WHERE f.requester.id = :userId AND f.status = it.somnia.dreamscape.api.friend.model.FriendshipStatus.PENDING
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findSentRequests(UUID userId);

    // Per il contatore della Dashboard
    int countByAddressee_IdAndStatus(UUID addresseeId, FriendshipStatus status);

    // Tutte le righe di un utente, qualunque stato (cancellazione dell'account)
    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            JOIN FETCH f.addressee
            WHERE f.requester.id = :userId OR f.addressee.id = :userId
            """)
    List<Friendship> findAllOfUser(UUID userId);
}
