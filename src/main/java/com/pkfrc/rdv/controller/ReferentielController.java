package com.pkfrc.rdv.controller;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.service.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/referentiel")
@Tag(name = "Référentiel", description = "Données de référence : services et plages horaires")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/services")
    @Operation(summary = "Lister tous les services disponibles")
    public List<RdvDtos.ServiceResponse> findServices() {
        return referentielService.findAllServices();
    }

    @GetMapping("/plages")
    @Operation(summary = "Lister toutes les plages horaires disponibles (08h-16h)")
    public List<RdvDtos.PlageResponse> findPlages() {
        return referentielService.findAllPlages();
    }
}
