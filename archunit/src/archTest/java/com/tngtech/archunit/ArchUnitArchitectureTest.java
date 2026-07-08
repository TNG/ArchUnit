package com.tngtech.archunit;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

import static com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import static com.tngtech.archunit.ArchitectureTestImportOptions.sourceRootOf;

@AnalyzeClasses(
        packages = "com.tngtech.archunit",
        importOptions = {DoNotIncludeTests.class, DoNotIncludeSelfTests.class, ArchUnitArchitectureTest.DoNotIncludeTestResources.class})
public class ArchUnitArchitectureTest {
    private ArchUnitArchitectureTest() {
    }

    @ArchTest
    public static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);

    public static final class DoNotIncludeTestResources implements ImportOption {
        private static final String TEST_RESOURCES_ROOT = sourceRootOf(ArchUnitArchitectureTest.class)
                .replace("/classes/java/archTest", "/resources/test");

        @Override
        public boolean includes(Location location) {
            return !location.contains(TEST_RESOURCES_ROOT);
        }
    }
}
