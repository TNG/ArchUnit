package com.tngtech.archunit.library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.tngtech.archunit.library.Architectures.OnionArchitecture;
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.cli.CliAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.persistence.PersistenceAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.adapter.rest.RestAdapterLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.application.ApplicationLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.domain.model.DomainModelLayerClass;
import com.tngtech.archunit.library.testclasses.onionarchitecture.domain.service.DomainServiceLayerClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.SomePkgClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.DataProviders;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.beans.Introspector.decapitalize;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ArchitecturesTest {
    private static final String NEW_LINE_REPLACE = "###";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @DataProvider
    public static Object[][] layeredArchitectureDefinitions() {
        return testForEach(
                layeredArchitecture()
                        .layer("One").definedBy("..library.testclasses.some.pkg..")
                        .layer("Two").definedBy("..library.testclasses.first.any.pkg..", "..library.testclasses.second.any.pkg..")
                        .optionalLayer("Three").definedBy("..library.testclasses..three..")
                        .whereLayer("One").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                        .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two"),
                layeredArchitecture()
                        .layer("One").definedBy(
                        resideInAnyPackage("..library.testclasses.some.pkg..")
                                .as("'..library.testclasses.some.pkg..'"))
                        .layer("Two").definedBy(
                        resideInAnyPackage("..library.testclasses.first.any.pkg..", "..library.testclasses.second.any.pkg..")
                                .as("'..library.testclasses.first.any.pkg..', '..library.testclasses.second.any.pkg..'"))
                        .optionalLayer("Three").definedBy(
                        resideInAnyPackage("..library.testclasses..three..")
                                .as("'..library.testclasses..three..'"))
                        .whereLayer("One").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                        .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two"));
    }

    @Test
    @UseDataProvider("layeredArchitectureDefinitions")
    public void layered_architecture_description(LayeredArchitecture architecture) {
        assertThat(architecture.getDescription()).isEqualTo(
                "Layered architecture consisting of" + lineSeparator() +
                        "layer 'One' ('..library.testclasses.some.pkg..')" + lineSeparator() +
                        "layer 'Two' ('..library.testclasses.first.any.pkg..', '..library.testclasses.second.any.pkg..')" + lineSeparator() +
                        "optional layer 'Three' ('..library.testclasses..three..')" + lineSeparator() +
                        "where layer 'One' may not be accessed by any layer" + lineSeparator() +
                        "where layer 'Two' may only be accessed by layers ['One']" + lineSeparator() +
                        "where layer 'Three' may only be accessed by layers ['One', 'Two']");
    }

    @Test
    public void layered_architecture_overridden_description() {
        LayeredArchitecture architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
    }

    @Test
    public void layered_architecture_because_clause() {
        ArchRule architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden")
                .because("some reason");

        assertThat(architecture.getDescription()).isEqualTo("overridden, because some reason");
    }

    @Test
    public void layered_architecture_defining_constraint_on_non_existing_target_layer_is_rejected() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no layer");
        thrown.expectMessage("NotThere");

        layeredArchitecture()
                .layer("Some").definedBy("any")
                .whereLayer("NotThere").mayNotBeAccessedByAnyLayer();
    }

    @Test
    public void layered_architecture_defining_constraint_on_non_existing_origin_is_rejected() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no layer");
        thrown.expectMessage("NotThere");

        layeredArchitecture()
                .layer("Some").definedBy("any")
                .whereLayer("Some").mayOnlyBeAccessedByLayers("NotThere");
    }

    @Test
    public void layered_architecture_rejects_empty_layers_by_default() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers();

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureLayeredArchitectureWithEmptyLayers(result);
    }

    @Test
    public void layered_architecture_allows_empty_layers_if_all_layers_are_optional() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers().withOptionalLayers(true);
        assertThat(architecture.getDescription()).startsWith("Layered architecture consisting of (optional)");

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isFalse();
        assertThat(result.getFailureReport().isEmpty()).as("failure report").isTrue();
    }

    @Test
    public void layered_architecture_rejects_empty_layers_if_layers_are_explicity_not_optional_by_default() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers().withOptionalLayers(false);

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureLayeredArchitectureWithEmptyLayers(result);
    }

    private LayeredArchitecture aLayeredArchitectureWithEmptyLayers() {
        return layeredArchitecture()
                .layer("Some").definedBy(absolute("should.not.be.found.."))
                .layer("Other").definedBy(absolute("also.not.found"))
                .layer("Okay").definedBy("..testclasses..");
    }

    private void assertFailureLayeredArchitectureWithEmptyLayers(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isTrue();
        assertPatternMatches(result.getFailureReport().getDetails(),
                ImmutableSet.of(expectedEmptyLayer("Some"), expectedEmptyLayer("Other")));
    }

    @Test
    public void layered_architecture_allows_individual_empty_optionalLayer() {
        LayeredArchitecture architecture = layeredArchitecture()
                .optionalLayer("can be absent").definedBy(absolute("should.not.be.found.."));

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertThat(result.hasViolation()).as("result of evaluating empty optionalLayer has violation").isFalse();
        assertThat(result.getFailureReport().isEmpty()).as("failure report").isTrue();
    }

    @Test
    @UseDataProvider("layeredArchitectureDefinitions")
    public void layered_architecture_gathers_all_layer_violations(LayeredArchitecture architecture) {
        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);

        assertPatternMatches(result.getFailureReport().getDetails(),
                ImmutableSet.of(
                        expectedAccessViolationPattern(FirstAnyPkgClass.class, "call", SomePkgSubClass.class, "callMe"),
                        expectedAccessViolationPattern(SecondThreeAnyClass.class, "call", SomePkgClass.class, "callMe"),
                        expectedAccessViolationPattern(FirstThreeAnyClass.class, "call", FirstAnyPkgClass.class, "callMe"),
                        fieldTypePattern(FirstAnyPkgClass.class, "illegalTarget", SomePkgSubClass.class),
                        fieldTypePattern(FirstThreeAnyClass.class, "illegalTarget", FirstAnyPkgClass.class),
                        fieldTypePattern(SecondThreeAnyClass.class, "illegalTarget", SomePkgClass.class)));
    }

    @DataProvider
    public static Object[][] toIgnore() {
        LayeredArchitecture layeredArchitecture = layeredArchitecture()
                .layer("One").definedBy(absolute("some.pkg.."))
                .whereLayer("One").mayNotBeAccessedByAnyLayer();

        return DataProviders.testForEach(
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(FirstAnyPkgClass.class, SomePkgSubClass.class),
                        "rule with ignore specified as class objects"),
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(FirstAnyPkgClass.class.getName(), SomePkgSubClass.class.getName()),
                        "rule with ignore specified as class names"),
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(name(FirstAnyPkgClass.class.getName()), name(SomePkgSubClass.class.getName())),
                        "rule with ignore specified as predicates"));
    }

    @Test
    @UseDataProvider("toIgnore")
    public void layered_architecture_ignores_specified_violations(RuleWithIgnore layeredArchitectureWithIgnore) {
        JavaClasses classes = new ClassFileImporter().importClasses(
                FirstAnyPkgClass.class, SomePkgSubClass.class,
                SecondThreeAnyClass.class, SomePkgClass.class);

        EvaluationResult result = layeredArchitectureWithIgnore.rule.evaluate(classes);

        assertThat(singleLine(result))
                .doesNotMatch(String.format(".*%s[^%s]*%s.*",
                        quote(FirstAnyPkgClass.class.getName()), NEW_LINE_REPLACE, quote(SomePkgSubClass.class.getName())))
                .matches(String.format(".*%s[^%s]*%s.*",
                        quote(SecondThreeAnyClass.class.getName()), NEW_LINE_REPLACE, quote(SomePkgClass.class.getName())));
    }

    @Test
    public void layered_architecture_combines_multiple_ignores() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                FirstAnyPkgClass.class, SomePkgSubClass.class,
                SecondThreeAnyClass.class, SomePkgClass.class);

        LayeredArchitecture layeredArchitecture = layeredArchitecture()
                .layer("One").definedBy(absolute("some.pkg.."))
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .ignoreDependency(FirstAnyPkgClass.class, SomePkgSubClass.class);

        assertThat(layeredArchitecture.evaluate(classes).hasViolation()).as("result has violation").isTrue();

        layeredArchitecture = layeredArchitecture
                .ignoreDependency(SecondThreeAnyClass.class, SomePkgClass.class);

        assertThat(layeredArchitecture.evaluate(classes).hasViolation()).as("result has violation").isFalse();
    }

    @Test
    public void onion_architecture_description() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModels("onionarchitecture.domain.model..")
                .domainServices("onionarchitecture.domain.service..")
                .applicationServices("onionarchitecture.application..")
                .adapter("cli", "onionarchitecture.adapter.cli..")
                .adapter("persistence", "onionarchitecture.adapter.persistence..")
                .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query..");

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

    @Test
    public void onion_architecture_gathers_all_violations() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModels(absolute("onionarchitecture.domain.model"))
                .domainServices(absolute("onionarchitecture.domain.service"))
                .applicationServices(absolute("onionarchitecture.application"))
                .adapter("cli", absolute("onionarchitecture.adapter.cli"))
                .adapter("persistence", absolute("onionarchitecture.adapter.persistence"))
                .adapter("rest", absolute("onionarchitecture.adapter.rest"));
        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);

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

        assertPatternMatches(result.getFailureReport().getDetails(), expectedViolations.toPatterns());
    }

    @Test
    public void onion_architecture_rejects_empty_layers_by_default() {
        OnionArchitecture architecture = anOnionArchitectureWithEmptyLayers();

        JavaClasses classes = new ClassFileImporter().importPackages(absolute("onionarchitecture"));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureOnionArchitectureWithEmptyLayers(result);
    }

    @Test
    public void onion_architecture_allows_empty_layers_if_all_layers_are_optional() {
        OnionArchitecture architecture = anOnionArchitectureWithEmptyLayers().withOptionalLayers(true);
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

    private OnionArchitecture anOnionArchitectureWithEmptyLayers() {
        return onionArchitecture()
                    .domainModels(absolute("onionarchitecture.domain.model.does.not.exist"))
                    .domainServices(absolute("onionarchitecture.domain.service.not.there"))
                    .applicationServices(absolute("onionarchitecture.application.http410"));
    }

    private void assertFailureOnionArchitectureWithEmptyLayers(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isTrue();
        assertPatternMatches(result.getFailureReport().getDetails(), ImmutableSet.of(
                expectedEmptyLayer("adapter"), expectedEmptyLayer("application service"),
                expectedEmptyLayer("domain model"), expectedEmptyLayer("domain service")
        ));
    }

    private String singleLine(EvaluationResult result) {
        return Joiner.on(NEW_LINE_REPLACE).join(result.getFailureReport().getDetails()).replace("\n", NEW_LINE_REPLACE);
    }

    private void assertPatternMatches(List<String> input, Set<String> expectedRegexes) {
        Set<String> toMatch = new HashSet<>(expectedRegexes);
        for (String line : input) {
            if (!matchIteratorAndRemove(toMatch, line)) {
                Assert.fail("Line '" + line + "' didn't match any pattern in " + expectedRegexes);
            }
        }
        assertThat(toMatch).as("Unmatched Patterns").isEmpty();
    }

    private boolean matchIteratorAndRemove(Set<String> toMatch, String line) {
        for (Iterator<String> toMatchIterator = toMatch.iterator(); toMatchIterator.hasNext(); ) {
            if (line.matches(toMatchIterator.next())) {
                toMatchIterator.remove();
                return true;
            }
        }
        return false;
    }

    private static String expectedAccessViolationPattern(Class<?> from, String fromMethod, Class<?> to, String toMethod) {
        return String.format(".*%s.%s().*%s.%s().*", quote(from.getName()), fromMethod, quote(to.getName()), toMethod);
    }

    private static String expectedEmptyLayer(String layerName) {
        return String.format("Layer '%s' is empty", layerName);
    }

    private static String fieldTypePattern(Class<?> owner, String fieldName, Class<?> fieldType) {
        return String.format("Field .*%s\\.%s.* has type .*<%s>.*", owner.getSimpleName(), fieldName, fieldType.getName());
    }

    private static String[] absolute(String... pkgSuffix) {
        List<String> result = new ArrayList<>();
        for (String s : pkgSuffix) {
            String absolute = ArchitecturesTest.class.getPackage().getName() + ".testclasses." + s;
            result.add(absolute.replaceAll("\\.\\.\\.+", ".."));
        }
        return result.toArray(new String[0]);
    }

    private static class RuleWithIgnore {
        private final ArchRule rule;
        private final String description;

        private RuleWithIgnore(ArchRule rule, String description) {
            this.rule = rule;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static class ExpectedOnionViolations {
        private final Set<ExpectedOnionViolation> expected = new HashSet<>();

        From from(Class<?> from) {
            return new From(from);
        }

        private ExpectedOnionViolations add(ExpectedOnionViolation expectedOnionViolation) {
            expected.add(expectedOnionViolation);
            return this;
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
                        .add(fieldTypePattern(from, decapitalize(to.getSimpleName()), to));
            }
            return result.build();
        }
    }
}
