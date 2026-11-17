package it.somnia.dreamscape.api.pack.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.pack.model.PackSlotRarity;

public interface PackSlotRarityRepository extends JpaRepository<PackSlotRarity, UUID> {
}
