package it.somnia.dreamscape.api.pack.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackSlot;

public interface PackSlotRepository extends JpaRepository<PackSlot, UUID> {

    // Svuota gli slot prima di salvare quelli nuovi (backoffice); le rarità si cancellano con ON DELETE CASCADE.
    // Query diretta: con delete() e save() Hibernate inserirebbe prima di cancellare (uq_pack_slots_pack_order)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PackSlot slot WHERE slot.pack = :pack")
    void deleteAllSlotsOf(@Param("pack") Pack pack);
}
