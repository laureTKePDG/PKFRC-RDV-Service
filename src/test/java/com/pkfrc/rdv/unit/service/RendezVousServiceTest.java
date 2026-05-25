package com.pkfrc.rdv.unit.service;

import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.RdvStatut;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.domain.service.RendezVousService;
import com.pkfrc.rdv.domain.service.UtilisateurService;
import com.pkfrc.rdv.infrastructure.exception.RdvException;
import com.pkfrc.rdv.infrastructure.persistence.entity.*;
import com.pkfrc.rdv.infrastructure.persistence.mapper.RendezVousMapper;
import com.pkfrc.rdv.infrastructure.persistence.mapper.UtilisateurMapper;
import com.pkfrc.rdv.infrastructure.persistence.repository.PlageHoraireJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.RendezVousJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.ServiceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RendezVousService - Tests unitaires")
class RendezVousServiceTest {

    @Mock private RendezVousJpaRepository rdvRepo;
    @Mock private ServiceJpaRepository serviceRepo;
    @Mock private PlageHoraireJpaRepository plageRepo;
    @Mock private UtilisateurService utilisateurService;
    @Mock private RendezVousMapper mapper;

    @InjectMocks
    private RendezVousService rdvService;

    private ServiceEntity serviceFixture;
    private UtilisateurEntity responsableFixture;
    private UtilisateurEntity clientFixture;
    private PlageHoraireEntity plageFixture;

