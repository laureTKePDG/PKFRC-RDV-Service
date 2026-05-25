package com.pkfrc.rdv.infrastructure.persistence.mapper;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.infrastructure.persistence.entity.RendezVousEntity;
import org.springframework.stereotype.Component;

@Component
public class RendezVousMapper {

    private final UtilisateurMapper utilisateurMapper;

    public RendezVousMapper(UtilisateurMapper utilisateurMapper) {
        this.utilisateurMapper = utilisateurMapper;
    }

    public RdvDtos.RendezVousResponse toResponse(RendezVousEntity entity) {
        var plage = new RdvDtos.PlageResponse(
                entity.getPlage().getId(),
                entity.getPlage().getHeureDebut(),
                entity.getPlage().getHeureFin(),
                entity.getPlage().getLibelle()
        );

        var participants = entity.getParticipants()
                .stream()
                .map(utilisateurMapper::toResponse)
                .toList();

        return new RdvDtos.RendezVousResponse(
                entity.getRefRdv(),
                entity.getService().getRef(),
                entity.getService().getLibelle(),
                utilisateurMapper.toResponse(entity.getResponsable()),
                plage,
                entity.getDateRdv(),
                entity.getMotifRdv(),
                entity.getStatut(),
                participants,
                entity.getCreatedAt()
        );
    }
}
