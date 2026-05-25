package com.pkfrc.rdv.domain.service;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.infrastructure.exception.RdvException;
import com.pkfrc.rdv.infrastructure.persistence.entity.UtilisateurEntity;
import com.pkfrc.rdv.infrastructure.persistence.mapper.UtilisateurMapper;
import com.pkfrc.rdv.infrastructure.persistence.repository.UtilisateurJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(UtilisateurService.class);

    private final UtilisateurJpaRepository utilisateurRepo;
    private final UtilisateurMapper mapper;

    public UtilisateurService(UtilisateurJpaRepository utilisateurRepo,
                               UtilisateurMapper mapper) {
        this.utilisateurRepo = utilisateurRepo;
        this.mapper = mapper;
    }

    @Transactional
    public RdvDtos.UtilisateurResponse creerUtilisateur(RdvDtos.UtilisateurRequest request) {
        log.info("Création utilisateur ref={}, role={}", request.ref(), request.role());

        if (utilisateurRepo.existsByRef(request.ref())) {
            throw RdvException.refRdvDuplicate(request.ref());
        }
        if (utilisateurRepo.existsByEmail(request.email())) {
            throw new RdvException.ConflictException("email",
                    "Un utilisateur avec l'email [%s] existe déjà".formatted(request.email()));
        }

        var entity = mapper.toEntity(request);
        var saved = utilisateurRepo.save(entity);
        log.info("Utilisateur créé: {}", saved.getRef());
        return mapper.toResponse(saved);
    }

    public RdvDtos.UtilisateurResponse findByRef(String ref) {
        return utilisateurRepo.findByRef(ref)
                .map(mapper::toResponse)
                .orElseThrow(() -> RdvException.utilisateurNotFound(ref));
    }

    public List<RdvDtos.UtilisateurResponse> findByRole(UserRole role) {
        return utilisateurRepo.findByRoleAndActifTrue(role)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<RdvDtos.UtilisateurResponse> findAll() {
        return utilisateurRepo.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void desactiverUtilisateur(String ref) {
        var entity = utilisateurRepo.findByRef(ref)
                .orElseThrow(() -> RdvException.utilisateurNotFound(ref));
        entity.setActif(false);
        log.info("Utilisateur désactivé: {}", ref);
    }

    // Méthode interne pour la couche service RDV
    UtilisateurEntity findEntityByRefAndRole(String ref, UserRole expectedRole) {
        var entity = utilisateurRepo.findByRef(ref)
                .filter(UtilisateurEntity::isActif)
                .orElseThrow(() -> RdvException.utilisateurNotFound(ref));

        if (entity.getRole() != expectedRole) {
            throw RdvException.roleMismatch(ref, expectedRole.name());
        }
        return entity;
    }

    UtilisateurEntity findEntityByRef(String ref) {
        return utilisateurRepo.findByRef(ref)
                .filter(UtilisateurEntity::isActif)
                .orElseThrow(() -> RdvException.utilisateurNotFound(ref));
    }
}
