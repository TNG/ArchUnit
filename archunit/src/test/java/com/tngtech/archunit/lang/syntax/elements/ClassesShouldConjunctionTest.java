package com.tngtech.archunit.lang.syntax.elements;

import java.util.Comparator;
import java.util.stream.Stream;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.fullyQualifiedName;
import static com.tngtech.archunit.lang.conditions.ArchConditions.haveFullyQualifiedName;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.locationPattern;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassesShouldConjunctionTest {
    static Stream<ArchRule> ORed_conditions() {
        return Stream.of(
                classes()
                        .should(haveFullyQualifiedName(RightOne.class.getName()))
                        .orShould(haveFullyQualifiedName(RightTwo.class.getName())),
                classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .orShould().haveFullyQualifiedName(RightTwo.class.getName()));
    }

    @ParameterizedTest
    @MethodSource("ORed_conditions")
    void orShould_ORs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should have fully qualified name '%s' or should have fully qualified name '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("%s and %s",
                        doesntHaveFqnMessage(Wrong.class, RightOne.class),
                        doesntHaveFqnMessage(Wrong.class, RightTwo.class)));
    }

    static Stream<ArchRule> ORed_conditions_that() {
        return Stream.of(
                noClasses()
                        .should(accessClassesThat(have(fullyQualifiedName(Wrong.class.getName()))))
                        .orShould(haveFullyQualifiedName(Wrong.class.getName())),
                noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .orShould().haveFullyQualifiedName(Wrong.class.getName()));
    }

    @ParameterizedTest
    @MethodSource("ORed_conditions_that")
    void orShould_ORs_conditions_that(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class, OtherWrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "no classes should access classes that have fully qualified name '%s' or should have fully qualified name '%s'",
                        Wrong.class.getName(), Wrong.class.getName()));
        assertThat(report.getDetails()).usingElementComparator(matchesRegex()).contains(
                classHasFullNameRegex(Wrong.class),
                otherWrongCallsWrongRegex());
    }

    static Stream<ArchRule> ANDed_conditions() {
        return Stream.of(
                classes()
                        .should(haveFullyQualifiedName(RightOne.class.getName()))
                        .andShould(haveFullyQualifiedName(RightTwo.class.getName())),
                classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .andShould().haveFullyQualifiedName(RightTwo.class.getName()));
    }

    @ParameterizedTest
    @MethodSource("ANDed_conditions")
    void andShould_ANDs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should have fully qualified name '%s' and should have fully qualified name '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                doesntHaveFqnMessage(RightTwo.class, RightOne.class),
                doesntHaveFqnMessage(RightOne.class, RightTwo.class),
                doesntHaveFqnMessage(Wrong.class, RightOne.class),
                doesntHaveFqnMessage(Wrong.class, RightTwo.class));
    }

    private String doesntHaveFqnMessage(Class<?> clazz, Class<?> expectedFqn) {
        return String.format("Class <%s> does not have fully qualified name '%s' in (%s.java:0)",
                clazz.getName(), expectedFqn.getName(), locationOf(clazz).getSimpleName());
    }

    private Class<?> locationOf(Class<?> clazz) {
        return clazz.getEnclosingClass() != null ? clazz.getEnclosingClass() : clazz;
    }

    static Stream<ArchRule> ANDed_conditions_that() {
        return Stream.of(
                noClasses()
                        .should(accessClassesThat(have(fullyQualifiedName(Wrong.class.getName()))))
                        .andShould(haveFullyQualifiedName(OtherWrong.class.getName())),
                noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .andShould().haveFullyQualifiedName(OtherWrong.class.getName()));
    }

    @ParameterizedTest
    @MethodSource("ANDed_conditions_that")
    void andShould_ANDs_conditions_that(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class, OtherWrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "no classes should access classes that have fully qualified name '%s' and should have fully qualified name '%s'",
                        Wrong.class.getName(), OtherWrong.class.getName()));
        assertThat(report.getDetails()).usingElementComparator(matchesRegex())
                .containsOnly(classHasFullNameRegex(OtherWrong.class) + " and " + otherWrongCallsWrongRegex());
    }

    private String classHasFullNameRegex(Class<?> clazz) {
        return String.format("Class <%s> has fully qualified name '%s' in %s",
                quote(clazz.getName()), quote(clazz.getName()), locationPattern(locationOf(clazz)));
    }

    private String otherWrongCallsWrongRegex() {
        return String.format("Method <%s.call\\(\\)> calls constructor <%s.<init>\\(.*\\)>.*",
                quote(OtherWrong.class.getName()), quote(Wrong.class.getName()));
    }

    private Comparator<String> matchesRegex() {
        return (element, assertAgainst) -> element.matches(assertAgainst) ? 0 : element.compareTo(assertAgainst);
    }

    private static class RightOne {
    }

    private static class RightTwo {
    }

    private static class Wrong {
    }

    private static class OtherWrong {
        void call() {
            new Wrong();
        }
    }
}
