package com.pkfrc.rdv.domain.model;

/**
 * Rôles possibles d'un utilisateur dans le système.
 * Utilisation de Java 21 sealed interface + pattern matching.
 */
public enum UserRole {
    CLIENT,
    RESPONSABLE;

    public boolean isResponsable() {
        return this == RESPONSABLE;
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
