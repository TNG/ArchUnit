package com.tngtech.archunit.integration.junit;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.SecondBeanImplementingSomeBusinessInterface;
import com.tngtech.archunit.example.SomeBusinessInterface;
import com.tngtech.archunit.example.SomeOtherBusinessInterface;
import com.tngtech.archunit.exampletest.junit.RestrictNumberOfClassesWithACertainPropertyTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.junit.MessageAssertionChain;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.namesOf;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class RestrictNumberOfClassesWithACertainPropertyIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = RestrictNumberOfClassesWithACertainPropertyIntegrationTest.class, method = "expectViolationFromTooManyClasses")
    public static final ArchRule no_new_classes_should_implement_SomeBusinessInterface =
            RestrictNumberOfClassesWithACertainPropertyTest.no_new_classes_should_implement_SomeBusinessInterface;

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromTooManyClasses(ExpectsViolations expectViolations) {
        expectViolations.ofRule(
                String.format("classes that implement %s should contain number of elements less than or equal to '1', "
                                + "because from now on new classes should implement %s",
                        SomeBusinessInterface.class.getName(), SomeOtherBusinessInterface.class.getName()))
                .by(classesContaining(ClassViolatingSessionBeanRules.class, SecondBeanImplementingSomeBusinessInterface.class));
    }

    private static MessageAssertionChain.Link classesContaining(final Class<?>... classes) {
        final String expectedLine = String.format("there is/are %d element(s) in classes %s", classes.length, namesOf(classes));
        return new MessageAssertionChain.Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                List<String> rest = new ArrayList<>();
                for (String line : lines) {
                    if (!line.equals(expectedLine)) {
                        rest.add(line);
                    }
                }
                boolean matches = (rest.size() == lines.size() - 1);
                return new Result(matches, rest, String.format("No line matched '%s'", expectedLine));
            }

            @Override
            public String getDescription() {
                return "classes containing " + namesOf(classes);
            }
        };
    }
}
