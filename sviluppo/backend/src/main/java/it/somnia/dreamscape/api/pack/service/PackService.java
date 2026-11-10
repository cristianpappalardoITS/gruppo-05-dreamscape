package it.somnia.dreamscape.api.pack.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.pack.dto.response.PackResponse;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import lombok.RequiredArgsConstructor;

/**
 * Il negozio dei pacchetti: quali pacchetti sono in vendita e com'è fatto ognuno.
 */
@Service
@RequiredArgsConstructor
public class PackService {

    private final PackRepository packRepository;
    private final Clock clock;

    /**
     * Restituisce i pacchetti in vendita in questo momento ({@code GET /packs}), dal più economico.
     *
     * <p>Un pacchetto è in vendita se è di tipo SHOP, attivo e dentro il suo periodo di vendita.
     * Per ogni pacchetto ci sono le copertine attive e gli slot con le probabilità delle rarità.
     *
     * @return i pacchetti in vendita, lista vuota se non ce n'è nessuno
     */
    @Transactional(readOnly = true)
    public List<PackResponse> listPacksOnSale() {
        return packRepository.findOnSale(PackType.SHOP, clock.instant()).stream()
                .map(PackResponse::from)
                .toList();
    }
}
