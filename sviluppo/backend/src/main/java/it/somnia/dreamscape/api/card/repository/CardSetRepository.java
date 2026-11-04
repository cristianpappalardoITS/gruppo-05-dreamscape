package it.somnia.dreamscape.api.card.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.CardSet;

public interface CardSetRepository extends JpaRepository<CardSet, UUID> {

    Optional<CardSet> findByCode(String code);
}
