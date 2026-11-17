package it.somnia.dreamscape.api.common.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

// Lista paginata: { items, page, size, totalItems, totalPages } (guida per il frontend, capitolo 2)
public record PageResponse<T>(
        @Schema(description = "Gli elementi della pagina") List<T> items,
        @Schema(description = "Numero della pagina, da 0", example = "0") int page,
        @Schema(description = "Elementi per pagina", example = "24") int size,
        @Schema(description = "Elementi in tutto", example = "99") long totalItems,
        @Schema(description = "Pagine in tutto", example = "5") int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
