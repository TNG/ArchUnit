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
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyClass;
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
    public void defining_constraint_on_non_existing_layer_is_rejected() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no layer");
        thrown.expectMessage("Other");

        layeredArchitecture()
                .layer("Some").definedBy("any")
                .whereLayer("Other").mayNotBeAccessedByAnyLayer();
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
                        expectedViolationPattern(FirstAnyPkgClass.class, "call", SomePkgSubClass.class, "callMe"),
                        expectedViolationPattern(SecondThreeAnyClass.class, "call", SomePkgClass.class, "callMe"),
                        expectedViolationPattern(FirstThreeAnyClass.class, "call", FirstAnyPkgClass.class, "callMe")));
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

    private String expectedViolationPattern(Class<?> from, String fromMethod, Class<?> to, String toMethod) {
        return String.format(".*%s.%s().*%s.%s().*", quote(from.getName()), fromMethod, quote(to.getName()), toMethod);
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