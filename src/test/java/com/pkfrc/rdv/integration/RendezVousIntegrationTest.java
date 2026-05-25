package com.pkfrc.rdv.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkfrc.rdv.application.dto.RdvDtos;
import com.pkfrc.rdv.domain.model.UserRole;
import com.pkfrc.rdv.infrastructure.persistence.repository.RendezVousJpaRepository;
import com.pkfrc.rdv.infrastructure.persistence.repository.UtilisateurJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration avec Testcontainers PostgreSQL (pas de H2).
 * Couvre le flux complet de création de RDV et la gestion de la concurrence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - Flux RDV complet")
class RendezVousIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("rdvdb_test")
            .withUsername("rdv_test")
            .withPassword("rdv_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RendezVousJpaRepository rdvRepo;
    @Autowired UtilisateurJpaRepository utilisateurRepo;

    // Dates fixes pour les tests (J+3 pour respecter le délai de 2 jours)
    private static final LocalDate DATE_TEST = LocalDate.now().plusDays(3);
    private static final LocalDateTime DATE_HEURE_TEST = DATE_TEST.atTime(10, 0);

    @BeforeEach
    void setUp() throws Exception {
        // Créer les utilisateurs de test via API
        creerUtilisateur("RESP-TEST-01", "resp01@test.cm", "699000001", "Doe", "John", UserRole.RESPONSABLE);
        creerUtilisateur("RESP-TEST-02", "resp02@test.cm", "699000002", "Doe", "Jane", UserRole.RESPONSABLE);
        creerUtilisateur("CLT-TEST-01", "client01@test.cm", "699000003", "Client", "Un", UserRole.CLIENT);
        creerUtilisateur("CLT-TEST-02", "client02@test.cm", "699000004", "Client", "Deux", UserRole.CLIENT);
    }

    @AfterEach
    void tearDown() {
        rdvRepo.deleteAll();
        utilisateurRepo.deleteAll();
    }

    // ── Tests de flux métier ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Flux complet : créer un RDV puis le consulter")
    void shouldCreateAndRetrieveRdv() throws Exception {
        var request = buildRdvRequest("RDV-INT-001", "SRV-RH", "RESP-TEST-01",
                DATE_HEURE_TEST, "CLT-TEST-01", null);

        // Création
        var result = mockMvc.perform(post("/api/v1/rendez-vous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refRdv").value("RDV-INT-001"))
                .andExpect(jsonPath("$.statut").value("CONFIRME"))
                .andExpect(jsonPath("$.participants").isArray())
                .andReturn();

        // Consultation
        mockMvc.perform(get("/api/v1/rendez-vous/RDV-INT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refRdv").value("RDV-INT-001"))
                .andExpect(jsonPath("$.responsable.ref").value("RESP-TEST-01"));
    }

    @Test
    @WithMockUser
    @DisplayName("Doit rejeter un RDV si le responsable a déjà un créneau à cette heure")
    void shouldRejectDuplicateSlotForResponsable() throws Exception {
        // Premier RDV
        var req1 = buildRdvRequest("RDV-UNIQ-001", "SRV-RH", "RESP-TEST-01",
                DATE_HEURE_TEST, "CLT-TEST-01", null);
        mockMvc.perform(post("/api/v1/rendez-vous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second RDV avec même responsable/plage/date → conflit
        var req2 = buildRdvRequest("RDV-UNIQ-002", "SRV-DAF", "RESP-TEST-01",
                DATE_HEURE_TEST, "CLT-TEST-02", null);
        mockMvc.perform(post("/api/v1/rendez-vous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("Doit rejeter un RDV pris trop tôt (délai < 2 jours)")
    void shouldRejectRdvTooClose() throws Exception {
        var request = buildRdvRequest("RDV-PROCHE", "SRV-RH", "RESP-TEST-01",
                LocalDateTime.now().plusDays(1).withHour(10), "CLT-TEST-01", null);

        mockMvc.perform(post("/api/v1/rendez-vous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RDV_DELAI_MINIMUM"));
    }

    @Test
    @WithMockUser
    @DisplayName("Annuler un RDV confirmé")
    void shouldCancelRdv() throws Exception {
        var request = buildRdvRequest("RDV-CANCEL", "SRV-RH", "RESP-TEST-01",
                DATE_TEST.plusDays(1).atTime(10, 0), "CLT-TEST-01", null);
        mockMvc.perform(post("/api/v1/rendez-vous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/rendez-vous/RDV-CANCEL/annuler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULE"));
    }

    @Test
    @WithMockUser
    @DisplayName("Test concurrence : deux requêtes simultanées pour le même créneau → une seule passe")
    void shouldHandleConcurrentRdvCreation() throws Exception {
        int threadCount = 10;
        var latch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(threadCount);
        var successCount = new AtomicInteger(0);
        var conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    latch.await(); // Tous les threads attendent le signal
                    var req = buildRdvRequest(
                            "RDV-CONCURRENT-%d".formatted(idx),
                            "SRV-RH", "RESP-TEST-01",
                            DATE_TEST.plusDays(2).atTime(14, 0),
                            idx % 2 == 0 ? "CLT-TEST-01" : "CLT-TEST-02",
                            null
                    );
                    var response = mockMvc.perform(post("/api/v1/rendez-vous")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int status = response.getResponse().getStatus();
                    if (status == 201) successCount.incrementAndGet();
                    else if (status == 409) conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignorer les exceptions de threading dans le test
                }
            });
        }

        latch.countDown(); // Déclencher tous les threads simultanément
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        // Un seul RDV doit avoir été créé pour ce créneau
        assertThat(successCount.get())
                .as("Exactement 1 RDV doit être créé malgré la concurrence")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("Les autres requêtes doivent recevoir un conflit 409")
                .isGreaterThanOrEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("Référentiel - lister les services (accès public)")
    void shouldListServicesPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/referentiel/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("Référentiel - lister les plages horaires (accès public)")
    void shouldListPlagesPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/referentiel/plages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    @DisplayName("API sécurisée - requête sans auth doit retourner 401")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/utilisateurs"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @WithMockUser
    private void creerUtilisateur(String ref, String email, String tel,
                                   String nom, String prenom, UserRole role) throws Exception {
        var request = new RdvDtos.UtilisateurRequest(ref, email, tel, nom, prenom, role);
        mockMvc.perform(post("/api/v1/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private RdvDtos.RendezVousRequest buildRdvRequest(String refRdv, String refService,
            String refResp, LocalDateTime dateHeure, String client1, String client2) {
        return new RdvDtos.RendezVousRequest(
                client1, refRdv, refService, refResp, dateHeure, "Motif du RDV", client2);
    }
}
