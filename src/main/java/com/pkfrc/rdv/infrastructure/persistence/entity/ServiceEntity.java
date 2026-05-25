package com.pkfrc.rdv.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ref;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(nullable = false)
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServiceEntity() {}

    public ServiceEntity(String ref, String libelle) {
        this.ref = ref;
        this.libelle = libelle;
    }

    public Long getId()      { return id; }
    public String getRef()   { return ref; }
    public String getLibelle() { return libelle; }
    public boolean isActif() { return actif; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceEntity s)) return false;
        return Objects.equals(ref, s.ref);
    }

    @Override
    public int hashCode() { return Objects.hash(ref); }
}
