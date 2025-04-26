package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubclass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.library.dependencies.GivenSlicesTest.TEST_CLASSES_PACKAGE;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SlicesShouldTest {
    static Stream<SliceRule> rules() {
        return Stream.of(
                slices().matching(TEST_CLASSES_PACKAGE + ".(*)..").should().notDependOnEachOther(),
                slices().matching(TEST_CLASSES_PACKAGE + ".(*)..").should().beFreeOfCycles());
    }

    @ParameterizedTest
    @MethodSource("rules")
    void slice_rules_should_ignore_configured_dependencies(SliceRule rule) {
        JavaClasses classes = new ClassFileImporter().importPackages(TEST_CLASSES_PACKAGE);

        assertViolations(classes, rule)
                .contain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .contain(FirstAnyPkgClass.class, SomePkgSubclass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(classIn(".*\\.first\\.three\\..*"), alwaysTrue());
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .contain(FirstAnyPkgClass.class, SomePkgSubclass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(FirstAnyPkgClass.class.getName(), SomePkgSubclass.class.getName());
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .doNotContain(FirstAnyPkgClass.class, SomePkgSubclass.class)
                .contain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(SecondAnyClass.class, FirstAnyPkgClass.class);
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .doNotContain(FirstAnyPkgClass.class, SomePkgSubclass.class)
                .doNotContain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .contain(SecondThreeAnyClass.class, SomePkgClass.class);

        rule = rule.ignoreDependency(alwaysTrue(), classIn(".*\\.some\\.pkg\\..*"));
        assertViolations(classes, rule)
                .doNotContain(FirstThreeAnyClass.class, SecondThreeAnyClass.class)
                .doNotContain(FirstAnyPkgClass.class, SomePkgSubclass.class)
                .doNotContain(SecondAnyClass.class, FirstAnyPkgClass.class)
                .doNotContain(SecondThreeAnyClass.class, SomePkgClass.class);
    }

    static Stream<Arguments> rules_with_expected_base_description() {
        return Stream.of(
                $(slices().matching("foo.(*)..").should().notDependOnEachOther(),
                        "slices matching 'foo.(*)..' should not depend on each other"),
                $(slices().matching("foo.(*)..").should().beFreeOfCycles(),
                        "slices matching 'foo.(*)..' should be free of cycles"));
    }

    @ParameterizedTest
    @MethodSource("rules_with_expected_base_description")
    void chaining_messages(SliceRule rule, String baseDescription) {
        assertThat(rule.getDescription()).isEqualTo(baseDescription);

        rule = rule.because("reason one");
        assertThat(rule.getDescription()).isEqualTo(baseDescription + ", because reason one");

        rule = rule.because("reason two");
        assertThat(rule.getDescription()).isEqualTo(baseDescription + ", because reason one, because reason two");

        rule = rule.as("overridden");
        assertThat(rule.getDescription()).isEqualTo("overridden");

        rule = rule.because("new reason");
        assertThat(rule.getDescription()).isEqualTo("overridden, because new reason");
    }

    private ViolationsAssertion assertViolations(JavaClasses classes, SliceRule rule) {
        return new ViolationsAssertion(rule.evaluate(classes));
    }

    private DescribedPredicate<JavaClass> classIn(String packageRegex) {
        return new DescribedPredicate<JavaClass>("class in " + packageRegex) {
            @Override
            public boolean test(JavaClass input) {
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
                fail(String.format("Expected a message to report a dependency from %s to %s", from.getName(), to.getName()));
            }
            return this;
        }

        ViolationsAssertion doNotContain(Class<?> from, Class<?> to) {
            if (containsMessage(from, to)) {
                fail(String.format("Expected no message to reports a dependency from %s to %s", from.getName(), to.getName()));
            }
            return this;
        }

        private boolean containsMessage(Class<?> from, Class<?> to) {
            Pattern pattern = Pattern.compile(String.format("%s.*%s", quote(from.getName()), quote(to.getName())));
            return failureMessages.stream().anyMatch(pattern.asPredicate());
        }
    }
}