    @BeforeEach
    void setUp() {
        serviceFixture = new ServiceEntity("SRV-RH", "RH");
        responsableFixture = new UtilisateurEntity(
                "RESP-001", "resp@pkfrc.cm", "699000001", "Dupont", "Jean", UserRole.RESPONSABLE);
        clientFixture = new UtilisateurEntity(
                "CLT-001", "client@pkfrc.cm", "699000002", "Martin", "Marie", UserRole.CLIENT);
        plageFixture = buildPlage(1L, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Nested
    @DisplayName("prendreRendezVous")
    class PrendreRendezVous {

        @Test
        @DisplayName("Doit créer un RDV valide avec un seul participant")
        void shouldCreateValidRdvWithOneParticipant() {
            // Given
            var request = buildRequest("RDV-001", LocalDate.now().plusDays(3), 9);

            when(rdvRepo.existsByRefRdv("RDV-001")).thenReturn(false);
            when(serviceRepo.findByRef("SRV-RH")).thenReturn(Optional.of(serviceFixture));
            when(utilisateurService.findEntityByRefAndRole("RESP-001", UserRole.RESPONSABLE))
                    .thenReturn(responsableFixture);
            when(plageRepo.findByHeureDebut(LocalTime.of(9, 0))).thenReturn(Optional.of(plageFixture));
            when(rdvRepo.findConflitsWithLock("RESP-001", 1L, LocalDate.now().plusDays(3)))
                    .thenReturn(List.of());
            when(utilisateurService.findEntityByRefAndRole("CLT-001", UserRole.CLIENT))
                    .thenReturn(clientFixture);
            when(rdvRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(buildResponse("RDV-001"));

            // When
            var result = rdvService.prendreRendezVous(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.refRdv()).isEqualTo("RDV-001");
            verify(rdvRepo).save(any(RendezVousEntity.class));
        }

        @Test
        @DisplayName("Doit rejeter un RDV si la référence existe déjà")
        void shouldRejectDuplicateRefRdv() {
            // Given
            var request = buildRequest("RDV-DUP", LocalDate.now().plusDays(3), 9);
            when(rdvRepo.existsByRefRdv("RDV-DUP")).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> rdvService.prendreRendezVous(request))
                    .isInstanceOf(RdvException.ConflictException.class)
                    .hasMessageContaining("RDV-DUP");

            verify(rdvRepo, never()).save(any());
        }

        @Test
        @DisplayName("Doit rejeter un RDV pris moins de 2 jours à l'avance")
        void shouldRejectRdvTooClose() {
            // Given - demain (J+1) au lieu de J+2 minimum
            var request = buildRequest("RDV-002", LocalDate.now().plusDays(1), 9);
            when(rdvRepo.existsByRefRdv("RDV-002")).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> rdvService.prendreRendezVous(request))
                    .isInstanceOf(RdvException.BusinessRuleException.class)
                    .hasMessageContaining("2 jours");
        }

        @Test
        @DisplayName("Doit rejeter si le responsable a déjà un RDV sur la plage")
        void shouldRejectWhenResponsableHasConflict() {
            // Given
            var request = buildRequest("RDV-003", LocalDate.now().plusDays(3), 9);
            var rdvExistant = mock(RendezVousEntity.class);

            when(rdvRepo.existsByRefRdv("RDV-003")).thenReturn(false);
            when(serviceRepo.findByRef("SRV-RH")).thenReturn(Optional.of(serviceFixture));
            when(utilisateurService.findEntityByRefAndRole("RESP-001", UserRole.RESPONSABLE))
                    .thenReturn(responsableFixture);
            when(plageRepo.findByHeureDebut(LocalTime.of(9, 0))).thenReturn(Optional.of(plageFixture));
            when(rdvRepo.findConflitsWithLock("RESP-001", 1L, LocalDate.now().plusDays(3)))
                    .thenReturn(List.of(rdvExistant));

            // When / Then
            assertThatThrownBy(() -> rdvService.prendreRendezVous(request))
                    .isInstanceOf(RdvException.ConflictException.class)
                    .hasMessageContaining("RESP-001");

            verify(rdvRepo, never()).save(any());
        }

        @Test
        @DisplayName("Doit rejeter si le service est introuvable")
        void shouldRejectUnknownService() {
            var request = buildRequest("RDV-004", LocalDate.now().plusDays(3), 9);
            when(rdvRepo.existsByRefRdv("RDV-004")).thenReturn(false);
            when(serviceRepo.findByRef("SRV-RH")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rdvService.prendreRendezVous(request))
                    .isInstanceOf(RdvException.ResourceNotFoundException.class)
                    .hasMessageContaining("Service");
        }

        @Test
        @DisplayName("Doit rejeter si les deux participants sont identiques")
        void shouldRejectIdenticalParticipants() {
            var request = new RdvDtos.RendezVousRequest(
                    "CLT-001", "RDV-005", "SRV-RH", "RESP-001",
                    LocalDateTime.now().plusDays(3).withHour(9).withMinute(0),
                    "Motif test", "CLT-001"  // même ref que client1
            );

            when(rdvRepo.existsByRefRdv("RDV-005")).thenReturn(false);
            when(serviceRepo.findByRef("SRV-RH")).thenReturn(Optional.of(serviceFixture));
            when(utilisateurService.findEntityByRefAndRole("RESP-001", UserRole.RESPONSABLE))
                    .thenReturn(responsableFixture);
            when(plageRepo.findByHeureDebut(LocalTime.of(9, 0))).thenReturn(Optional.of(plageFixture));
            when(rdvRepo.findConflitsWithLock(any(), any(), any())).thenReturn(List.of());
            when(utilisateurService.findEntityByRefAndRole("CLT-001", UserRole.CLIENT))
                    .thenReturn(clientFixture);

            assertThatThrownBy(() -> rdvService.prendreRendezVous(request))
                    .isInstanceOf(RdvException.BusinessRuleException.class)
                    .hasMessageContaining("différentes");
        }
    }

    @Nested
    @DisplayName("annulerRendezVous")
    class AnnulerRendezVous {

        @Test
        @DisplayName("Doit annuler un RDV confirmé")
        void shouldCancelConfirmedRdv() {
            var rdv = new RendezVousEntity("RDV-X", serviceFixture, responsableFixture,
                    plageFixture, LocalDate.now().plusDays(5), "Motif");
            rdv.addParticipant(clientFixture);

            when(rdvRepo.findByRefRdvWithDetails("RDV-X")).thenReturn(Optional.of(rdv));
            when(mapper.toResponse(rdv)).thenReturn(buildResponse("RDV-X"));

            var result = rdvService.annulerRendezVous("RDV-X");

            assertThat(rdv.getStatut()).isEqualTo(RdvStatut.ANNULE);
        }

        @Test
        @DisplayName("Doit lever une exception si RDV introuvable")
        void shouldThrowIfRdvNotFound() {
            when(rdvRepo.findByRefRdvWithDetails("RDV-GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rdvService.annulerRendezVous("RDV-GHOST"))
                    .isInstanceOf(RdvException.ResourceNotFoundException.class);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RdvDtos.RendezVousRequest buildRequest(String refRdv, LocalDate date, int heure) {
        return new RdvDtos.RendezVousRequest(
                "CLT-001", refRdv, "SRV-RH", "RESP-001",
                date.atTime(heure, 0), "Motif du RDV", null
        );
    }

    private RdvDtos.RendezVousResponse buildResponse(String ref) {
        return new RdvDtos.RendezVousResponse(
                ref, "SRV-RH", "RH",
                new RdvDtos.UtilisateurResponse("RESP-001", "resp@pkfrc.cm", "699000001",
                        "Dupont", "Jean", UserRole.RESPONSABLE, true, null),
                new RdvDtos.PlageResponse(1L, LocalTime.of(9, 0), LocalTime.of(10, 0), "09h-10h"),
                LocalDate.now().plusDays(3), "Motif", RdvStatut.CONFIRME, List.of(), null
        );
    }

    private PlageHoraireEntity buildPlage(Long id, LocalTime debut, LocalTime fin) {
        // Utilisation de réflexion pour setter l'id sur une entité sans setter public
        try {
            var plage = new PlageHoraireEntity() {};
            var f = PlageHoraireEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(plage, id);
            var fd = PlageHoraireEntity.class.getDeclaredField("heureDebut");
            fd.setAccessible(true);
            fd.set(plage, debut);
            var ff = PlageHoraireEntity.class.getDeclaredField("heureFin");
            ff.setAccessible(true);
            ff.set(plage, fin);
            var fl = PlageHoraireEntity.class.getDeclaredField("libelle");
            fl.setAccessible(true);
            fl.set(plage, "09h00 - 10h00");
            var fa = PlageHoraireEntity.class.getDeclaredField("actif");
            fa.setAccessible(true);
            fa.set(plage, true);
            return plage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
