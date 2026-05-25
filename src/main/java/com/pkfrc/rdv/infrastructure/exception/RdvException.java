package com.pkfrc.rdv.infrastructure.exception;

/**
 * Hiérarchie d'exceptions métier utilisant les sealed classes Java 21.
 */
public sealed class RdvException extends RuntimeException
        permits RdvException.ResourceNotFoundException,
                RdvException.BusinessRuleException,
                RdvException.ConflictException {

    protected RdvException(String message) {
        super(message);
    }

    protected RdvException(String message, Throwable cause) {
        super(message, cause);
    }

    // ── 404 ─────────────────────────────────────────────────────────────────

    public static final class ResourceNotFoundException extends RdvException {
        private final String resourceType;
        private final String identifier;

        public ResourceNotFoundException(String resourceType, String identifier) {
            super("%s introuvable : [%s]".formatted(resourceType, identifier));
            this.resourceType = resourceType;
            this.identifier = identifier;
        }

        public String getResourceType() { return resourceType; }
        public String getIdentifier()   { return identifier; }
    }

    // ── 422 / Règles métier ──────────────────────────────────────────────────

    public static final class BusinessRuleException extends RdvException {
        private final String ruleCode;

        public BusinessRuleException(String ruleCode, String message) {
            super(message);
            this.ruleCode = ruleCode;
        }

        public String getRuleCode() { return ruleCode; }
    }

    // ── 409 ─────────────────────────────────────────────────────────────────

    public static final class ConflictException extends RdvException {
        private final String conflictField;

        public ConflictException(String conflictField, String message) {
            super(message);
            this.conflictField = conflictField;
        }

        public String getConflictField() { return conflictField; }
    }

    // Factory methods pour lisibilité

    public static ResourceNotFoundException utilisateurNotFound(String ref) {
        return new ResourceNotFoundException("Utilisateur", ref);
    }

    public static ResourceNotFoundException serviceNotFound(String ref) {
        return new ResourceNotFoundException("Service", ref);
    }

    public static ResourceNotFoundException rdvNotFound(String ref) {
        return new ResourceNotFoundException("RendezVous", ref);
    }

    public static ConflictException refRdvDuplicate(String ref) {
        return new ConflictException("refRdv",
                "Un RDV avec la référence [%s] existe déjà".formatted(ref));
    }

    public static ConflictException responsableIndisponible(String refResponsable, String plage, String date) {
        return new ConflictException("responsable",
                "Le responsable [%s] a déjà un RDV sur la plage [%s] du [%s]"
                        .formatted(refResponsable, plage, date));
    }

    public static BusinessRuleException rdvTropProcheException(int joursAvant) {
        return new BusinessRuleException("RDV_DELAI_MINIMUM",
                "Un RDV doit être pris au moins %d jours avant sa date".formatted(joursAvant));
    }

    public static BusinessRuleException maxParticipantsAtteint() {
        return new BusinessRuleException("MAX_PARTICIPANTS",
                "Un RDV ne peut pas avoir plus de 2 personnes physiques");
    }

    public static BusinessRuleException roleMismatch(String ref, String expectedRole) {
        return new BusinessRuleException("ROLE_INVALIDE",
                "L'utilisateur [%s] n'a pas le rôle requis : %s".formatted(ref, expectedRole));
    }
}
