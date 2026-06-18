package com.jalaldeveloper.accountingsystem;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.jalaldeveloper.accountingsystem");
    }

    @Test
    void domainCoreMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..domain.core..", "..domain.valueobject..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..dataaccess..",
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void applicationServicesMustNotDependOnJpaEntities() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..service.domain..")
                .and().haveSimpleNameEndingWith("ApplicationServiceImpl")
                .should().dependOnClassesThat().resideInAnyPackage("..dataaccess.entity..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void restControllersMustNotDependOnDataaccess() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..application.rest..")
                .and().resideOutsideOfPackage("com.jalaldeveloper.accountingsystem.platform..")
                .should().dependOnClassesThat().resideInAnyPackage("..dataaccess..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void boundedContextsMustNotImportForeignDataaccess() {
        String[] contexts = {"accounting", "purchase", "sales", "pos", "inventory", "contacts"};
        for (String source : contexts) {
            for (String target : contexts) {
                if (source.equals(target)) {
                    continue;
                }
                ArchRule rule = noClasses()
                        .that().resideInAnyPackage("com.jalaldeveloper.accountingsystem." + source + "..")
                        .should().dependOnClassesThat()
                        .resideInAnyPackage("com.jalaldeveloper.accountingsystem." + target + ".dataaccess..")
                        .allowEmptyShould(true);
                rule.check(classes);
            }
        }
    }
}
