# PKFRC RDV Service

Service REST de prise de rendez-vous dans les services administratifs du PK Fokam Research Center.

## Stack technique

| Composant | Version |
|-----------|---------|
| Java | 21 (Virtual Threads, Records, Sealed Classes, Pattern Matching) |
| Spring Boot | 3.2.5 |
| Spring Security | 6.x (SecurityFilterChain DSL) |
| PostgreSQL | 16 |
| Flyway | 10.x |
| Hibernate | 6.x |
| Testcontainers | 1.19.x |
| ArchUnit | 1.3.x |

## Architecture

```
src/main/java/com/pkfrc/rdv/
├── RdvServiceApplication.java
├── config/                         # Spring Security, OpenAPI
├── controller/                     # Controllers REST (WebMVC)
├── application/
│   └── dto/                        # Records Java 21 (immutables)
├── domain/
│   ├── model/                      # Enums métier (UserRole, RdvStatut)
│   └── service/                    # Logique métier
└── infrastructure/
    ├── exception/                  # RdvException (sealed), GlobalExceptionHandler
    └── persistence/
        ├── entity/                 # Entités JPA
        ├── mapper/                 # Entity ↔ DTO
        └── repository/            # Spring Data JPA
```

### Règles d'architecture (vérifiées par ArchUnit)
- Les **controllers** n'accèdent pas directement aux repositories JPA
- Les **domain services** ne dépendent pas des controllers
- Les **entités JPA** ne filtrent pas dans la couche controller

## Fonctionnalités métier

### Règles de gestion
- Un RDV a **un seul responsable**
- Un responsable ne peut avoir **plus d'un RDV par plage horaire**
- Un RDV peut avoir **maximum 2 personnes physiques** (clients)
- Un RDV doit être pris **au moins 2 jours avant** sa date
- Plages horaires : **08h00 à 16h00**, durée 1h (8 plages)
- Services : **Archives, DAF, RH, Comptabilité, Affaires sociales**

### Gestion de la concurrence
La création simultanée de RDV pour le même responsable/plage/date est gérée par :
1. **`Isolation.REPEATABLE_READ`** : évite les lectures fantômes
2. **Pessimistic READ lock** (`SELECT FOR SHARE`) : blocage pendant la vérification de disponibilité
3. **Contrainte unique PostgreSQL** (`uq_responsable_plage_date`) : protection ultime
4. **`@Version` (optimistic locking)** : pour les mises à jour concurrentes

## Prérequis

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (pour PostgreSQL)

## Lancement rapide

### Via Docker Compose (recommandé)

```bash
# Cloner le projet
git clone https://github.com/pkfrc/rdv-service.git
cd rdv-service

# Démarrer PostgreSQL + application
docker-compose up --build

# L'API sera disponible sur http://localhost:8080
```


## Exécuter les tests

```bash
# Tous les tests (nécessite Docker pour Testcontainers)
./mvnw test

# Tests unitaires uniquement
./mvnw test -pl . -Dtest="com.pkfrc.rdv.unit.**"

# Tests d'intégration uniquement
./mvnw test -pl . -Dtest="com.pkfrc.rdv.integration.**"

# Tests d'architecture
./mvnw test -pl . -Dtest="ArchitectureTest"

# Rapport de couverture (JaCoCo)
./mvnw verify
open target/site/jacoco/index.html
```

## API Documentation

Authentification : Basic Auth (`admin` / `admin123` en dev)

### Endpoints principaux

#### Utilisateurs
```
POST   /api/v1/utilisateurs              # Créer un utilisateur (client ou responsable)
GET    /api/v1/utilisateurs              # Lister tous les utilisateurs (?role=CLIENT|RESPONSABLE)
GET    /api/v1/utilisateurs/{ref}        # Consulter un utilisateur
DELETE /api/v1/utilisateurs/{ref}        # Désactiver (soft delete)
```

#### Rendez-vous
```
POST   /api/v1/rendez-vous               # Prendre un RDV
GET    /api/v1/rendez-vous/{refRdv}      # Consulter un RDV
GET    /api/v1/rendez-vous?date=...      # Lister par date (ISO: yyyy-MM-dd)
GET    /api/v1/rendez-vous/service/{ref}?debut=...&fin=...   # Lister par service/période
PATCH  /api/v1/rendez-vous/{refRdv}/annuler    # Annuler
PATCH  /api/v1/rendez-vous/{refRdv}/terminer   # Clôturer
```

#### Référentiel (accès public)
```
GET    /api/v1/referentiel/services      # 5 services disponibles
GET    /api/v1/referentiel/plages        # 8 plages horaires (08h-16h)
```

### Exemples cURL

```bash
# Créer un responsable
curl -X POST http://localhost:8080/api/v1/utilisateurs \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "ref": "RESP-001",
    "email": "responsable@pkfrc.cm",
    "telephone": "+237699000001",
    "nom": "Fokam",
    "prenom": "Pierre",
    "role": "RESPONSABLE"
  }'

# Créer un client
curl -X POST http://localhost:8080/api/v1/utilisateurs \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "ref": "CLT-001",
    "email": "client@example.cm",
    "telephone": "+237677000001",
    "nom": "Dupont",
    "prenom": "Marie",
    "role": "CLIENT"
  }'

# Prendre un RDV (J+3, plage 10h)
curl -X POST http://localhost:8080/api/v1/rendez-vous \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "refClient": "CLT-001",
    "refRDV": "RDV-2026-001",
    "refService": "SRV-RH",
    "refResponsable": "RESP-001",
    "dateRDV": "2026-05-28T10:00:00",
    "motifRdv": "Demande de document administratif"
  }'

# Annuler un RDV
curl -X PATCH http://localhost:8080/api/v1/rendez-vous/RDV-2026-001/annuler \
  -u admin:admin123
```

## Migrations de base de données (Flyway)

```
src/main/resources/db/migration/
├── V1__init_schema.sql        # Création des tables, index, contraintes
└── V2__seed_reference_data.sql # Données fixes (services + plages horaires)
```

En production, Flyway applique automatiquement les migrations au démarrage.



## Runbook de déploiement

### Déploiement initial
```bash
# 1. Build de l'image Docker
docker build -t pkfrc/rdv-service:1.0.0 .

# 2. Push vers le registry
docker push pkfrc/rdv-service:1.0.0

# 3. Appliquer le déploiement K8s
kubectl apply -f k8s/

# 4. Vérifier le statut
kubectl rollout status deployment/rdv-service
```

### Rollback
```bash
# Rollback vers la version précédente
kubectl rollout undo deployment/rdv-service

# Ou vers une version spécifique
kubectl rollout undo deployment/rdv-service --to-revision=2

# Vérifier
kubectl rollout history deployment/rdv-service
```

### Health checks
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

## Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DB_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/rdvdb` |
| `DB_USER` | Utilisateur BD | `rdv_user` |
| `DB_PASSWORD` | Mot de passe BD | `rdv_pass` |
| `API_USER` | Utilisateur Basic Auth | `admin` |
| `API_PASSWORD` | Mot de passe Basic Auth | `admin123` |
| `PORT` | Port HTTP | `8080` |
| `SPRING_PROFILES_ACTIVE` | Profil Spring | `prod` |

