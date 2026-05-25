package com.pkfrc.rdv.unit.service;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.domain.service.UtilisateurService;
import com.pkfrc.rdv.infrastructure.exception.RdvException;
import com.pkfrc.rdv.infrastructure.persistence.entity.UtilisateurEntity;
import com.pkfrc.rdv.infrastructure.persistence.mapper.UtilisateurMapper;
import com.pkfrc.rdv.infrastructure.persistence.repository.UtilisateurJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UtilisateurService - Tests unitaires")
class UtilisateurServiceTest {

    @Mock private UtilisateurJpaRepository repo;
    @Mock private UtilisateurMapper mapper;

    @InjectMocks
    private UtilisateurService service;

    @Test
    @DisplayName("Doit créer un utilisateur valide")
    void shouldCreateUser() {
        var request = new RdvDtos.UtilisateurRequest(
                "USR-001", "test@pkfrc.cm", "699000001", "Toto", "Titi", UserRole.CLIENT);
        var entity = new UtilisateurEntity("USR-001", "test@pkfrc.cm", "699000001",
                "Toto", "Titi", UserRole.CLIENT);
        var response = new RdvDtos.UtilisateurResponse("USR-001", "test@pkfrc.cm", "699000001",
                "Toto", "Titi", UserRole.CLIENT, true, null);

        when(repo.existsByRef("USR-001")).thenReturn(false);
        when(repo.existsByEmail("test@pkfrc.cm")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repo.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        var result = service.creerUtilisateur(request);

        assertThat(result.ref()).isEqualTo("USR-001");
        assertThat(result.role()).isEqualTo(UserRole.CLIENT);
    }

    @Test
    @DisplayName("Doit lever ConflictException si la référence existe déjà")
    void shouldThrowOnDuplicateRef() {
        var request = new RdvDtos.UtilisateurRequest(
                "USR-DUP", "new@pkfrc.cm", "699000001", "A", "B", UserRole.CLIENT);
        when(repo.existsByRef("USR-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.creerUtilisateur(request))
                .isInstanceOf(RdvException.ConflictException.class);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("findByRef doit lever ResourceNotFoundException si absent")
    void shouldThrowWhenUserNotFound() {
        when(repo.findByRef("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRef("GHOST"))
                .isInstanceOf(RdvException.ResourceNotFoundException.class)
                .hasMessageContaining("GHOST");
    }
}
