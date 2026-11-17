package it.somnia.dreamscape.api.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /friend-requests: { "username": "lucida" }
public record SendFriendRequestRequest(
        @NotBlank(message = "Scrivi lo username.")
        @Size(max = 40, message = "Lo username è troppo lungo.")
        String username) {
}
