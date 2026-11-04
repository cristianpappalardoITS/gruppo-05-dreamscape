package it.somnia.dreamscape.api.card.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un'espansione (set di carte): set base, espansioni future, set di prova
@Entity
@Table(name = "card_sets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSet {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Codice stabile: BASE, EXP1, TEST...
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
