package it.somnia.dreamscape.api.user.repository;

import java.time.Instant;
import java.util.List;
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

    // L'utente collegato a un account Google (accesso con Google, #159)
    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    // Ricerca dei giocatori per username (pagina Amici): solo account attivi del ruolo indicato, senza
    // chi cerca. Containing aggiunge i % da solo e tratta % e _ scritti dall'utente come caratteri normali
    List<User> findTop20ByEnabledTrueAndRoleAndUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(
            UserRole role, String username, UUID excludedUserId);

    // Numeri della Dashboard admin (GET /admin/stats): solo i giocatori, anche gli account cancellati per i nuovi
    long countByRoleAndEnabledTrue(UserRole role);

    long countByRoleAndCreatedAtGreaterThanEqual(UserRole role, Instant from);

    long countByRoleAndEnabledTrueAndLastLoginAtGreaterThanEqual(UserRole role, Instant from);

    // I Dream Credits sui saldi dei giocatori
    @Query("SELECT COALESCE(SUM(u.credits), 0) FROM User u WHERE u.role = :role")
    long sumCreditsByRole(UserRole role);
}
