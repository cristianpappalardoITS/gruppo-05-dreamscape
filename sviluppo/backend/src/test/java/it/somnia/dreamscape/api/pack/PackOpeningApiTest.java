package it.somnia.dreamscape.api.pack;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

// Lettura di un'apertura (GET /pack-openings/{openingId}). Finché non c'è l'acquisto le aperture
// si preparano qui, salvandole con il repository.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackOpeningApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private PackOpeningRepository packOpeningRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getOpening_ownOpening_returnsCardsInDrawOrder() throws Exception {
        // Given
        UUID openingId = saveOpening("anna_test", "TEST-003", "TEST-001", "TEST-002", "TEST-008", "TEST-013-FA");

        // When / Then
        mockMvc.perform(get("/api/v1/pack-openings/" + openingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingId").value(openingId.toString()))
                .andExpect(jsonPath("$.packName").value("Dream Pack"))
                .andExpect(jsonPath("$.openedAt").exists())
                .andExpect(jsonPath("$.cards.length()").value(5))
                .andExpect(jsonPath("$.cards[0].card.code").value("TEST-003"))
                .andExpect(jsonPath("$.cards[1].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.cards[2].card.code").value("TEST-002"))
                .andExpect(jsonPath("$.cards[3].card.code").value("TEST-008"))
                .andExpect(jsonPath("$.cards[4].card.code").value("TEST-013-FA"));
    }

    @Test
    void getOpening_fullArt_returnsNameAndEffectOfBaseCard() throws Exception {
        // Given: TEST-013 ha Riposo 3
        UUID openingId = saveOpening("anna_test", "TEST-013-FA");

        // When / Then
        mockMvc.perform(get("/api/v1/pack-openings/" + openingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.cards[0].card.variantType").value("FULL_ART"))
                .andExpect(jsonPath("$.cards[0].card.baseCardId").exists())
                .andExpect(jsonPath("$.cards[0].card.name").value("Sognatrice Lucida (prova)"))
                .andExpect(jsonPath("$.cards[0].card.effect.code").value("REST"))
                .andExpect(jsonPath("$.cards[0].card.effect.value").value(3));
    }

    @Test
    void getOpening_cardWithoutEffect_returnsNullEffect() throws Exception {
        // Given: TEST-001 non ha effetti
        UUID openingId = saveOpening("anna_test", "TEST-001");

        // When / Then
        mockMvc.perform(get("/api/v1/pack-openings/" + openingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(jsonPath("$.cards[0].card.effect").doesNotExist());
    }

    @Test
    void getOpening_otherUsersOpening_returns404() throws Exception {
        // Given
        UUID openingId = saveOpening("bruno_test", "TEST-001");

        // When / Then
        mockMvc.perform(get("/api/v1/pack-openings/" + openingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACK_OPENING_NOT_FOUND"));
    }

    @Test
    void getOpening_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/pack-openings/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACK_OPENING_NOT_FOUND"));
    }

    @Test
    void getOpening_notAnId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/pack-openings/non-un-id").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getOpening_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/pack-openings/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    // Salva un'apertura del Dream Pack con le carte indicate, poi svuota la cache di Hibernate
    // perché l'API rilegga tutto dal database
    private UUID saveOpening(String username, String... cardCodes) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        Pack pack = packRepository.findAll().stream()
                .filter(candidate -> candidate.getName().equals("Dream Pack"))
                .findFirst().orElseThrow();
        PackOpening opening = new PackOpening(user, pack, null, PackOpeningSource.PURCHASE, 500);
        List.of(cardCodes).forEach(code -> opening.addCard(cardRepository.findByCode(code).orElseThrow()));
        UUID openingId = packOpeningRepository.save(opening).getId();
        entityManager.flush();
        entityManager.clear();
        return openingId;
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
