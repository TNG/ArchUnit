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

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ArchitecturesTest {
    private static final String NEW_LINE_REPLACE = "###";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void description_of_layered_architecture() {
        LayeredArchitecture architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .layer("Two").definedBy("first.any.pkg..", "second.any.pkg..")
                .layer("Three").definedBy("..three..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two");

        assertThat(architecture.getDescription()).isEqualTo(
                "Layered architecture consisting of" + lineSeparator() +
                        "layer 'One' ('some.pkg..')" + lineSeparator() +
                        "layer 'Two' ('first.any.pkg..', 'second.any.pkg..')" + lineSeparator() +
                        "layer 'Three' ('..three..')" + lineSeparator() +
                        "where layer 'One' may not be accessed by any layer" + lineSeparator() +
                        "where layer 'Two' may only be accessed by layers ['One']" + lineSeparator() +
                        "where layer 'Three' may only be accessed by layers ['One', 'Two']");
    }

    @Test
    public void overridden_description_of_layered_architecture() {
        LayeredArchitecture architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
    }

    @Test
    public void because_clause_on_layered_architecture() {
        ArchRule architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden")
                .because("some reason");

        assertThat(architecture.getDescription()).isEqualTo("overridden, because some reason");
    }

    @Test
    public void defining_constraint_on_non_existing_target_layer_is_rejected() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no layer");
        thrown.expectMessage("NotThere");

        layeredArchitecture()
                .layer("Some").definedBy("any")
                .whereLayer("NotThere").mayNotBeAccessedByAnyLayer();
    }

    @Test
    public void defining_constraint_on_non_existing_origin_is_rejected() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no layer");
        thrown.expectMessage("NotThere");

        layeredArchitecture()
                .layer("Some").definedBy("any")
                .whereLayer("Some").mayOnlyBeAccessedByLayers("NotThere");
    }

    @Test
    public void gathers_all_layer_violations() {
        LayeredArchitecture architecture = layeredArchitecture()
                .layer("One").definedBy(absolute("some.pkg.."))
                .layer("Two").definedBy(absolute("first.any.pkg..", "second.any.pkg.."))
                .layer("Three").definedBy(absolute("..three.."))
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two");

        JavaClasses classes = new ClassFileImporter().importPackages(getClass().getPackage().getName() + ".testclasses");

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
    public void ignores_specified_violations(RuleWithIgnore layeredArchitectureWithIgnore) {
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
    public void combines_multiple_ignores() {
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
    }    @Test
    public void description_of_onion_architecture() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModel("onionarchitecture.domain.model..")
                .domainService("onionarchitecture.domain.service..")
                .application("onionarchitecture.application..")
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
    public void overridden_description_of_onion_architecture() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModel("onionarchitecture.domain.model..")
                .domainService("onionarchitecture.domain.service..")
                .application("onionarchitecture.application..")
                .adapter("cli", "onionarchitecture.adapter.cli..")
                .adapter("persistence", "onionarchitecture.adapter.persistence..")
                .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query..")
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
    }

    @Test
    public void because_clause_on_onion_architecture() {
        ArchRule architecture = onionArchitecture()
                .domainModel("onionarchitecture.domain.model..")
                .domainService("onionarchitecture.domain.service..")
                .application("onionarchitecture.application..")
                .adapter("cli", "onionarchitecture.adapter.cli..")
                .adapter("persistence", "onionarchitecture.adapter.persistence..")
                .adapter("rest", "onionarchitecture.adapter.rest.command..", "onionarchitecture.adapter.rest.query..")
                .as("overridden")
                .because("some reason");

        assertThat(architecture.getDescription()).isEqualTo("overridden, because some reason");
    }

    @Test
    public void gathers_all_onion_architecture_violations() {
        OnionArchitecture architecture = onionArchitecture()
                .domainModel(absolute("onionarchitecture.domain.model"))
                .domainService(absolute("onionarchitecture.domain.service"))
                .application(absolute("onionarchitecture.application"))
                .adapter("cli", absolute("onionarchitecture.adapter.cli"))
                .adapter("persistence", absolute("onionarchitecture.adapter.persistence"))
                .adapter("rest", absolute("onionarchitecture.adapter.rest"));
        JavaClasses classes = new ClassFileImporter().importPackages(getClass().getPackage().getName() + ".testclasses.onionarchitecture");

        EvaluationResult result = architecture.evaluate(classes);

        ImmutableSet<String> expectedRegexes = ImmutableSet.of(
                expectedAccessViolationPattern(DomainModelLayerClass.class, "call", DomainServiceLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainModelLayerClass.class, "call", ApplicationLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainModelLayerClass.class, "call", CliAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainModelLayerClass.class, "call", PersistenceAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainModelLayerClass.class, "call", RestAdapterLayerClass.class, "callMe"),
                fieldTypePattern(DomainModelLayerClass.class, "domainServiceLayerClass", DomainServiceLayerClass.class),
                fieldTypePattern(DomainModelLayerClass.class, "applicationLayerClass", ApplicationLayerClass.class),
                fieldTypePattern(DomainModelLayerClass.class, "cliAdapterLayerClass", CliAdapterLayerClass.class),
                fieldTypePattern(DomainModelLayerClass.class, "persistenceAdapterLayerClass", PersistenceAdapterLayerClass.class),
                fieldTypePattern(DomainModelLayerClass.class, "restAdapterLayerClass", RestAdapterLayerClass.class),

                expectedAccessViolationPattern(DomainServiceLayerClass.class, "call", ApplicationLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainServiceLayerClass.class, "call", CliAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainServiceLayerClass.class, "call", PersistenceAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(DomainServiceLayerClass.class, "call", RestAdapterLayerClass.class, "callMe"),
                fieldTypePattern(DomainServiceLayerClass.class, "applicationLayerClass", ApplicationLayerClass.class),
                fieldTypePattern(DomainServiceLayerClass.class, "cliAdapterLayerClass", CliAdapterLayerClass.class),
                fieldTypePattern(DomainServiceLayerClass.class, "persistenceAdapterLayerClass", PersistenceAdapterLayerClass.class),
                fieldTypePattern(DomainServiceLayerClass.class, "restAdapterLayerClass", RestAdapterLayerClass.class),

                expectedAccessViolationPattern(ApplicationLayerClass.class, "call", CliAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(ApplicationLayerClass.class, "call", PersistenceAdapterLayerClass.class, "callMe"),
                expectedAccessViolationPattern(ApplicationLayerClass.class, "call", RestAdapterLayerClass.class, "callMe"),
                fieldTypePattern(ApplicationLayerClass.class, "cliAdapterLayerClass", ApplicationLayerClass.class),
                fieldTypePattern(ApplicationLayerClass.class, "persistenceAdapterLayerClass", ApplicationLayerClass.class),
                fieldTypePattern(ApplicationLayerClass.class, "restAdapterLayerClass", ApplicationLayerClass.class),

                expectedAccessViolationPattern(CliAdapterLayerClass.class, "call", PersistenceAdapterLayerClass.class, "callMe"),
                fieldTypePattern(CliAdapterLayerClass.class, "persistenceAdapterLayerClass", PersistenceAdapterLayerClass.class),
                expectedAccessViolationPattern(CliAdapterLayerClass.class, "call", RestAdapterLayerClass.class, "callMe"),
                fieldTypePattern(CliAdapterLayerClass.class, "restAdapterLayerClass", RestAdapterLayerClass.class),

                expectedAccessViolationPattern(PersistenceAdapterLayerClass.class, "call", CliAdapterLayerClass.class, "callMe"),
                fieldTypePattern(PersistenceAdapterLayerClass.class, "cliAdapterLayerClass", CliAdapterLayerClass.class),
                expectedAccessViolationPattern(PersistenceAdapterLayerClass.class, "call", RestAdapterLayerClass.class, "callMe"),
                fieldTypePattern(PersistenceAdapterLayerClass.class, "restAdapterLayerClass", RestAdapterLayerClass.class),

                expectedAccessViolationPattern(RestAdapterLayerClass.class, "call", CliAdapterLayerClass.class, "callMe"),
                fieldTypePattern(RestAdapterLayerClass.class, "cliAdapterLayerClass", CliAdapterLayerClass.class),
                expectedAccessViolationPattern(RestAdapterLayerClass.class, "call", PersistenceAdapterLayerClass.class, "callMe"),
                fieldTypePattern(RestAdapterLayerClass.class, "persistenceAdapterLayerClass", PersistenceAdapterLayerClass.class)
        );
        assertPatternMatches(result.getFailureReport().getDetails(), expectedRegexes);
        assertThat(result.getFailureReport().getDetails().size()).isEqualTo(expectedRegexes.size());
    }


    private String singleLine(EvaluationResult result) {
        return Joiner.on(NEW_LINE_REPLACE).join(result.getFailureReport().getDetails()).replace("\n", NEW_LINE_REPLACE);
    }

    private void assertPatternMatches(List<String> input, Set<String> expectedRegexes) {
        Set<String> toMatch = new HashSet<>(expectedRegexes);
        for (String line : input) {
            if (!matchIteratorAndRemove(toMatch, line)) {
                Assert.fail("Line " + line + " didn't match any pattern in " + expectedRegexes);
            }
        }
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

    private String expectedAccessViolationPattern(Class<?> from, String fromMethod, Class<?> to, String toMethod) {
        return String.format(".*%s.%s().*%s.%s().*", quote(from.getName()), fromMethod, quote(to.getName()), toMethod);
    }

    private String fieldTypePattern(Class<?> owner, String fieldName, Class<?> fieldType) {
        return String.format("Field .*%s\\.%s.* has type .*%s.*", owner.getSimpleName(), fieldName, fieldType.getSimpleName());
    }

    private static String[] absolute(String... pkgSuffix) {
        List<String> result = new ArrayList<>();
        for (String s : pkgSuffix) {
            String absolute = ArchitecturesTest.class.getPackage().getName() + ".testclasses." + s;
            result.add(absolute.replaceAll("\\.\\.\\.+", ".."));
        }
        return result.toArray(new String[result.size()]);
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
}
