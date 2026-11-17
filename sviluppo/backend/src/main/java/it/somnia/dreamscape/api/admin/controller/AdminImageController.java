package it.somnia.dreamscape.api.admin.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.response.ImageUploadResponse;
import it.somnia.dreamscape.api.admin.service.AdminImageService;
import it.somnia.dreamscape.api.admin.service.ImageType;
import lombok.RequiredArgsConstructor;

// Backoffice: caricamento delle illustrazioni e delle immagini dei pacchetti (solo ADMIN, SecurityConfig)
@Tag(name = "Backoffice: immagini", description = "Solo role ADMIN. Caricamento delle illustrazioni delle carte e "
        + "delle copertine dei pacchetti: il backend conserva l'originale e crea le copie WebP.")
@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
public class AdminImageController {

    private final AdminImageService adminImageService;

    // Form multipart/form-data con file, code e type
    @Operation(summary = "Caricamento di un'immagine", description = "Form multipart/form-data. ILLUSTRATION e "
            + "FULL_ART creano una copia grande (1024 px) e una miniatura (400 px), COVER una copia sola. Ogni "
            + "caricamento ha una versione nuova nel nome (-v1, -v2...). Non modifica la carta: gli indirizzi si "
            + "salvano poi nella carta o nel pacchetto.")
    @ApiResponse(responseCode = "201", description = "Indirizzi delle copie WebP (thumbUrl non c'è per COVER)")
    @ApiResponse(responseCode = "400", description = "INVALID_IMAGE (non è un PNG o un JPEG), VALIDATION_ERROR "
            + "(codice non valido) o MALFORMED_REQUEST (manca il file o il tipo)")
    @ApiResponse(responseCode = "413", description = "FILE_TOO_LARGE: oltre 15 MB")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @Parameter(description = "Il file PNG o JPEG, al massimo 15 MB") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Codice della carta (es. BASE-013, anche BASE-013-FA per FULL_ART) o nome del "
                    + "pacchetto (es. dream-pack)")
            @RequestParam String code,
            @Parameter(description = "Che immagine è") @RequestParam ImageType type) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminImageService.upload(file.getBytes(), code, type));
    }
}
