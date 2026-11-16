package it.somnia.dreamscape.api.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;

// Username ed email sono unici senza distinguere maiuscole e minuscole (indici su LOWER())
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    // La lega di un punteggio: quella con la soglia più alta che il punteggio raggiunge.
    // Le leghe si leggono qui finché non c'è il loro codice (#106).
    @Query(value = """
            SELECT code, name, min_rating AS minRating
            FROM leagues
            WHERE min_rating <= :rating
            ORDER BY min_rating DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<LeagueView> findLeagueForRating(int rating);

    // La lega successiva: la prima con la soglia sopra il punteggio; vuota per la lega più alta
    @Query(value = """
            SELECT code, name, min_rating AS minRating
            FROM leagues
            WHERE min_rating > :rating
            ORDER BY min_rating ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<LeagueView> findNextLeague(int rating);
}
