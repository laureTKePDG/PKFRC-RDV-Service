package com.pkfrc.rdv.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "plages_horaires")
public class PlageHoraireEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "heure_debut", nullable = false, unique = true)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(nullable = false, length = 20)
    private String libelle;

    @Column(nullable = false)
    private boolean actif = true;

    protected PlageHoraireEntity() {}

    public Long getId()            { return id; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public LocalTime getHeureFin()   { return heureFin; }
    public String getLibelle()     { return libelle; }
    public boolean isActif()       { return actif; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlageHoraireEntity p)) return false;
        return Objects.equals(heureDebut, p.heureDebut);
    }

    @Override
    public int hashCode() { return Objects.hash(heureDebut); }
}
