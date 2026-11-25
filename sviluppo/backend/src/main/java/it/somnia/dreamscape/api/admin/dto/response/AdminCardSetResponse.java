package it.somnia.dreamscape.api.admin.dto.response;

import it.somnia.dreamscape.api.card.model.CardSet;

/**
 * Un'espansione delle carte nel backoffice ({@code GET /admin/card-sets}).
 *
 * @param code     codice, es. BASE
 * @param name     nome
 * @param isActive se è attiva
 */
public record AdminCardSetResponse(String code, String name, boolean isActive) {

    public static AdminCardSetResponse from(CardSet cardSet) {
        return new AdminCardSetResponse(cardSet.getCode(), cardSet.getName(), cardSet.isActive());
    }
}
