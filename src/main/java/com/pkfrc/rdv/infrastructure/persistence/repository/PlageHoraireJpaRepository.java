package com.pkfrc.rdv.infrastructure.persistence.repository;

import com.pkfrc.rdv.infrastructure.persistence.entity.PlageHoraireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlageHoraireJpaRepository extends JpaRepository<PlageHoraireEntity, Long> {

    Optional<PlageHoraireEntity> findByHeureDebut(LocalTime heureDebut);

    List<PlageHoraireEntity> findByActifTrueOrderByHeureDebutAsc();
}
