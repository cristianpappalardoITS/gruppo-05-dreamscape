package it.somnia.dreamscape.api.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.repository.DeckRepository;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

// Riepilogo della Dashboard (GET /me/summary). Punteggio, mazzo classificato e aperture si preparano
// qui con i repository, perché partite e mazzi non hanno ancora le loro API.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private PackOpeningRepository packOpeningRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getSummary_newPlayer_returnsStartingValues() throws Exception {
        // Given: anna_test è come dopo la registrazione (1000 punti, nessun pacchetto aperto);
        // con l'accesso ha 550 DC, 500 della registrazione e 50 della ricompensa giornaliera

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("anna_test"))
                .andExpect(jsonPath("$.credits").value(550))
                .andExpect(jsonPath("$.rating").value(1000))
                .andExpect(jsonPath("$.league.code").value("DORMANT"))
                .andExpect(jsonPath("$.league.name").value("Dormiente"))
                .andExpect(jsonPath("$.league.nextLeagueName").value("Sognatore"))
                .andExpect(jsonPath("$.league.nextLeagueMinRating").value(1100))
                .andExpect(jsonPath("$.hasRankedDeck").value(false))
                .andExpect(jsonPath("$.missions.length()").value(2))
                .andExpect(jsonPath("$.missions[0].code").value("DAILY_PLAY"))
                .andExpect(jsonPath("$.missions[0].progress").value(0))
                .andExpect(jsonPath("$.missions[0].isCompleted").value(false))
                .andExpect(jsonPath("$.missions[1].code").value("WEEKLY_WIN"))
                .andExpect(jsonPath("$.missions[1].progress").value(0))
                .andExpect(jsonPath("$.pendingFriendRequests").value(0))
                .andExpect(jsonPath("$.pendingTrades").value(0))
                .andExpect(jsonPath("$.pendingChallenges").value(0))
                .andExpect(jsonPath("$.latestCards.length()").value(0));
    }

    @Test
    void getSummary_ratingOnThreshold_returnsThatLeague() throws Exception {
        // Given: 1300 è proprio la soglia di Lucido
        setRating("anna_test", 1300);

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.rating").value(1300))
                .andExpect(jsonPath("$.league.code").value("LUCID"))
                .andExpect(jsonPath("$.league.nextLeagueName").value("Dreamweaver"))
                .andExpect(jsonPath("$.league.nextLeagueMinRating").value(1500));
    }

    @Test
    void getSummary_highestLeague_returnsNoNextLeague() throws Exception {
        // Given
        setRating("anna_test", 1650);

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.league.code").value("DREAMWEAVER"))
                .andExpect(jsonPath("$.league.nextLeagueName").doesNotExist())
                .andExpect(jsonPath("$.league.nextLeagueMinRating").doesNotExist());
    }

    @Test
    void getSummary_rankedDeckChosen_returnsHasRankedDeck() throws Exception {
        // Given
        User anna = userRepository.findByUsernameIgnoreCase("anna_test").orElseThrow();
        anna.setRankedDeck(deckRepository.save(new Deck(anna, "Mazzo classificato")));
        entityManager.flush();
        entityManager.clear();

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.hasRankedDeck").value(true));
    }

    @Test
    void getSummary_severalOpenings_returnsCardsOfLatestInDrawOrder() throws Exception {
        // Given: due aperture; nella risposta ci sono solo le carte della seconda
        saveOpening("anna_test", "TEST-001", "TEST-002");
        saveOpening("anna_test", "TEST-008", "TEST-003", "TEST-013-FA");

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.latestCards.length()").value(3))
                .andExpect(jsonPath("$.latestCards[0].card.code").value("TEST-008"))
                .andExpect(jsonPath("$.latestCards[1].card.code").value("TEST-003"))
                .andExpect(jsonPath("$.latestCards[2].card.code").value("TEST-013-FA"))
                .andExpect(jsonPath("$.latestCards[2].card.effect.code").value("REST"));
    }

    @Test
    void getSummary_otherUsersOpening_isNotShown() throws Exception {
        // Given
        saveOpening("bruno_test", "TEST-001");

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.latestCards.length()").value(0));
    }

    @Test
    void getSummary_deletedAccount_returns401() throws Exception {
        // Given: il token di accesso è ancora valido, ma l'account non è più attivo
        String token = bearer("anna_test");
        User anna = userRepository.findByUsernameIgnoreCase("anna_test").orElseThrow();
        anna.setEnabled(false);
        entityManager.flush();
        entityManager.clear();

        // When / Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void getSummary_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary")).andExpect(status().isUnauthorized());
    }

    private void setRating(String username, int rating) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        user.setRating(rating);
        entityManager.flush();
        entityManager.clear();
    }

    // Salva un'apertura del Dream Pack con le carte indicate, poi svuota la cache di Hibernate
    // perché l'API rilegga tutto dal database
    private void saveOpening(String username, String... cardCodes) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        Pack pack = packRepository.findAll().stream()
                .filter(candidate -> candidate.getName().equals("Dream Pack"))
                .findFirst().orElseThrow();
        PackOpening opening = new PackOpening(user, pack, null, PackOpeningSource.PURCHASE, 500);
        List.of(cardCodes).forEach(code -> opening.addCard(cardRepository.findByCode(code).orElseThrow()));
        packOpeningRepository.save(opening);
        entityManager.flush();
        entityManager.clear();
    }

    private String bearer(String username) throws Exception {
        String body = "{ \"login\": \"" + username + "\", \"password\": \"" + TEST_PASSWORD + "\" }";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }
}
