package com.tngtech.archunit.lang.syntax.elements;

import java.util.stream.Stream;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testobjects.SomeRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.locationPattern;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.singleLineFailureReportOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static java.util.regex.Pattern.quote;

public class ClassesShouldNewerJavaVersionTest {

    static Stream<Arguments> be_records() {
        return Stream.of(
                $(classes().should().beRecords(), SomeRecord.class, String.class),
                $(classes().should(ArchConditions.beRecords()), SomeRecord.class, String.class));
    }

    @ParameterizedTest
    @MethodSource
    void be_records(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be records")
                .containsPattern(String.format("Class <%s> is no record in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* record.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> not_be_records() {
        return Stream.of(
                $(classes().should().notBeRecords(), String.class, SomeRecord.class),
                $(classes().should(ArchConditions.notBeRecords()), String.class, SomeRecord.class));
    }

    @ParameterizedTest
    @MethodSource
    void not_be_records(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be records")
                .containsPattern(String.format("Class <%s> is a record in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* record.*", quote(satisfied.getName())));
    }
}
