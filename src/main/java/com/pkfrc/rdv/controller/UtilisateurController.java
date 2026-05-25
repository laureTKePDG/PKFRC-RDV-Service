package com.pkfrc.rdv.controller;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.domain.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs (clients et responsables)")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un utilisateur (client ou responsable)")
    public ResponseEntity<RdvDtos.UtilisateurResponse> creer(
            @Valid @RequestBody RdvDtos.UtilisateurRequest request) {
        var response = utilisateurService.creerUtilisateur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ref}")
    @Operation(summary = "Récupérer un utilisateur par sa référence")
    public RdvDtos.UtilisateurResponse findByRef(@PathVariable String ref) {
        return utilisateurService.findByRef(ref);
    }

    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs, filtrable par rôle")
    public List<RdvDtos.UtilisateurResponse> findAll(
            @RequestParam(required = false) UserRole role) {
        return role != null
                ? utilisateurService.findByRole(role)
                : utilisateurService.findAll();
    }

    @DeleteMapping("/{ref}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Désactiver un utilisateur (soft delete)")
    public void desactiver(@PathVariable String ref) {
        utilisateurService.desactiverUtilisateur(ref);
    }
}
