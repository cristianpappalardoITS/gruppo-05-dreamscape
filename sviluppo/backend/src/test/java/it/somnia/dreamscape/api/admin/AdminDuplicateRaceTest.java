package it.somnia.dreamscape.api.admin;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;

// Due richieste con lo stesso codice o nome nello stesso momento: il controllo del service non vede l'altra
// (qui lo si simula facendogli rispondere "non c'è"), il vincolo UNIQUE del database sì. L'errore deve essere
// lo stesso del controllo, non il CONFLICT generico
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminDuplicateRaceTest extends AdminApiTestSupport {

    @MockitoSpyBean
    private CardRepository cardRepository;

    @MockitoSpyBean
    private PackRepository packRepository;

    @Test
    void createCard_codeSavedByAnotherRequestMeanwhile_returnsCardCodeTaken() throws Exception {
        // Given
        doReturn(Optional.empty()).when(cardRepository).findByCode("TEST-001");

        // When / Then
        mockMvc.perform(post("/api/v1/admin/cards").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "TEST-001", "name": "Carta Doppia", "attack": 3, "defense": 3,
                                  "factionCode": "DREAMS", "rarityCode": "COMMON", "cardSetCode": "TEST" }"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARD_CODE_TAKEN"));
    }

    @Test
    void createPack_nameSavedByAnotherRequestMeanwhile_returnsPackNameTaken() throws Exception {
        // Given
        doReturn(Optional.empty()).when(packRepository).findByName("Dream Pack");

        // When / Then
        mockMvc.perform(post("/api/v1/admin/packs").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "name": "Dream Pack", "packType": "SHOP", "priceCredits": 100, "fullArtChance": 0,
                                  "slots": [ { "cardCount": 1, "rarities": [ { "rarityCode": "COMMON", "weight": 100 } ] } ] }"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_NAME_TAKEN"));
    }
}
