package com.pkfrc.rdv.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pkfrc.rdv.domain.model.RdvStatut;
import com.pkfrc.rdv.domain.model.UserRole;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTOs utilisant les Records Java 21 pour l'immutabilité et la concision.
 * Pattern matching et sealed types utilisés dans la couche service.
 */
public final class RdvDtos {

    private RdvDtos() {}

    // ── Utilisateur ─────────────────────────────────────────────────────────

    public record UtilisateurRequest(
            @NotBlank(message = "La référence est obligatoire")
            @Size(max = 50)
            String ref,

            @NotBlank(message = "L'email est obligatoire")
            @Email(message = "Format email invalide")
            @Size(max = 255)
            String email,

            @NotBlank(message = "Le téléphone est obligatoire")
            @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
            String telephone,

            @NotBlank(message = "Le nom est obligatoire")
            @Size(max = 100)
            String nom,

            @NotBlank(message = "Le prénom est obligatoire")
            @Size(max = 100)
            String prenom,

            @NotNull(message = "Le rôle est obligatoire")
            UserRole role
    ) {}

    public record UtilisateurResponse(
            String ref,
            String email,
            String telephone,
            String nom,
            String prenom,
            UserRole role,
            boolean actif,
            Instant createdAt
    ) {}

    // ── RendezVous ───────────────────────────────────────────────────────────

    public record RendezVousRequest(
            @NotBlank(message = "La référence client est obligatoire")
            String refClient,

            @NotBlank(message = "La référence RDV est obligatoire")
            @Size(max = 50)
            String refRDV,

            @NotBlank(message = "La référence service est obligatoire")
            String refService,

            @NotBlank(message = "La référence responsable est obligatoire")
            String refResponsable,

            @NotNull(message = "La date et heure du RDV sont obligatoires")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            java.time.LocalDateTime dateRDV,

            @NotBlank(message = "Le motif du RDV est obligatoire")
            @Size(min = 5, max = 500, message = "Le motif doit contenir entre 5 et 500 caractères")
            String motifRdv,

            // Deuxième participant optionnel (max 2 personnes physiques)
            String refClient2
    ) {}

    public record RendezVousResponse(
            String refRdv,
            String refService,
            String libelleService,
            UtilisateurResponse responsable,
            PlageResponse plage,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate dateRdv,
            String motifRdv,
            RdvStatut statut,
            List<UtilisateurResponse> participants,
            Instant createdAt
    ) {}

    // ── Plage Horaire ────────────────────────────────────────────────────────

    public record PlageResponse(
            Long id,
            @JsonFormat(pattern = "HH:mm")
            LocalTime heureDebut,
            @JsonFormat(pattern = "HH:mm")
            LocalTime heureFin,
            String libelle
    ) {}

    // ── Service ──────────────────────────────────────────────────────────────

    public record ServiceResponse(
            String ref,
            String libelle,
            boolean actif
    ) {}

    // ── Erreur standardisée ──────────────────────────────────────────────────

    public record ErrorResponse(
            String code,
            String message,
            String timestamp,
            List<FieldError> errors
    ) {
        public record FieldError(String field, String message) {}
    }

    // ── Pagination ───────────────────────────────────────────────────────────

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
