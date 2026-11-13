package it.somnia.dreamscape.api.pack.dto.request;

import jakarta.validation.constraints.NotBlank;

// Scelta della fazione nella pagina Benvenuto: { "starterFaction": "DREAMS" }
public record WelcomeRequest(@NotBlank(message = "Scegli una fazione.") String starterFaction) {
}
