-- ============================================================
-- V2__seed_reference_data.sql
-- Données de référence : services et plages horaires
-- ============================================================

-- Services administratifs (5 fixes)
INSERT INTO services (ref, libelle) VALUES
    ('SRV-ARCHIVES',        'Archives'),
    ('SRV-DAF',             'DAF'),
    ('SRV-RH',              'RH'),
    ('SRV-COMPTABILITE',    'Comptabilité'),
    ('SRV-AFFAIRES-SOC',    'Affaires sociales');

-- Plages horaires 08h00 → 16h00 (8 plages d'une heure)
INSERT INTO plages_horaires (heure_debut, heure_fin, libelle) VALUES
    ('08:00', '09:00', '08h00 - 09h00'),
    ('09:00', '10:00', '09h00 - 10h00'),
    ('10:00', '11:00', '10h00 - 11h00'),
    ('11:00', '12:00', '11h00 - 12h00'),
    ('12:00', '13:00', '12h00 - 13h00'),
    ('13:00', '14:00', '13h00 - 14h00'),
    ('14:00', '15:00', '14h00 - 15h00'),
    ('15:00', '16:00', '15h00 - 16h00');
