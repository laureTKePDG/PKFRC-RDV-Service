package com.pkfrc.rdv.infrastructure.persistence.entity;

import com.pkfrc.rdv.domain.model.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "utilisateurs", indexes = {
        @Index(name = "idx_utilisateurs_email", columnList = "email"),
        @Index(name = "idx_utilisateurs_role", columnList = "role")
})
public class UtilisateurEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ref;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    private UserRole role;

    @Column(nullable = false)
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UtilisateurEntity() {}

    public UtilisateurEntity(String ref, String email, String telephone,
                              String nom, String prenom, UserRole role) {
        this.ref = ref;
        this.email = email;
        this.telephone = telephone;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
    }

    // Getters
    public Long getId()           { return id; }
    public String getRef()        { return ref; }
    public String getEmail()      { return email; }
    public String getTelephone()  { return telephone; }
    public String getNom()        { return nom; }
    public String getPrenom()     { return prenom; }
    public UserRole getRole()     { return role; }
    public boolean isActif()      { return actif; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters nécessaires
    public void setActif(boolean actif) { this.actif = actif; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtilisateurEntity u)) return false;
        return Objects.equals(ref, u.ref);
    }

    @Override
    public int hashCode() { return Objects.hash(ref); }

    @Override
    public String toString() {
        return "UtilisateurEntity[ref=%s, role=%s]".formatted(ref, role);
    }
}
