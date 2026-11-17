package it.somnia.dreamscape.api.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// Gli indirizzi delle copie WebP appena create: si mettono così come sono nella carta o nel pacchetto.
// thumbUrl non c'è per le copertine, che hanno una sola copia.
public record ImageUploadResponse(String url, @JsonInclude(JsonInclude.Include.NON_NULL) String thumbUrl) {
}
