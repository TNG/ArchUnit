package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.TestUtils;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassesShouldConjunctionTest {
    @Test
    public void orShould_ORs_conditions() {
        EvaluationResult result = classes()
                .should().beNamed(RightOne.class.getName())
                .orShould(ArchConditions.beNamed(RightTwo.class.getName()))
                .evaluate(TestUtils.javaClassesViaReflection(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should be named '%s' or should be named '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("class %s is not named '%s' and class %s is not named '%s'",
                        Wrong.class.getName(), RightOne.class.getName(),
                        Wrong.class.getName(), RightTwo.class.getName()));

    }

    private static class RightOne {
    }

    private static class RightTwo {
    }

    private static class Wrong {
    }
}