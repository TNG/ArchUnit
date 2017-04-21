package com.tngtech.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.TestUtils;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportTestUtils;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchUnitTestStructureTest {
    private static final JavaClasses archUnitClasses = new ClassFileImporter().importPackages("com.tngtech.archunit");

    @Test
    public void only_TestUtils_accesses_ImportTestUtils() {
        classes().that().haveFullyQualifiedName(ImportTestUtils.class.getName())
                .should(onlyBeAccessedByClassesThat(
                        have(nameMatching(TestUtils.class.getName() + ".*"))
                                .or(have(nameMatching(ImportTestUtils.class.getName() + ".*")))))
                .because("we wan't one central TestUtils for all tests")
                .check(archUnitClasses);
    }

    @Test
    public void ImportTestUtils_doesnt_access_TestUtils() {
        noClasses().that().haveNameMatching(ImportTestUtils.class.getName() + ".*")
                .should().accessClassesThat().haveNameMatching(TestUtils.class.getName() + ".*")
                .because("we wan't one central TestUtils for all tests")
                .check(archUnitClasses);
    }
}
