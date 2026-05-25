package com.pkfrc.rdv.infrastructure.persistence.repository;

import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.infrastructure.persistence.entity.UtilisateurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurJpaRepository extends JpaRepository<UtilisateurEntity, Long> {

    Optional<UtilisateurEntity> findByRef(String ref);

    Optional<UtilisateurEntity> findByEmail(String email);

    List<UtilisateurEntity> findByRoleAndActifTrue(UserRole role);

    boolean existsByRef(String ref);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UtilisateurEntity u WHERE u.role = :role AND u.actif = true")
    List<UtilisateurEntity> findActiveByRole(UserRole role);
}
