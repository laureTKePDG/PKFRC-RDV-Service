package com.pkfrc.rdv.domain.model;

public enum RdvStatut {
    CONFIRME,
    ANNULE,
    TERMINE;

    public boolean isAnnulable() {
        return this == CONFIRME;
    }
}
