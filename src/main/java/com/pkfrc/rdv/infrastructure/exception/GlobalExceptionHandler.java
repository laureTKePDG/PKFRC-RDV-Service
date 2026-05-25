package com.pkfrc.rdv.infrastructure.exception;

import com.pkfrc.rdv.application.dto.RdvDtos;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Gestionnaire global des erreurs API.
 * Retourne des réponses standardisées RFC 7807 (Problem Details).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(RdvException.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RdvDtos.ErrorResponse handleNotFound(RdvException.ResourceNotFoundException ex) {
        log.warn("Ressource non trouvée: {}", ex.getMessage());
        return buildError("NOT_FOUND", ex.getMessage());
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────

    @ExceptionHandler(RdvException.ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public RdvDtos.ErrorResponse handleConflict(RdvException.ConflictException ex) {
        log.warn("Conflit détecté: {}", ex.getMessage());
        return buildError("CONFLICT", ex.getMessage());
    }

    // ── 422 Unprocessable Entity ─────────────────────────────────────────────

    @ExceptionHandler(RdvException.BusinessRuleException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public RdvDtos.ErrorResponse handleBusinessRule(RdvException.BusinessRuleException ex) {
        log.warn("Règle métier violée [{}]: {}", ex.getRuleCode(), ex.getMessage());
        return buildError(ex.getRuleCode(), ex.getMessage());
    }

    // ── 400 Bad Request - Validation Bean Validation 3.0 ────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RdvDtos.ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<RdvDtos.ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new RdvDtos.ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.warn("Erreurs de validation: {}", fieldErrors);
        return new RdvDtos.ErrorResponse(
                "VALIDATION_ERROR",
                "Les données fournies sont invalides",
                Instant.now().toString(),
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RdvDtos.ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        List<RdvDtos.ErrorResponse.FieldError> errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> new RdvDtos.ErrorResponse.FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()
                ))
                .toList();

        return new RdvDtos.ErrorResponse("VALIDATION_ERROR", "Contrainte violée",
                Instant.now().toString(), errors);
    }

    // ── 409 - Locking optimiste (concurrence) ────────────────────────────────

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public RdvDtos.ErrorResponse handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflit de version optimiste détecté: {}", ex.getMessage());
        return buildError("OPTIMISTIC_LOCK_CONFLICT",
                "Le RDV a été modifié par une autre requête simultanée. Veuillez réessayer.");
    }

    // ── 409 - Contrainte d'intégrité BD ─────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public RdvDtos.ErrorResponse handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Violation d'intégrité: {}", ex.getMessage());
        String message = extractConstraintMessage(ex);
        return buildError("DATA_INTEGRITY_VIOLATION", message);
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RdvDtos.ErrorResponse handleGeneric(Exception ex) {
        log.error("Erreur inattendue", ex);
        return buildError("INTERNAL_ERROR", "Une erreur interne est survenue");
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private RdvDtos.ErrorResponse buildError(String code, String message) {
        return new RdvDtos.ErrorResponse(code, message, Instant.now().toString(), List.of());
    }

    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("uq_responsable_plage_date")) {
            return "Ce responsable a déjà un RDV sur cette plage horaire à cette date";
        }
        if (msg != null && msg.contains("utilisateurs_ref_key")) {
            return "Un utilisateur avec cette référence existe déjà";
        }
        if (msg != null && msg.contains("utilisateurs_email_key")) {
            return "Un utilisateur avec cet email existe déjà";
        }
        if (msg != null && msg.contains("rendez_vous_ref_rdv_key")) {
            return "Un RDV avec cette référence existe déjà";
        }
        return "Violation de contrainte d'intégrité";
    }
}
