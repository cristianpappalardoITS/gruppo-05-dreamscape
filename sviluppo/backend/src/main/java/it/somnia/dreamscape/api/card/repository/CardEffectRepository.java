package it.somnia.dreamscape.api.card.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;

// Gli effetti stanno solo sulle carte normali: per una versione si cerca con la sua carta base
public interface CardEffectRepository extends JpaRepository<CardEffect, UUID> {

    // Al massimo un effetto per carta (regolamento attuale, controllato dal service)
    Optional<CardEffect> findByCard(Card card);

    // Per le liste di carte, con una sola query
    List<CardEffect> findByCardIn(Collection<Card> cards);
}
