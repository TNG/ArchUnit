package com.tngtech.archunit.junit.testexamples;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.junit.testexamples.FullAnalyzeClassesSpec.FirstLocationProvider;
import com.tngtech.archunit.junit.testexamples.FullAnalyzeClassesSpec.SecondLocationProvider;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(
        packages = {"first.pkg", "second.pkg"},
        packagesOf = {Object.class, File.class},
        locations = {FirstLocationProvider.class, SecondLocationProvider.class},
        importOptions = {DoNotIncludeTests.class, DoNotIncludeJars.class})
public class FullAnalyzeClassesSpec {
    @ArchTest
    public static final ArchRule irrelevant = classes().should(new ArchCondition<JavaClass>("exist") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
        }
    });

    public static class FirstLocationProvider implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Collections.emptySet();
        }
    }

    public static class SecondLocationProvider implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Collections.emptySet();
        }
    }
}
