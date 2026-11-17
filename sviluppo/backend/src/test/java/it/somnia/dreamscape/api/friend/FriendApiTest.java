package it.somnia.dreamscape.api.friend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.friend.service.FriendshipRemovedEvent;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Lista amici (#93) con gli utenti di prova di V902: anna_test e bruno_test sono amici,
// carla_test ha chiesto l'amicizia a bruno_test
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class FriendApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents events;

    // ------------------------------------------------------------------ amici

    @Test
    void listFriends_anna_returnsBrunoWithTheDate() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/friends").header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.id").value(userId("bruno_test").toString()))
                .andExpect(jsonPath("$[0].user.username").value("bruno_test"))
                .andExpect(jsonPath("$[0].since").isNotEmpty())
                .andExpect(jsonPath("$[0].user.email").doesNotExist());
    }

    @Test
    void listFriends_withoutLogin_returnsUnauthorized() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removeFriend_friend_removesItAndPublishesTheEvent() throws Exception {
        // Given
        String anna = bearer("anna_test");
        UUID annaId = userId("anna_test");
        UUID brunoId = userId("bruno_test");

        // When
        mockMvc.perform(delete("/api/v1/friends/" + brunoId).header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNoContent());

        // Then: niente più amici, e scambi e sfide in attesa possono annullarsi ascoltando l'evento
        mockMvc.perform(get("/api/v1/friends").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(0));
        assertEquals(List.of(new FriendshipRemovedEvent(annaId, brunoId)),
                events.stream(FriendshipRemovedEvent.class).toList());
    }

    @Test
    void removeFriend_notAFriend_returnsFriendNotFound() throws Exception {
        // When / Then: carla_test ha solo chiesto l'amicizia a bruno_test
        mockMvc.perform(delete("/api/v1/friends/" + userId("carla_test"))
                        .header(HttpHeaders.AUTHORIZATION, bearer("bruno_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
        assertEquals(0, events.stream(FriendshipRemovedEvent.class).count());
    }

    // ------------------------------------------------------------------ ricerca

    @Test
    void searchUsers_partOfTheUsername_returnsOthersByUsername() throws Exception {
        // When / Then: chi cerca non compare
        mockMvc.perform(get("/api/v1/users").param("search", "_TEST")
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("bruno_test"))
                .andExpect(jsonPath("$[1].username").value("carla_test"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void searchUsers_percentSigns_areNormalCharacters() throws Exception {
        // When / Then: "%%" non diventa "tutti i giocatori"
        mockMvc.perform(get("/api/v1/users").param("search", "%%")
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchUsers_oneCharacter_returnsValidationError() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/users").param("search", "a")
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ------------------------------------------------------------------ richieste

    @Test
    void sendRequest_newPlayer_createsTheRequestForBoth() throws Exception {
        // Given
        String anna = bearer("anna_test");
        String carla = bearer("carla_test");

        // When
        sendRequest(anna, "CARLA_TEST")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("carla_test"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // Then: anna la vede tra le mandate, carla tra le ricevute e nel contatore della Dashboard
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "sent")
                        .header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.username").value("carla_test"));
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "received")
                        .header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.username").value("anna_test"));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(jsonPath("$.pendingFriendRequests").value(1));
    }

    @Test
    void sendRequest_otherHadAlreadyAsked_acceptsItAtOnce() throws Exception {
        // Given: carla_test aveva già chiesto l'amicizia a bruno_test
        String bruno = bearer("bruno_test");

        // When / Then: 200 con l'amico invece di 201 con una richiesta nuova
        sendRequest(bruno, "carla_test")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("carla_test"))
                .andExpect(jsonPath("$.since").isNotEmpty());
        mockMvc.perform(get("/api/v1/friends").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "received")
                        .header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sendRequest_yourself_returnsCannotFriendYourself() throws Exception {
        // When / Then
        sendRequest(bearer("anna_test"), "anna_test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_FRIEND_YOURSELF"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void sendRequest_unknownUsername_returnsUserNotFound() throws Exception {
        // When / Then
        sendRequest(bearer("anna_test"), "nessuno_test")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void sendRequest_alreadyFriends_returnsAlreadyFriends() throws Exception {
        // When / Then
        sendRequest(bearer("anna_test"), "bruno_test")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_FRIENDS"));
    }

    @Test
    void sendRequest_twice_returnsAlreadySent() throws Exception {
        // When / Then: carla_test aveva già chiesto l'amicizia a bruno_test
        sendRequest(bearer("carla_test"), "bruno_test")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FRIEND_REQUEST_ALREADY_SENT"));
    }

    @Test
    void sendRequest_blankUsername_returnsValidationError() throws Exception {
        // When / Then
        sendRequest(bearer("anna_test"), " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listRequests_unknownDirection_returnsValidationError() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "all")
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void acceptRequest_received_makesThemFriends() throws Exception {
        // Given
        String bruno = bearer("bruno_test");
        String requestId = receivedRequestId(bruno);

        // When / Then
        mockMvc.perform(post("/api/v1/friend-requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("carla_test"))
                .andExpect(jsonPath("$.since").isNotEmpty());
        mockMvc.perform(get("/api/v1/friends").header(HttpHeaders.AUTHORIZATION, bearer("carla_test")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.username").value("bruno_test"));
    }

    @Test
    void acceptRequest_sentByYou_returnsRequestNotFound() throws Exception {
        // Given: la richiesta è di carla_test; solo bruno_test la può accettare
        String requestId = receivedRequestId(bearer("bruno_test"));

        // When / Then
        mockMvc.perform(post("/api/v1/friend-requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, bearer("carla_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIEND_REQUEST_NOT_FOUND"));
    }

    @Test
    void deleteRequest_received_rejectsIt() throws Exception {
        // Given
        String bruno = bearer("bruno_test");
        String requestId = receivedRequestId(bruno);

        // When
        mockMvc.perform(delete("/api/v1/friend-requests/" + requestId).header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isNoContent());

        // Then: la richiesta non c'è più per nessuno dei due
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "sent")
                        .header(HttpHeaders.AUTHORIZATION, bearer("carla_test")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteRequest_sentByYou_cancelsIt() throws Exception {
        // Given
        String requestId = receivedRequestId(bearer("bruno_test"));

        // When / Then
        mockMvc.perform(delete("/api/v1/friend-requests/" + requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("carla_test")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRequest_otherPlayers_returnsRequestNotFound() throws Exception {
        // Given: la richiesta è tra carla_test e bruno_test
        String requestId = receivedRequestId(bearer("bruno_test"));

        // When / Then
        mockMvc.perform(delete("/api/v1/friend-requests/" + requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("anna_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIEND_REQUEST_NOT_FOUND"));
    }

    // ------------------------------------------------------------------ cancellazione dell'account

    @Test
    void deleteAccount_removesFriendsAndRequests() throws Exception {
        // Given: bruno_test è amico di anna_test e ha una richiesta di carla_test
        String anna = bearer("anna_test");
        String carla = bearer("carla_test");
        UUID brunoId = userId("bruno_test");

        // When
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("bruno_test"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"" + TEST_PASSWORD + "\" }"))
                .andExpect(status().isNoContent());

        // Then: l'amicizia finisce con il suo evento, la richiesta di carla_test sparisce
        mockMvc.perform(get("/api/v1/friends").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/friend-requests").param("direction", "sent")
                        .header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(jsonPath("$.length()").value(0));
        assertEquals(List.of(new FriendshipRemovedEvent(brunoId, userId("anna_test"))),
                events.stream(FriendshipRemovedEvent.class).toList());
        // L'account anonimo non si trova più con la ricerca
        mockMvc.perform(get("/api/v1/users").param("search", "deleted").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ResultActions sendRequest(String bearer, String username) throws Exception {
        return mockMvc.perform(post("/api/v1/friend-requests").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"username\": \"" + username + "\" }"));
    }

    // L'id della prima richiesta ricevuta dall'utente
    private String receivedRequestId(String bearer) throws Exception {
        String body = mockMvc.perform(get("/api/v1/friend-requests").param("direction", "received")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode requests = objectMapper.readTree(body);
        assertTrue(requests.size() > 0, "Nessuna richiesta ricevuta");
        return requests.get(0).get("id").asString();
    }

    private UUID userId(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow().getId();
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
