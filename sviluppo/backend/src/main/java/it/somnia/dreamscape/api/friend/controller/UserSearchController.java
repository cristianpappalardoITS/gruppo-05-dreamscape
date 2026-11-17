package it.somnia.dreamscape.api.friend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// Ricerca dei Dreamweaver per username, nella pagina Amici: solo id e username
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final FriendService friendService;

    @GetMapping
    public List<UserSummaryResponse> searchUsers(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @NotBlank @Size(min = 2, max = 40) String search) {
        return friendService.searchUsers(CurrentUser.id(jwt), search);
    }
}
