package com.tngtech.archunit.core.importer;

import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.DEPENDENCY_RESOLUTION_PROCESS_PROPERTY_PREFIX;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_ACCESSES_TO_TYPES_PROPERTY_NAME;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_ANNOTATION_TYPES_PROPERTY_NAME;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_ENCLOSING_TYPES_PROPERTY_NAME;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_GENERIC_SIGNATURE_TYPES_DEFAULT_VALUE;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_GENERIC_SIGNATURE_TYPES_PROPERTY_NAME;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_MEMBER_TYPES_PROPERTY_NAME;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcess.MAX_ITERATIONS_FOR_SUPERTYPES_PROPERTY_NAME;
import static com.tngtech.archunit.testutil.ArchConfigurationRule.resetConfigurationAround;
import static java.util.Collections.singleton;

public class DependencyResolutionProcessTestUtils {

    static JavaClasses importClassesWithOnlyGenericTypeResolution(final Class<?>... classes) {
        return ImporterWithAdjustedResolutionRuns.disableAllIterationsExcept(
                MAX_ITERATIONS_FOR_GENERIC_SIGNATURE_TYPES_PROPERTY_NAME, MAX_ITERATIONS_FOR_GENERIC_SIGNATURE_TYPES_DEFAULT_VALUE
        ).importClasses(classes);
    }

    static JavaClass importClassWithOnlyGenericTypeResolution(final Class<?> clazz) {
        return importClassesWithOnlyGenericTypeResolution(clazz).get(clazz);
    }

    static class ImporterWithAdjustedResolutionRuns {
        private final Set<String> propertyNames;
        private final Optional<Integer> number;

        private ImporterWithAdjustedResolutionRuns(Set<String> propertyNames, Optional<Integer> number) {
            this.propertyNames = propertyNames;
            this.number = number;
        }

        static ImporterWithAdjustedResolutionRuns disableAllIterationsExcept(String... propertyNames) {
            return new ImporterWithAdjustedResolutionRuns(ImmutableSet.copyOf(propertyNames), Optional.<Integer>empty());
        }

        static ImporterWithAdjustedResolutionRuns disableAllIterationsExcept(String propertyName, int number) {
            return new ImporterWithAdjustedResolutionRuns(singleton(propertyName), Optional.of(number));
        }

        JavaClass importClass(final Class<?> clazz) {
            return importClasses(clazz).get(clazz);
        }

        public JavaClasses importClasses(final Class<?>... classes) {
            return resetConfigurationAround(new Callable<JavaClasses>() {
                @Override
                public JavaClasses call() {
                    ImporterWithAdjustedResolutionRuns.this.setAllIterationsToZeroExcept(propertyNames);
                    return new ClassFileImporter().importClasses(classes);
                }
            });
        }

        private void setAllIterationsToZeroExcept(Set<String> propertyNames) {
            Set<String> allPropertyNames = newHashSet(
                    MAX_ITERATIONS_FOR_MEMBER_TYPES_PROPERTY_NAME,
                    MAX_ITERATIONS_FOR_ACCESSES_TO_TYPES_PROPERTY_NAME,
                    MAX_ITERATIONS_FOR_SUPERTYPES_PROPERTY_NAME,
                    MAX_ITERATIONS_FOR_ENCLOSING_TYPES_PROPERTY_NAME,
                    MAX_ITERATIONS_FOR_ANNOTATION_TYPES_PROPERTY_NAME,
                    MAX_ITERATIONS_FOR_GENERIC_SIGNATURE_TYPES_PROPERTY_NAME
            );
            allPropertyNames.removeAll(propertyNames);

            for (String propertyNameToDisable : allPropertyNames) {
                setResolutionProperty(propertyNameToDisable, 0);
            }

            if (number.isPresent()) {
                setResolutionProperty(getOnlyElement(propertyNames), number.get());
            }
        }

        private void setResolutionProperty(String propertyName, int number) {
            ArchConfiguration.get().setProperty(DEPENDENCY_RESOLUTION_PROCESS_PROPERTY_PREFIX + "." + propertyName, String.valueOf(number));
        }
    }
}