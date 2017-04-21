package com.tngtech.archunit.lang.syntax.elements;

import java.util.Comparator;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldConjunctionTest {
    @DataProvider
    public static Object[][] ORed_conditions() {
        return $$(
                $(classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .orShould(ArchConditions.haveFullyQualifiedName(RightTwo.class.getName()))),
                $(classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .orShould().haveFullyQualifiedName(RightTwo.class.getName())));
    }

    @Test
    @UseDataProvider("ORed_conditions")
    public void orShould_ORs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should have fully qualified name '%s' or should have fully qualified name '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("class %s doesn't have fully qualified name '%s' and class %s doesn't have fully qualified name '%s'",
                        Wrong.class.getName(), RightOne.class.getName(),
                        Wrong.class.getName(), RightTwo.class.getName()));
    }

    @DataProvider
    public static Object[][] ORed_conditions_that() {
        return $$(
                $(noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .orShould(ArchConditions.haveFullyQualifiedName(Wrong.class.getName()))),
                $(noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .orShould().haveFullyQualifiedName(Wrong.class.getName())));
    }

    @Test
    @UseDataProvider("ORed_conditions_that")
    public void orShould_ORs_conditions_that(ArchRule rule) {
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

    @DataProvider
    public static Object[][] ANDed_conditions() {
        return $$(
                $(classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .andShould(ArchConditions.haveFullyQualifiedName(RightTwo.class.getName()))),
                $(classes()
                        .should().haveFullyQualifiedName(RightOne.class.getName())
                        .andShould().haveFullyQualifiedName(RightTwo.class.getName())));
    }

    @Test
    @UseDataProvider("ANDed_conditions")
    public void andShould_ANDs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should have fully qualified name '%s' and should have fully qualified name '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("class %s doesn't have fully qualified name '%s'",
                        RightTwo.class.getName(), RightOne.class.getName()),
                String.format("class %s doesn't have fully qualified name '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()),
                String.format("class %s doesn't have fully qualified name '%s'",
                        Wrong.class.getName(), RightOne.class.getName()),
                String.format("class %s doesn't have fully qualified name '%s'",
                        Wrong.class.getName(), RightTwo.class.getName()));
    }

    @DataProvider
    public static Object[][] ANDed_conditions_that() {
        return $$(
                $(noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .andShould(ArchConditions.haveFullyQualifiedName(OtherWrong.class.getName()))),
                $(noClasses()
                        .should().accessClassesThat().haveFullyQualifiedName(Wrong.class.getName())
                        .andShould().haveFullyQualifiedName(OtherWrong.class.getName())));
    }

    @Test
    @UseDataProvider("ANDed_conditions_that")
    public void andShould_ANDs_conditions_that(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class, OtherWrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "no classes should access classes that have fully qualified name '%s' and should have fully qualified name '%s'",
                        Wrong.class.getName(), OtherWrong.class.getName()));
        assertThat(report.getDetails()).usingElementComparator(matchesRegex())
                .containsOnly(otherWrongCallsWrongRegex() + " and " + classHasFullNameRegex(OtherWrong.class));
    }

    private String classHasFullNameRegex(Class<?> clazz) {
        return String.format("class %s has fully qualified name '%s'",
                quote(clazz.getName()), quote(clazz.getName()));
    }

    private String otherWrongCallsWrongRegex() {
        return String.format("Method <%s.call\\(\\)> calls constructor <%s.<init>\\(.*\\)>.*",
                quote(OtherWrong.class.getName()), quote(Wrong.class.getName()));
    }

    private Comparator<String> matchesRegex() {
        return new Comparator<String>() {
            @Override
            public int compare(String element, String assertAgainst) {
                return element.matches(assertAgainst) ? 0 : element.compareTo(assertAgainst);
            }
        };
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