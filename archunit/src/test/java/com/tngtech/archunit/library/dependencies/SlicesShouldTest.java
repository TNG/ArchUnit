package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyClass;
import com.tngtech.archunit.library.testclasses.second.any.pkg.SecondAnyClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.SomePkgClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;
import org.junit.Assert;
import org.junit.Test;

import static com.tngtech.archunit.library.dependencies.GivenSlicesTest.TEST_CLASSES_PACKAGE;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

public class SlicesShouldTest {
    @Test
    public void ignore_dependency_where_slices_shouldnt_depend_on_each_other() {
        JavaClasses classes = new ClassFileImporter().importPackages(TEST_CLASSES_PACKAGE);

        SliceRule rule = slices().matching(TEST_CLASSES_PACKAGE + ".(*)..").should().notDependOnEachOther();
        assertViolations(classes, rule)
                .contain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .contain(FirstAnyPkgClass.class, SomePkgSubClass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(DescribedPredicate.<JavaClass>alwaysTrue(), classIn(".*\\.second\\..*"));
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .contain(FirstAnyPkgClass.class, SomePkgSubClass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(FirstAnyPkgClass.class.getName(), SomePkgSubClass.class.getName());
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .doNotContain(FirstAnyPkgClass.class, SomePkgSubClass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(SecondAnyClass.class, FirstAnyPkgClass.class);
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .doNotContain(FirstAnyPkgClass.class, SomePkgSubClass.class)
                .doNotContain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);
    }

    @Test
    public void chaining_messages() {
        SliceRule rule = slices().matching("foo.(*)..").should().notDependOnEachOther();

        String baseMessage = "slices matching 'foo.(*)..' should not depend on each other";
        assertThat(rule.getDescription()).isEqualTo(baseMessage);

        rule = rule.because("reason one");
        assertThat(rule.getDescription()).isEqualTo(baseMessage + ", because reason one");

        rule = rule.because("reason two");
        assertThat(rule.getDescription()).isEqualTo(baseMessage + ", because reason one, because reason two");

        rule = rule.as("overridden");
        assertThat(rule.getDescription()).isEqualTo("overridden");

        rule = rule.because("new reason");
        assertThat(rule.getDescription()).isEqualTo("overridden, because new reason");
    }

    private ViolationsAssertion assertViolations(JavaClasses classes, SliceRule rule) {
        return new ViolationsAssertion(rule.evaluate(classes));
    }

    private DescribedPredicate<JavaClass> classIn(final String packageRegex) {
        return new DescribedPredicate<JavaClass>("class in " + packageRegex) {
            @Override
            public boolean apply(JavaClass input) {
                return input.getName().matches(packageRegex);
            }
        };
    }

    private static class ViolationsAssertion {
        private final List<String> failureMessages;

        ViolationsAssertion(EvaluationResult result) {
            failureMessages = result.getFailureReport().getDetails();
        }

        ViolationsAssertion contain(Class<?> from, Class<?> to) {
            if (!containsMessage(from, to)) {
                Assert.fail(String.format("Expected a message to report a dependency from %s to %s", from.getName(), to.getName()));
            }
            return this;
        }

        ViolationsAssertion doNotContain(Class<?> from, Class<?> to) {
            if (containsMessage(from, to)) {
                Assert.fail(String.format("Expected no message to reports a dependency from %s to %s", from.getName(), to.getName()));
            }
            return this;
        }

        private boolean containsMessage(Class<?> from, Class<?> to) {
            Pattern pattern = Pattern.compile(String.format("%s.*%s", quote(from.getName()), quote(to.getName())));
            for (String message : failureMessages) {
                if (pattern.matcher(message).find()) {
                    return true;
                }
            }
            return false;
        }
    }
}
