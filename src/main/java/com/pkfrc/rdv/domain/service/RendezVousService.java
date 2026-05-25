package com.pkfrc.rdv.domain.service;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.RdvStatut;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.infrastructure.exception.RdvException;
import com.pkfrc.rdv.infrastructure.persistence.entity.RendezVousEntity;
import com.pkfrc.rdv.infrastructure.persistence.entity.UtilisateurEntity;
import com.pkfrc.rdv.infrastructure.persistence.mapper.RendezVousMapper;
import com.pkfrc.rdv.infrastructure.persistence.repository.PlageHoraireJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.RendezVousJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.ServiceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RendezVousService {

    private static final Logger log = LoggerFactory.getLogger(RendezVousService.class);
    private static final int DELAI_MIN_JOURS = 2;
    private static final int MAX_PARTICIPANTS = 2;

    private final RendezVousJpaRepository rdvRepo;
    private final ServiceJpaRepository serviceRepo;
    private final PlageHoraireJpaRepository plageRepo;
    private final UtilisateurService utilisateurService;
    private final RendezVousMapper mapper;

    public RendezVousService(RendezVousJpaRepository rdvRepo,
                              ServiceJpaRepository serviceRepo,
                              PlageHoraireJpaRepository plageRepo,
                              UtilisateurService utilisateurService,
                              RendezVousMapper mapper) {
        this.rdvRepo = rdvRepo;
        this.serviceRepo = serviceRepo;
        this.plageRepo = plageRepo;
        this.utilisateurService = utilisateurService;
        this.mapper = mapper;
    }

    /**
     * Crée un RDV avec gestion robuste de la concurrence.
     *
     * Stratégie de concurrence :
     * 1. Isolation REPEATABLE_READ : évite les lectures non répétables
     * 2. Pessimistic READ lock : SELECT FOR SHARE sur les conflits existants
     * 3. Contrainte unique BD (uq_responsable_plage_date) : filet de sécurité ultime
     * 4. @Version sur l'entité : locking optimiste pour les mises à jour
     *
     * Si deux requêtes arrivent simultanément pour le même responsable/plage/date,
     * l'une obtiendra le lock et l'autre attendra. Quand la première valide,
     * la contrainte unique empêche la seconde de persister.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public RdvDtos.RendezVousResponse prendreRendezVous(RdvDtos.RendezVousRequest request) {
        log.info("Prise de RDV ref={} pour responsable={}", request.refRDV(), request.refResponsable());

        // 1. Vérification unicité de la référence RDV
        if (rdvRepo.existsByRefRdv(request.refRDV())) {
            throw RdvException.refRdvDuplicate(request.refRDV());
        }

        // 2. Extraction date et heure depuis le LocalDateTime
        var dateRdv  = request.dateRDV().toLocalDate();
        var heureRdv = request.dateRDV().toLocalTime().withMinute(0).withSecond(0).withNano(0);

        // 3. Validation : RDV au moins 2 jours à l'avance
        validerDelaiMinimum(dateRdv);

        // 4. Résolution des entités référencées
        var service       = serviceRepo.findByRef(request.refService())
                .filter(s -> s.isActif())
                .orElseThrow(() -> RdvException.serviceNotFound(request.refService()));

        var responsable   = utilisateurService.findEntityByRefAndRole(
                request.refResponsable(), UserRole.RESPONSABLE);

        var plage         = plageRepo.findByHeureDebut(heureRdv)
                .filter(p -> p.isActif())
                .orElseThrow(() -> new RdvException.ResourceNotFoundException(
                        "PlageHoraire", heureRdv.toString()));

        // 5. Vérification disponibilité avec locking pessimiste (évite race condition)
        var conflits = rdvRepo.findConflitsWithLock(
                responsable.getRef(), plage.getId(), dateRdv);

        if (!conflits.isEmpty()) {
            throw RdvException.responsableIndisponible(
                    responsable.getRef(), plage.getLibelle(), dateRdv.toString());
        }

        // 6. Construction du RDV
        var rdv = new RendezVousEntity(
                request.refRDV(), service, responsable, plage, dateRdv, request.motifRdv());

        // 7. Ajout participants (max 2)
        ajouterParticipants(rdv, request.refClient(), request.refClient2());

        // 8. Persistance
        var saved = rdvRepo.save(rdv);
        log.info("RDV créé avec succès: ref={}, date={}, responsable={}",
                saved.getRefRdv(), saved.getDateRdv(), saved.getResponsable().getRef());

        return mapper.toResponse(saved);
    }

    public RdvDtos.RendezVousResponse findByRef(String refRdv) {
        var entity = rdvRepo.findByRefRdvWithDetails(refRdv)
                .orElseThrow(() -> RdvException.rdvNotFound(refRdv));
        return mapper.toResponse(entity);
    }

    public List<RdvDtos.RendezVousResponse> findByDate(LocalDate date) {
        return rdvRepo.findByDateRdvWithDetails(date)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<RdvDtos.RendezVousResponse> findByServiceAndPeriode(
            String refService, LocalDate debut, LocalDate fin) {
        // Vérification que le service existe
        serviceRepo.findByRef(refService)
                .orElseThrow(() -> RdvException.serviceNotFound(refService));

        return rdvRepo.findByServiceAndPeriode(refService, debut, fin, RdvStatut.CONFIRME)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public RdvDtos.RendezVousResponse annulerRendezVous(String refRdv) {
        log.info("Annulation du RDV ref={}", refRdv);
        var entity = rdvRepo.findByRefRdvWithDetails(refRdv)
                .orElseThrow(() -> RdvException.rdvNotFound(refRdv));
        entity.annuler();
        return mapper.toResponse(entity);
    }

    @Transactional
    public RdvDtos.RendezVousResponse terminerRendezVous(String refRdv) {
        log.info("Clôture du RDV ref={}", refRdv);
        var entity = rdvRepo.findByRefRdvWithDetails(refRdv)
                .orElseThrow(() -> RdvException.rdvNotFound(refRdv));
        entity.terminer();
        return mapper.toResponse(entity);
    }

    // ── Méthodes privées ─────────────────────────────────────────────────────

    private void validerDelaiMinimum(LocalDate dateRdv) {
        var dateMinimale = LocalDate.now().plusDays(DELAI_MIN_JOURS);
        if (dateRdv.isBefore(dateMinimale)) {
            throw RdvException.rdvTropProcheException(DELAI_MIN_JOURS);
        }
    }

    private void ajouterParticipants(RendezVousEntity rdv, String refClient1, String refClient2) {
        if (refClient1 == null || refClient1.isBlank()) {
            throw new RdvException.BusinessRuleException("PARTICIPANT_OBLIGATOIRE",
                    "Au moins un client doit être associé au RDV");
        }

        // Switch pattern matching Java 21
        int nbParticipants = switch (refClient2) {
            case null, "" -> 1;
            default -> 2;
        };

        if (nbParticipants > MAX_PARTICIPANTS) {
            throw RdvException.maxParticipantsAtteint();
        }

        var client1 = utilisateurService.findEntityByRefAndRole(refClient1, UserRole.CLIENT);
        rdv.addParticipant(client1);

        if (nbParticipants == 2) {
            if (refClient2.equals(refClient1)) {
                throw new RdvException.BusinessRuleException("PARTICIPANTS_DISTINCTS",
                        "Les deux participants doivent être des personnes différentes");
            }
            var client2 = utilisateurService.findEntityByRefAndRole(refClient2, UserRole.CLIENT);
            rdv.addParticipant(client2);
        }
    }
}
