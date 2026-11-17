package it.somnia.dreamscape.api.trade.dto.response;

import java.util.UUID;

// Una carta che esce con lo scambio ed è l'ultima copia della collezione: dopo non ne resta nessuna
public record LastCopyWarningResponse(UUID cardId, String cardName) {
}
