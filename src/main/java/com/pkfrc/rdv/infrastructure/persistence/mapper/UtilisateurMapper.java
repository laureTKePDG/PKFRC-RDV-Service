package com.pkfrc.rdv.infrastructure.persistence.mapper;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.infrastructure.persistence.entity.UtilisateurEntity;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public RdvDtos.UtilisateurResponse toResponse(UtilisateurEntity entity) {
        return new RdvDtos.UtilisateurResponse(
                entity.getRef(),
                entity.getEmail(),
                entity.getTelephone(),
                entity.getNom(),
                entity.getPrenom(),
                entity.getRole(),
                entity.isActif(),
                entity.getCreatedAt()
        );
    }

    public UtilisateurEntity toEntity(RdvDtos.UtilisateurRequest request) {
        return new UtilisateurEntity(
                request.ref(),
                request.email(),
                request.telephone(),
                request.nom(),
                request.prenom(),
                request.role()
        );
    }
}
