package com.pkfrc.rdv.controller;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.service.RendezVousService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rendez-vous")
@Tag(name = "Rendez-vous", description = "Gestion des rendez-vous dans les services")
public class RendezVousController {

    private final RendezVousService rdvService;

    public RendezVousController(RendezVousService rdvService) {
        this.rdvService = rdvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prendre un rendez-vous",
               description = "Crée un RDV. Date minimale : J+2. Max 2 participants.")
    public ResponseEntity<RdvDtos.RendezVousResponse> prendreRdv(
            @Valid @RequestBody RdvDtos.RendezVousRequest request) {
        var response = rdvService.prendreRendezVous(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{refRdv}")
    @Operation(summary = "Consulter un rendez-vous par sa référence")
    public RdvDtos.RendezVousResponse findByRef(@PathVariable String refRdv) {
        return rdvService.findByRef(refRdv);
    }

    @GetMapping
    @Operation(summary = "Lister les RDV par date")
    public List<RdvDtos.RendezVousResponse> findByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return rdvService.findByDate(date);
    }

    @GetMapping("/service/{refService}")
    @Operation(summary = "Lister les RDV d'un service sur une période")
    public List<RdvDtos.RendezVousResponse> findByService(
            @PathVariable String refService,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return rdvService.findByServiceAndPeriode(refService, debut, fin);
    }

    @PatchMapping("/{refRdv}/annuler")
    @Operation(summary = "Annuler un rendez-vous")
    public RdvDtos.RendezVousResponse annuler(@PathVariable String refRdv) {
        return rdvService.annulerRendezVous(refRdv);
    }

    @PatchMapping("/{refRdv}/terminer")
    @Operation(summary = "Clôturer un rendez-vous")
    public RdvDtos.RendezVousResponse terminer(@PathVariable String refRdv) {
        return rdvService.terminerRendezVous(refRdv);
    }
}
