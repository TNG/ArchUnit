package com.tngtech.archunit.library;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.Architectures.OnionArchitecture;
import com.tngtech.archunit.library.testclasses.coveringallclasses.first.First;
import com.tngtech.archunit.library.testclasses.coveringallclasses.second.Second;
import com.tngtech.archunit.library.testclasses.coveringallclasses.third.Third;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.cli.CliAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.persistence.PersistenceAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.rest.RestAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.application.ApplicationLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.domain.model.DomainModelLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.domain.service.DomainServiceLayerClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.LayeredArchitectureTest.absolute;
import static com.tngtech.archunit.library.LayeredArchitectureTest.assertPatternMatches;
import static com.tngtech.archunit.library.LayeredArchitectureTest.expectedAccessViolationPattern;
import static com.tngtech.archunit.library.LayeredArchitectureTest.expectedEmptyLayerPattern;
import static com.tngtech.archunit.library.LayeredArchitectureTest.expectedFieldTypePattern;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.beans.Introspector.decapitalize;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class OnionArchitectureTest {

    static Stream<OnionArchitecture> onion_architecture_description() {
        return Stream.of(
                onionArchitecture()
                        .domainModels("onionarchitecture.domain.model..")
                        .domainServices("onionarchitecture.domain.service..")
                        .applicationServices("onionarchitecture.application..")
                        .adapter("cli", "onionarchitecture.adapter.cli..")
                        .adapter("persistence", "onionarchitecture.adapter.persistence..")
                        .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query.."),
                onionArchitecture()
                        .domainModels(alwaysTrue().as("'onionarchitecture.domain.model..'"))
                        .domainServices(alwaysTrue().as("'onionarchitecture.domain.service..'"))
                        .applicationServices(alwaysTrue().as("'onionarchitecture.application..'"))
                        .adapter("cli", alwaysTrue().as("'onionarchitecture.adapter.cli..'"))
                        .adapter("persistence", alwaysTrue().as("'onionarchitecture.adapter.persistence..'"))
                        .adapter("rest", alwaysTrue().as("'onionarchitecture.adapter.rest.command..', 'onionarchitecture.adapter.rest.query..'"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void onion_architecture_description(OnionArchitecture architecture) {
        assertThat(architecture.getDescription()).isEqualTo(
                "Onion architecture consisting of" + lineSeparator() +
                        "domain models ('onionarchitecture.domain.model..')" + lineSeparator() +
                        "domain services ('onionarchitecture.domain.service..')" + lineSeparator() +
                        "application services ('onionarchitecture.application..')" + lineSeparator() +
                        "adapter 'cli' ('onionarchitecture.adapter.cli..')" + lineSeparator() +
                        "adapter 'persistence' ('onionarchitecture.adapter.persistence..')" + lineSeparator() +
                        "adapter 'rest' ('onionarchitecture.adapter.rest.command..', 'onionarchitecture.adapter.rest.query..')"
        );
    }

    @Test
    public void onion_architecture_description_with_missing_layers() {
        OnionArchitecture architecture = onionArchitecture();

        assertThat(architecture.getDescription()).isEqualTo("Onion architecture consisting of");
    }

    @Test
    public void onion_architecture_overridden_description() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModels("onionarchitecture.domain.model..")
                .domainServices("onionarchitecture.domain.service..")
                .applicationServices("onionarchitecture.application..")
                .adapter("cli", "onionarchitecture.adapter.cli..")
                .adapter("persistence", "onionarchitecture.adapter.persistence..")
                .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query..")
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
    }

    @Test
    public void onion_architecture_because_clause() {
        ArchRule architecture = onionArchitecture()
                .domainModels("onionarchitecture.domain.model..")
                .domainServices("onionarchitecture.domain.service..")
                .applicationServices("onionarchitecture.application..")
                .adapter("cli", "onionarchitecture.adapter.cli..")
                .adapter("persistence", "onionarchitecture.adapter.persistence..")
                .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query..")
                .as("overridden")
                .because("some reason");

        assertThat(architecture.getDescription()).isEqualTo("overridden, because some reason");
    }

    static Stream<OnionArchitecture> onion_architecture_gathers_all_violations() {
        return Stream.of(
                getTestOnionArchitecture(),
                onionArchitecture()
                        .domainModels(resideInAnyPackage(absolute("onionarchitecture.domain.model")))
                        .domainServices(resideInAnyPackage(absolute("onionarchitecture.domain.service")))
                        .applicationServices(resideInAnyPackage(absolute("onionarchitecture.application")))
                        .adapter("cli", resideInAnyPackage(absolute("onionarchitecture.adapter.cli")))
                        .adapter("persistence", resideInAnyPackage(absolute("onionarchitecture.adapter.persistence")))
                        .adapter("rest", resideInAnyPackage(absolute("onionarchitecture.adapter.rest")))
        );
    }

    @ParameterizedTest
    @MethodSource
    void onion_architecture_gathers_all_violations(OnionArchitecture architecture) {
        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);

        assertPatternMatches(result.getFailureReport().getDetails(), getExpectedOnionViolations().toPatterns());
    }

    @Test
    public void onion_architecture_is_not_violated_by_ignored_dependencies() {
        OnionArchitecture onionIgnoringOriginApplicationLayerClass = getTestOnionArchitecture()
                .ignoreDependency(ApplicationLayerClass.class, CliAdapterLayerClass.class)
                .ignoreDependency(ApplicationLayerClass.class.getName(), PersistenceAdapterLayerClass.class.getName())
                .ignoreDependency(simpleNameStartingWith("ApplicationLayerCl"), simpleNameContaining("estAdapterLayerCl"));
        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = onionIgnoringOriginApplicationLayerClass.evaluate(classes);

        ExpectedOnionViolations expectedViolations = getExpectedOnionViolations().withoutViolationsWithOrigin(ApplicationLayerClass.class);
        assertPatternMatches(result.getFailureReport().getDetails(), expectedViolations.toPatterns());
    }

    @Test
    public void onion_architecture_with_overwritten_description_retains_ignored_dependencies() {
        ArchRule onionIgnoringOriginApplicationLayerClass = getTestOnionArchitecture()
                .ignoreDependency(equivalentTo(ApplicationLayerClass.class), alwaysTrue())
                .because("some reason causing description to be overwritten");

        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = onionIgnoringOriginApplicationLayerClass.evaluate(classes);

        ExpectedOnionViolations expectedViolations = getExpectedOnionViolations().withoutViolationsWithOrigin(ApplicationLayerClass.class);
        assertPatternMatches(result.getFailureReport().getDetails(), expectedViolations.toPatterns());
    }

    @Test
    public void onion_architecture_rejects_empty_layers_by_default() {
        OnionArchitecture architecture = anOnionArchitectureWithEmptyLayers();

        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureOnionArchitectureWithEmptyLayers(result);
    }

    static Stream<Function<OnionArchitecture, OnionArchitecture>> ruleTextModifications() {
        return Stream.<Function<OnionArchitecture, OnionArchitecture>>of(
                Function.identity(),
                onionArchitecture -> onionArchitecture
                        .as("Onion architecture consisting of (optional) changed"),
                onionArchitecture -> onionArchitecture
                        .because("changed"),
                onionArchitecture -> onionArchitecture
                        .as("Onion architecture consisting of (optional) changed")
                        .because("changed")
        );
    }

    @ParameterizedTest
    @MethodSource("ruleTextModifications")
    void onion_architecture_allows_empty_layers_if_all_layers_are_optional(Function<OnionArchitecture, OnionArchitecture> modifyRule) {
        OnionArchitecture architecture = modifyRule.apply(anOnionArchitectureWithEmptyLayers().withOptionalLayers(true));
        assertThat(architecture.getDescription()).startsWith("Onion architecture consisting of (optional)");

        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isFalse();
        assertThat(result.getFailureReport().isEmpty()).as("failure report").isTrue();
    }

    @Test
    public void onion_architecture_rejects_empty_layers_if_layers_are_explicitly_not_optional_by_default() {
        OnionArchitecture architecture = anOnionArchitectureWithEmptyLayers().withOptionalLayers(false);

        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureOnionArchitectureWithEmptyLayers(result);
    }

    @ParameterizedTest
    @MethodSource("ruleTextModifications")
    void onion_architecture_ensure_all_classes_are_contained_in_architecture(Function<OnionArchitecture, OnionArchitecture> modifyRule) {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class);

        OnionArchitecture architectureNotCoveringAllClasses = modifyRule.apply(onionArchitecture().withOptionalLayers(true)
                .domainModels("..first..")
                .ensureAllClassesAreContainedInArchitecture());

        assertThatRule(architectureNotCoveringAllClasses).checking(classes)
                .hasOnlyOneViolation("Class <" + Second.class.getName() + "> is not contained in architecture");

        OnionArchitecture architectureCoveringAllClasses = architectureNotCoveringAllClasses
                .domainServices("..second..");
        assertThatRule(architectureCoveringAllClasses).checking(classes).hasNoViolation();
    }

    @Test
    public void onion_architecture_ensure_all_classes_are_contained_in_architecture_ignoring_packages() {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class, Third.class);

        OnionArchitecture architecture = onionArchitecture().withOptionalLayers(true)
                .domainModels("..first..")
                .ensureAllClassesAreContainedInArchitectureIgnoring("..second..");

        assertThatRule(architecture).checking(classes)
                .hasOnlyOneViolation("Class <" + Third.class.getName() + "> is not contained in architecture");
    }

    @Test
    public void onion_architecture_ensure_all_classes_are_contained_in_architecture_ignoring_predicate() {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class, Third.class);

        OnionArchitecture architecture = onionArchitecture().withOptionalLayers(true)
                .domainModels("..first..")
                .ensureAllClassesAreContainedInArchitectureIgnoring(simpleName("Second"));

        assertThatRule(architecture).checking(classes)
                .hasOnlyOneViolation("Class <" + Third.class.getName() + "> is not contained in architecture");
    }

    private static OnionArchitecture getTestOnionArchitecture() {
        return onionArchitecture()
                .domainModels(absolute("onionarchitecture.domain.model"))
                .domainServices(absolute("onionarchitecture.domain.service"))
                .applicationServices(absolute("onionarchitecture.application"))
                .adapter("cli", absolute("onionarchitecture.adapter.cli"))
                .adapter("persistence", absolute("onionarchitecture.adapter.persistence"))
                .adapter("rest", absolute("onionarchitecture.adapter.rest"));
    }

    private ExpectedOnionViolations getExpectedOnionViolations() {
        ExpectedOnionViolations expectedViolations = new ExpectedOnionViolations();
        expectedViolations.from(DomainModelLayerClass.class)
                .to(DomainServiceLayerClass.class, ApplicationLayerClass.class, CliAdapterLayerClass.class,
                        PersistenceAdapterLayerClass.class, RestAdapterLayerClass.class);
        expectedViolations.from(DomainServiceLayerClass.class)
                .to(ApplicationLayerClass.class, CliAdapterLayerClass.class, PersistenceAdapterLayerClass.class, RestAdapterLayerClass.class);
        expectedViolations.from(ApplicationLayerClass.class)
                .to(CliAdapterLayerClass.class, PersistenceAdapterLayerClass.class, RestAdapterLayerClass.class);
        expectedViolations.from(CliAdapterLayerClass.class).to(PersistenceAdapterLayerClass.class, RestAdapterLayerClass.class);
        expectedViolations.from(PersistenceAdapterLayerClass.class).to(CliAdapterLayerClass.class, RestAdapterLayerClass.class);
        expectedViolations.from(RestAdapterLayerClass.class).to(CliAdapterLayerClass.class, PersistenceAdapterLayerClass.class);
        return expectedViolations;
    }

    private OnionArchitecture anOnionArchitectureWithEmptyLayers() {
        return onionArchitecture()
                .domainModels(absolute("onionarchitecture.domain.model.does.not.exist"))
                .domainServices(absolute("onionarchitecture.domain.service.not.there"))
                .applicationServices(absolute("onionarchitecture.application.http410"));
    }

    static void assertFailureOnionArchitectureWithEmptyLayers(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isTrue();
        assertPatternMatches(result.getFailureReport().getDetails(), ImmutableSet.of(
                expectedEmptyLayerPattern("adapter"), expectedEmptyLayerPattern("application service"),
                expectedEmptyLayerPattern("domain model"), expectedEmptyLayerPattern("domain service")
        ));
    }

    private static class ExpectedOnionViolations {
        private final Set<ExpectedOnionViolation> expected;

        private ExpectedOnionViolations() {
            this(new HashSet<>());
        }

        private ExpectedOnionViolations(Set<ExpectedOnionViolation> expected) {
            this.expected = expected;
        }

        From from(Class<?> from) {
            return new From(from);
        }

        private ExpectedOnionViolations add(ExpectedOnionViolation expectedOnionViolation) {
            expected.add(expectedOnionViolation);
            return this;
        }

        public ExpectedOnionViolations withoutViolationsWithOrigin(Class<?> clazz) {
            return new ExpectedOnionViolations(expected.stream()
                    .filter(expectedViolation -> !expectedViolation.from.equals(clazz))
                    .collect(toSet()));
        }

        Set<String> toPatterns() {
            ImmutableSet.Builder<String> result = ImmutableSet.builder();
            for (ExpectedOnionViolation expectedOnionViolation : expected) {
                result.addAll(expectedOnionViolation.toPatterns());
            }
            return result.build();
        }

        class From {
            private final Class<?> from;

            private From(Class<?> from) {
                this.from = from;
            }

            ExpectedOnionViolations to(Class<?>... to) {
                return ExpectedOnionViolations.this.add(new ExpectedOnionViolation(from, to));
            }
        }
    }

    private static class ExpectedOnionViolation {
        private final Class<?> from;
        private final Set<Class<?>> tos;

        private ExpectedOnionViolation(Class<?> from, Class<?>[] tos) {
            this.from = from;
            this.tos = ImmutableSet.copyOf(tos);
        }

        Set<String> toPatterns() {
            ImmutableSet.Builder<String> result = ImmutableSet.builder();
            for (Class<?> to : tos) {
                result.add(expectedAccessViolationPattern(from, "call", to, "callMe"))
                        .add(expectedFieldTypePattern(from, decapitalize(to.getSimpleName()), to));
            }
            return result.build();
        }
    }
}
