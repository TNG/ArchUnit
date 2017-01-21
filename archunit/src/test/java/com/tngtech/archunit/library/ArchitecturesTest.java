package com.tngtech.archunit.library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyFirstClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyFirstClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnySecondClass;
import com.tngtech.archunit.library.testclasses.some.pkg.SomeFirstClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomeSecondClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchitecturesTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void description_of_layered_architecture() {
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
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
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
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
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
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
                        expectedViolationPattern(FirstAnyFirstClass.class, "call", SomeSecondClass.class, "callMe"),
                        expectedViolationPattern(SecondThreeAnySecondClass.class, "call", SomeFirstClass.class, "callMe"),
                        expectedViolationPattern(FirstThreeAnyFirstClass.class, "call", FirstAnyFirstClass.class, "callMe")));
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

    private String[] absolute(String... pkgSuffix) {
        List<String> result = new ArrayList<>();
        for (String s : pkgSuffix) {
            String absolute = getClass().getPackage().getName() + ".testclasses." + s;
            result.add(absolute.replaceAll("\\.\\.\\.+", ".."));
        }
        return result.toArray(new String[result.size()]);
    }
}