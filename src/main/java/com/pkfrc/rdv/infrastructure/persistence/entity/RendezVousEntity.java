package com.pkfrc.rdv.infrastructure.persistence.entity;

import com.pkfrc.rdv.domain.model.RdvStatut;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
    name = "rendez_vous",
    indexes = {
        @Index(name = "idx_rdv_date", columnList = "date_rdv"),
        @Index(name = "idx_rdv_service", columnList = "service_id"),
        @Index(name = "idx_rdv_responsable", columnList = "responsable_id"),
        @Index(name = "idx_rdv_statut", columnList = "statut")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_responsable_plage_date",
            columnNames = {"responsable_id", "plage_id", "date_rdv"}
        )
    }
)
public class RendezVousEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_rdv", nullable = false, unique = true, length = 50)
    private String refRdv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private UtilisateurEntity responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plage_id", nullable = false)
    private PlageHoraireEntity plage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rdv_participants",
        joinColumns = @JoinColumn(name = "rdv_id"),
        inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<UtilisateurEntity> participants = new HashSet<>();

    @Column(name = "date_rdv", nullable = false)
    private LocalDate dateRdv;

    @Column(name = "motif_rdv", nullable = false, columnDefinition = "TEXT")
    private String motifRdv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RdvStatut statut = RdvStatut.CONFIRME;

    /**
     * Optimistic locking pour gestion de la concurrence.
     * Évite les conflits lors de créations/modifications simultanées.
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RendezVousEntity() {}

    public RendezVousEntity(String refRdv, ServiceEntity service, UtilisateurEntity responsable,
                             PlageHoraireEntity plage, LocalDate dateRdv, String motifRdv) {
        this.refRdv = refRdv;
        this.service = service;
        this.responsable = responsable;
        this.plage = plage;
        this.dateRdv = dateRdv;
        this.motifRdv = motifRdv;
    }

    public void addParticipant(UtilisateurEntity participant) {
        this.participants.add(participant);
    }

    public void annuler() {
        if (!statut.isAnnulable()) {
            throw new IllegalStateException("Le RDV [%s] ne peut pas être annulé (statut: %s)".formatted(refRdv, statut));
        }
        this.statut = RdvStatut.ANNULE;
    }

    public void terminer() {
        this.statut = RdvStatut.TERMINE;
    }

    // Getters
    public Long getId()                        { return id; }
    public String getRefRdv()                  { return refRdv; }
    public ServiceEntity getService()          { return service; }
    public UtilisateurEntity getResponsable()  { return responsable; }
    public PlageHoraireEntity getPlage()       { return plage; }
    public Set<UtilisateurEntity> getParticipants() { return participants; }
    public LocalDate getDateRdv()              { return dateRdv; }
    public String getMotifRdv()                { return motifRdv; }
    public RdvStatut getStatut()               { return statut; }
    public Long getVersion()                   { return version; }
    public Instant getCreatedAt()              { return createdAt; }
    public Instant getUpdatedAt()              { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RendezVousEntity r)) return false;
        return Objects.equals(refRdv, r.refRdv);
    }

    @Override
    public int hashCode() { return Objects.hash(refRdv); }

    @Override
    public String toString() {
        return "RendezVousEntity[ref=%s, date=%s, statut=%s]".formatted(refRdv, dateRdv, statut);
    }
}
