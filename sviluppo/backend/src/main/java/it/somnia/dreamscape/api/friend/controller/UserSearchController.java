package it.somnia.dreamscape.api.friend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// Ricerca dei Dreamweaver per username, nella pagina Amici: solo id e username
@Tag(name = "Amici")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final FriendService friendService;

    @Operation(summary = "Cerca un Dreamweaver", description = "Per username, senza distinguere maiuscole e minuscole: "
            + "al massimo 20, in ordine di username. Non ci sono chi cerca, gli admin e gli account cancellati.")
    @ApiResponse(responseCode = "200", description = "Id e username dei giocatori trovati")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: search sotto i 2 caratteri")
    @GetMapping
    public List<UserSummaryResponse> searchUsers(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Parte dello username, da 2 caratteri")
            @RequestParam @NotBlank @Size(min = 2, max = 40) String search) {
        return friendService.searchUsers(CurrentUser.id(jwt), search);
    }
}
