package it.somnia.dreamscape.api.user;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.user.repository.PasswordResetTokenRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.PasswordResetNotifier;

// Email del recupero password: parte dopo il commit e in un altro thread. Senza @Transactional,
// perché le transazioni dei test non arrivano mai al commit; questo contesto ha il suo database.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetMailTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private PasswordResetNotifier passwordResetNotifier;

    @Test
    void passwordResetRequest_knownEmail_sendsTheLinkAfterCommitInAnotherThread() throws Exception {
        // Given: al momento dell'invio il token deve essere già nel database
        Thread requestThread = Thread.currentThread();
        AtomicReference<Thread> sendingThread = new AtomicReference<>();
        AtomicBoolean tokenSaved = new AtomicBoolean();
        // Mockito registra la chiamata prima di eseguire la risposta: si aspetta la fine della risposta
        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            sendingThread.set(Thread.currentThread());
            tokenSaved.set(userRepository.findByUsernameIgnoreCase("anna_test")
                    .map(user -> passwordResetTokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(),
                            Instant.EPOCH))
                    .orElse(false));
            sent.countDown();
            return null;
        }).when(passwordResetNotifier).sendResetLink(anyString(), anyString(), anyString());

        // When
        mockMvc.perform(post("/api/v1/auth/password-reset-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"anna_test@example.test\" }"))
                .andExpect(status().isAccepted());

        // Then
        assertTrue(sent.await(5, TimeUnit.SECONDS), "Email non mandata entro 5 secondi");
        verify(passwordResetNotifier).sendResetLink(eq("anna_test@example.test"), eq("anna_test"), anyString());
        assertNotEquals(requestThread, sendingThread.get());
        assertTrue(tokenSaved.get());
    }
}
