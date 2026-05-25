package com.pkfrc.rdv.infrastructure.persistence.repository;

import com.pkfrc.rdv.domain.model.RdvStatut;
import com.pkfrc.rdv.infrastructure.persistence.entity.RendezVousEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RendezVousJpaRepository extends JpaRepository<RendezVousEntity, Long> {

    Optional<RendezVousEntity> findByRefRdv(String refRdv);

    @Query("""
        SELECT r FROM RendezVousEntity r
        JOIN FETCH r.service
        JOIN FETCH r.responsable
        JOIN FETCH r.plage
        WHERE r.refRdv = :refRdv
        """)
    Optional<RendezVousEntity> findByRefRdvWithDetails(@Param("refRdv") String refRdv);

    /**
     * Vérifie la disponibilité du responsable sur une plage/date donnée.
     * Utilisé pour la validation avant création.
     */
    @Query("""
        SELECT COUNT(r) > 0 FROM RendezVousEntity r
        WHERE r.responsable.ref = :refResponsable
          AND r.plage.id = :plageId
          AND r.dateRdv = :dateRdv
          AND r.statut <> com.pkfrc.rdv.domain.model.RdvStatut.ANNULE
        """)
    boolean existsConflitResponsable(
            @Param("refResponsable") String refResponsable,
            @Param("plageId") Long plageId,
            @Param("dateRdv") LocalDate dateRdv
    );

    /**
     * Lecture des RDV d'un responsable avec pessimistic read pour éviter les conditions de course
     * lors de créations simultanées. SELECT FOR SHARE au niveau SQL.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        SELECT r FROM RendezVousEntity r
        WHERE r.responsable.ref = :refResponsable
          AND r.plage.id = :plageId
          AND r.dateRdv = :dateRdv
          AND r.statut <> com.pkfrc.rdv.domain.model.RdvStatut.ANNULE
        """)
    List<RendezVousEntity> findConflitsWithLock(
            @Param("refResponsable") String refResponsable,
            @Param("plageId") Long plageId,
            @Param("dateRdv") LocalDate dateRdv
    );

    @Query("""
        SELECT r FROM RendezVousEntity r
        JOIN FETCH r.service
        JOIN FETCH r.responsable
        JOIN FETCH r.plage
        WHERE r.dateRdv = :dateRdv
        ORDER BY r.plage.heureDebut
        """)
    List<RendezVousEntity> findByDateRdvWithDetails(@Param("dateRdv") LocalDate dateRdv);

    @Query("""
        SELECT r FROM RendezVousEntity r
        JOIN FETCH r.service
        JOIN FETCH r.responsable
        JOIN FETCH r.plage
        WHERE r.service.ref = :refService
          AND r.dateRdv BETWEEN :debut AND :fin
          AND r.statut = :statut
        ORDER BY r.dateRdv, r.plage.heureDebut
        """)
    List<RendezVousEntity> findByServiceAndPeriode(
            @Param("refService") String refService,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin,
            @Param("statut") RdvStatut statut
    );

    boolean existsByRefRdv(String refRdv);
}
