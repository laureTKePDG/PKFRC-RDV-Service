package com.pkfrc.rdv.domain.service;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.infrastructure.persistence.repository.PlageHoraireJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.ServiceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final ServiceJpaRepository serviceRepo;
    private final PlageHoraireJpaRepository plageRepo;

    public ReferentielService(ServiceJpaRepository serviceRepo,
                               PlageHoraireJpaRepository plageRepo) {
        this.serviceRepo = serviceRepo;
        this.plageRepo = plageRepo;
    }

    public List<RdvDtos.ServiceResponse> findAllServices() {
        return serviceRepo.findByActifTrue()
                .stream()
                .map(s -> new RdvDtos.ServiceResponse(s.getRef(), s.getLibelle(), s.isActif()))
                .toList();
    }

    public List<RdvDtos.PlageResponse> findAllPlages() {
        return plageRepo.findByActifTrueOrderByHeureDebutAsc()
                .stream()
                .map(p -> new RdvDtos.PlageResponse(
                        p.getId(), p.getHeureDebut(), p.getHeureFin(), p.getLibelle()))
                .toList();
    }
}
