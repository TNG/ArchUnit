package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldTest {
    @DataProvider
    public static Object[][] beNamed_rules() {
        return $$(
                $(classes().should().beNamed(RightClass.class.getName())),
                $(classes().should(ArchConditions.beNamed(RightClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("beNamed_rules")
    public void beNamed(ArchRule rule) {
        EvaluationResult result = rule.evaluate(javaClassesViaReflection(RightClass.class, WrongClass.class));

        assertThat(result.getFailureReport().toString())
                .contains(String.format("classes should be named '%s'", RightClass.class.getName()))
                .contains(String.format("%s is not named '%s'", WrongClass.class.getName(), RightClass.class.getName()))
                .doesNotContain(String.format("%s is", RightClass.class.getName()));
    }

    private static class RightClass {
    }

    private static class WrongClass {
    }
}
