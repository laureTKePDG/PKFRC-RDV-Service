package com.pkfrc.rdv.infrastructure.persistence.repository;

import com.pkfrc.rdv.infrastructure.persistence.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceJpaRepository extends JpaRepository<ServiceEntity, Long> {

    Optional<ServiceEntity> findByRef(String ref);

    List<ServiceEntity> findByActifTrue();

    boolean existsByRef(String ref);
}
