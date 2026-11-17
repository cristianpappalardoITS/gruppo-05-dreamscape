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

import it.somnia.dreamscape.api.admin.dto.response.ImageUploadResponse;
import it.somnia.dreamscape.api.admin.service.AdminImageService;
import it.somnia.dreamscape.api.admin.service.ImageType;
import lombok.RequiredArgsConstructor;

// Backoffice: caricamento delle illustrazioni e delle immagini dei pacchetti (solo ADMIN, SecurityConfig)
@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
public class AdminImageController {

    private final AdminImageService adminImageService;

    // Form multipart/form-data con file, code e type
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file,
            @RequestParam String code, @RequestParam ImageType type) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminImageService.upload(file.getBytes(), code, type));
    }
}
