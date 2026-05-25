package com.pkfrc.rdv;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Tests ArchUnit pour garantir les règles d'architecture.
 * Vérifie que la structure hexagonale est respectée.
 */
@DisplayName("ArchUnit - Règles d'architecture")
class ArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter().importPackages("com.pkfrc.rdv");
    }

    @Test
    @DisplayName("Les controllers ne doivent pas accéder directement aux repositories JPA")
    void controllersShouldNotAccessRepositoriesDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.repository..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Les domain services ne doivent pas dépendre de la couche controller")
    void domainServicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controller..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Les entities JPA ne doivent pas être utilisées dans les controllers")
    void entitiesShouldNotLeakToControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.entity..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Les services domaine ne doivent être annotés qu'avec @Service")
    void domainServicesShouldBeAnnotatedWithService() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.service..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

        rule.check(classes);
    }

    @Test
    @DisplayName("L'architecture en couches doit être respectée")
    void layeredArchitectureShouldBeRespected() {
        var rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controllers").definedBy("com.pkfrc.rdv.controller..")
                .layer("Application").definedBy("com.pkfrc.rdv.application..")
                .layer("Domain").definedBy("com.pkfrc.rdv.domain..")
                .layer("Infrastructure").definedBy("com.pkfrc.rdv.infrastructure..")
                .whereLayer("Controllers").mayOnlyAccessLayers("Application", "Domain", "Infrastructure")
                .whereLayer("Domain").mayOnlyAccessLayers("Application", "Infrastructure")
                .whereLayer("Application").mayOnlyAccessLayers("Infrastructure");

        rule.check(classes);
    }
}
