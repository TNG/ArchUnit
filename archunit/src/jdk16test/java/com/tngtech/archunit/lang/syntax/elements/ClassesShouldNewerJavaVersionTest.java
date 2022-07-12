package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testobjects.SomeRecord;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.locationPattern;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.singleLineFailureReportOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
public class ClassesShouldNewerJavaVersionTest {

    @DataProvider
    public static Object[][] data_beRecords() {
        return $$(
                $(classes().should().beRecords(), SomeRecord.class, String.class),
                $(classes().should(ArchConditions.beRecords()), SomeRecord.class, String.class));
    }

    @Test
    @UseDataProvider
    public void test_beRecords(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be records")
                .containsPattern(String.format("Class <%s> is no record in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* record.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] data_notBeRecords() {
        return $$(
                $(classes().should().notBeRecords(), String.class, SomeRecord.class),
                $(classes().should(ArchConditions.notBeRecords()), String.class, SomeRecord.class));
    }

    @Test
    @UseDataProvider
    public void test_notBeRecords(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be records")
                .containsPattern(String.format("Class <%s> is a record in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* record.*", quote(satisfied.getName())));
    }
}
