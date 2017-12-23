package com.tngtech.archunit;

import com.tngtech.archunit.ArchUnitExampleArchitectureTest.ArchUnitExampleLocations;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit", importOptions = ArchUnitExampleLocations.class)
public class ArchUnitExampleArchitectureTest {
    @ArchTest
    public static final ArchRule examples_should_be_independent_of_Guava =
            noClasses().should().accessClassesThat().resideInAnyPackage("..google..")
                    .because("we want to keep the dependencies of archunit-example minimal");

    public static final class ArchUnitExampleLocations implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("archunit-example");
        }
    }
}
