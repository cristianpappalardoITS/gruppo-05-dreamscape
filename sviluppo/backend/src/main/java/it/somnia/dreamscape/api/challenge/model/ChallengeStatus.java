package it.somnia.dreamscape.api.challenge.model;

// Gli stati di una sfida (chk_challenges_status). Una sfida non scade: resta PENDING finché qualcuno decide
public enum ChallengeStatus {
    PENDING,
    REJECTED,
    CANCELLED,
    // Accettata e giocata: la partita ha challenge_id uguale all'id della sfida
    COMPLETED
}
