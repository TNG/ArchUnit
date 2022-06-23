package com.tngtech.archunit.tooling.utils;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.tngtech.archunit.tooling.TestFile;

public class JUnitEngineResolver {

    @Nonnull
    public List<String> resolveJUnitEngines(TestFile testFile) {
        /* TODO configuration issue:
            If archunit and junit-vintage are both included, then ArchUnit tests are being run twice (since they are discoverable by both engines).
            This behavior should either be suppressed somehow or, at the very least, clearly stated in the docs.

            (the former could be done by having ArchUnitTestEngine detect if JUnit Vintage is configured to run, and if so - skip JUnit 4 tests
            during discovery. There does not, however, seem to exist a foolproof way of detecting whether a given engine is configured to run)
        */
        if (TestFile.TestingFramework.JUNIT4.equals(testFile.getTestingFramework())
                && testFile.getFixture().getSimpleName().contains("Arch")) {
            return Arrays.asList("junit-jupiter", "archunit");
        }
        return Arrays.asList("junit-jupiter", "junit-vintage", "archunit");
    }
}
