package com.tngtech.archunit.exampletest;

import javax.persistence.EntityManager;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationLineMatcher;
import com.tngtech.archunit.library.freeze.ViolationStore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * This test demonstrates the use of {@link FreezingArchRule} with 'default' configuration.
 * While both rules shown have numerous violations, most of those violations have been 'frozen', i.e. at some point in time all violations
 * were recorded as accepted for the moment. Only violations added afterwards will be reported.<br>
 * You can see how the default text based {@link ViolationStore} stores the results under {@code src/test/resources/frozen} configured
 * via {@value ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}. You can also
 * observe that if you fix an old violation, this store will automatically be adjusted to not allow any regression.<br>
 * Furthermore you can observe how the default {@link ViolationLineMatcher} will ignore changes in line numbers of recorded violations,
 * i.e. if you only change the line numbers of frozen violations, the test will still pass.
 */
@Category(Example.class)
public class FrozenRulesTest {
    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingCodingRules.class);

    @Test
    public void no_classes_should_depend_on_service() {
        freeze(noClasses().should().dependOnClassesThat().resideInAPackage("..service.."))
                .check(classes);
    }

    @Test
    public void no_classes_should_use_the_EntityManager() {
        freeze(noClasses().should().dependOnClassesThat().areAssignableTo(EntityManager.class))
                .check(classes);
    }
}
