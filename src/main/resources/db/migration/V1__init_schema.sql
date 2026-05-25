-- ============================================================
-- V1__init_schema.sql
-- PKFRC RDV Service - Schéma relationnel normalisé
-- ============================================================

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUM : rôle utilisateur
-- ============================================================
CREATE TYPE user_role AS ENUM ('CLIENT', 'RESPONSABLE');

-- ============================================================
-- TABLE : services administratifs (données fixes)
-- ============================================================
CREATE TABLE services (
    id          BIGSERIAL       PRIMARY KEY,
    ref         VARCHAR(50)     NOT NULL UNIQUE,
    libelle     VARCHAR(100)    NOT NULL,
    actif       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE services IS 'Services administratifs de l''organisation (Archives, DAF, RH, Comptabilité, Affaires sociales)';

-- ============================================================
-- TABLE : utilisateurs (clients + responsables)
-- ============================================================
CREATE TABLE utilisateurs (
    id          BIGSERIAL       PRIMARY KEY,
    ref         VARCHAR(50)     NOT NULL UNIQUE,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    telephone   VARCHAR(20)     NOT NULL,
    nom         VARCHAR(100)    NOT NULL,
    prenom      VARCHAR(100)    NOT NULL,
    role        user_role       NOT NULL,
    actif       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE utilisateurs IS 'Tous les utilisateurs du système (clients et responsables)';
CREATE INDEX idx_utilisateurs_role ON utilisateurs(role);
CREATE INDEX idx_utilisateurs_email ON utilisateurs(email);

-- ============================================================
-- TABLE : plages horaires (données fixes, 08h-16h)
-- ============================================================
CREATE TABLE plages_horaires (
    id          BIGSERIAL       PRIMARY KEY,
    heure_debut TIME            NOT NULL UNIQUE,
    heure_fin   TIME            NOT NULL,
    libelle     VARCHAR(20)     NOT NULL,
    actif       BOOLEAN         NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE plages_horaires IS 'Plages horaires de RDV : 08h00 à 16h00, durée 1h';

-- ============================================================
-- TABLE : rendez-vous
-- ============================================================
CREATE TABLE rendez_vous (
    id              BIGSERIAL       PRIMARY KEY,
    ref_rdv         VARCHAR(50)     NOT NULL UNIQUE,
    service_id      BIGINT          NOT NULL REFERENCES services(id),
    responsable_id  BIGINT          NOT NULL REFERENCES utilisateurs(id),
    plage_id        BIGINT          NOT NULL REFERENCES plages_horaires(id),
    date_rdv        DATE            NOT NULL,
    motif_rdv       TEXT            NOT NULL,
    statut          VARCHAR(20)     NOT NULL DEFAULT 'CONFIRME',
    version         BIGINT          NOT NULL DEFAULT 0,   -- Optimistic locking
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_statut CHECK (statut IN ('CONFIRME', 'ANNULE', 'TERMINE')),

    -- Un responsable ne peut avoir qu'un seul RDV par plage sur une même date
    CONSTRAINT uq_responsable_plage_date UNIQUE (responsable_id, plage_id, date_rdv)
);

COMMENT ON TABLE rendez_vous IS 'Rendez-vous pris dans les services';
CREATE INDEX idx_rdv_date ON rendez_vous(date_rdv);
CREATE INDEX idx_rdv_service ON rendez_vous(service_id);
CREATE INDEX idx_rdv_responsable ON rendez_vous(responsable_id);
CREATE INDEX idx_rdv_statut ON rendez_vous(statut);

-- ============================================================
-- TABLE : participants au RDV (clients associés)
-- Maximum 2 personnes physiques par RDV
-- ============================================================
CREATE TABLE rdv_participants (
    rdv_id          BIGINT  NOT NULL REFERENCES rendez_vous(id) ON DELETE CASCADE,
    utilisateur_id  BIGINT  NOT NULL REFERENCES utilisateurs(id),
    PRIMARY KEY (rdv_id, utilisateur_id)
);

COMMENT ON TABLE rdv_participants IS 'Association RDV <-> clients (max 2 par RDV)';
CREATE INDEX idx_rdv_participants_rdv ON rdv_participants(rdv_id);
CREATE INDEX idx_rdv_participants_user ON rdv_participants(utilisateur_id);

-- ============================================================
-- TRIGGER : mise à jour automatique du champ updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_services_updated_at
    BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_utilisateurs_updated_at
    BEFORE UPDATE ON utilisateurs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_rdv_updated_at
    BEFORE UPDATE ON rendez_vous
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
