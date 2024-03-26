package com.tngtech.archunit.junit.internal;

import java.lang.reflect.Method;
import java.util.Collection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.internal.ArchUnitRunnerInternal.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.ArchTestWithRuleLibrary.someOtherMethodRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.FullyIgnoredRules.someFieldRuleInBaseClassOfIgnoredClassName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.FullyIgnoredRules.someFieldRuleInIgnoredClassName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.FullyIgnoredRules.someMethodRuleInBaseClassOfIgnoredClassName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.FullyIgnoredRules.someMethodRuleInIgnoredClassName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.IgnoredSubRules.someIgnoredSubFieldRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.IgnoredSubRules.someIgnoredSubMethodRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.IgnoredSubRules.someNonIgnoredSubFieldRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.IgnoredSubRules.someNonIgnoredSubMethodRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.OtherRules.someOtherFieldRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.PartiallyIgnoredRules.someIgnoredFieldRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.PartiallyIgnoredRules.someIgnoredMethodRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.Rules.someFieldRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerRunsRuleSetsTest.Rules.someMethodRuleName;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerTestUtils.getRule;
import static com.tngtech.archunit.junit.internal.ArchUnitRunnerTestUtils.newRunnerFor;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.testutil.TestUtils.invoke;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsRuleSetsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SharedCache cache;
    @Mock
    private ClassCache classCache;

    @Mock
    private RunNotifier runNotifier;

    @Captor
    private ArgumentCaptor<Description> descriptionCaptor;

    @InjectMocks
    private ArchUnitRunnerInternal runnerForRuleSet = newRunnerFor(ArchTestWithRuleSet.class);

    @InjectMocks
    private ArchUnitRunnerInternal runnerForIgnoredRuleSet = newRunnerFor(ArchTestWithIgnoredRuleSet.class);

    @InjectMocks
    private ArchUnitRunnerInternal runnerForRuleLibrary = newRunnerFor(ArchTestWithRuleLibrary.class);

    @InjectMocks
    private ArchUnitRunnerInternal runnerForIgnoredRuleLibrary = newRunnerFor(ArchTestWithIgnoredRuleLibrary.class);

    @InjectMocks
    private ArchUnitRunnerInternal runnerForFullyIgnoredRuleLibrary = newRunnerFor(ArchTestWithFullyIgnoredRuleLibrary.class);

    private final JavaClasses cachedClasses = importClassesWithContext(ArchUnitRunnerRunsRuleSetsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyzeFor(any(Class.class), any(ClassAnalysisRequest.class))).thenReturn(cachedClasses);
    }

    @Test
    public void should_find_children_in_rule_set() {
        assertThat(runnerForRuleSet.getChildren()).as("Rules defined in Test Class")
                .hasSize(2)
                .map(ArchTestExecution::describeSelf)
                .containsExactlyInAnyOrder(
                        Description.createTestDescription(
                                ArchTestWithRuleSet.class,
                                Rules.class.getSimpleName() + " > " + someFieldRuleName
                        ),
                        Description.createTestDescription(
                                ArchTestWithRuleSet.class,
                                Rules.class.getSimpleName() + " > " + someMethodRuleName
                        )
                );
    }

    @Test
    public void should_find_children_in_rule_library() {
        assertThat(runnerForRuleLibrary.getChildren()).as("Rules defined in Library")
                .hasSize(3)
                .map(ArchTestExecution::describeSelf)
                .containsExactlyInAnyOrder(
                        Description.createTestDescription(
                                ArchTestWithRuleLibrary.class,
                                ArchTestWithRuleSet.class.getSimpleName() + " > " + Rules.class.getSimpleName() + " > " + someFieldRuleName
                        ),
                        Description.createTestDescription(
                                ArchTestWithRuleLibrary.class,
                                ArchTestWithRuleSet.class.getSimpleName() + " > " + Rules.class.getSimpleName() + " > " + someMethodRuleName
                        ),
                        Description.createTestDescription(
                                ArchTestWithRuleLibrary.class.getName(),
                                someOtherMethodRuleName
                        )
                );
    }

    @Test
    public void can_run_rule_field() {
        run(someFieldRuleName, runnerForRuleSet, verifyTestRan());
    }

    @Test
    public void can_run_rule_method() {
        run(someMethodRuleName, runnerForRuleSet, verifyTestRan());
    }

    @Test
    public void ignores_field_rule_of_ignored_rule_set() {
        run(someFieldRuleName, runnerForIgnoredRuleSet, verifyTestIgnored());
    }

    @Test
    public void ignores_method_rule_of_ignored_rule_set() {
        run(someMethodRuleName, runnerForIgnoredRuleSet, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_field_rule() {
        run(someIgnoredFieldRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_method_rule() {
        run(someIgnoredMethodRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_field_rule_of_ignored_class() {
        run(someFieldRuleInIgnoredClassName, runnerForFullyIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_method_rule_of_ignored_class() {
        run(someMethodRuleInIgnoredClassName, runnerForFullyIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_field_rule_in_base_class_of_ignored_class() {
        run(someFieldRuleInBaseClassOfIgnoredClassName, runnerForFullyIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_method_rule_in_base_class_of_ignored_class() {
        run(someMethodRuleInBaseClassOfIgnoredClassName, runnerForFullyIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_nested_rule_set_in_base_class_of_ignored_class() {
        run(someOtherFieldRuleName, runnerForFullyIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_double_nested_field_rule() {
        run(someIgnoredSubFieldRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_double_nested_method_rule() {
        run(someIgnoredSubMethodRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void runs_double_nested_field_method_rule() {
        run(someNonIgnoredSubFieldRuleName, runnerForIgnoredRuleLibrary, verifyTestRan());
    }

    @Test
    public void runs_double_nested_method_rule() {
        run(someNonIgnoredSubMethodRuleName, runnerForIgnoredRuleLibrary, verifyTestRan());
    }

    @Test
    public void ignores_double_nested_field_rule_in_ignored_rule_set() {
        run(someFieldRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void ignores_double_nested_method_rule_in_ignored_rule_set() {
        run(someMethodRuleName, runnerForIgnoredRuleLibrary, verifyTestIgnored());
    }

    @Test
    public void should_allow_ArchRules_in_class_with_instance_field_in_abstract_base_class() {
        ArchUnitRunnerInternal runner = newRunnerFor(ArchTestWithRulesWithAbstractBaseClass.class, cache);

        runner.runChild(getRule(ArchUnitRunnerRunsRuleFieldsTest.AbstractBaseClass.INSTANCE_FIELD_NAME, runner), runNotifier);

        verifyTestFinishedSuccessfully(ArchUnitRunnerRunsRuleFieldsTest.AbstractBaseClass.INSTANCE_FIELD_NAME);
    }

    @Test
    public void should_allow_ArchRules_in_class_with_instance_method_in_abstract_base_class() {
        ArchUnitRunnerInternal runner = newRunnerFor(ArchTestWithRulesWithAbstractBaseClass.class, cache);

        runner.runChild(getRule(ArchUnitRunnerRunsMethodsTest.AbstractBaseClass.INSTANCE_METHOD_NAME, runner), runNotifier);

        verifyTestFinishedSuccessfully(ArchUnitRunnerRunsMethodsTest.AbstractBaseClass.INSTANCE_METHOD_NAME);
    }

    private void verifyTestFinishedSuccessfully(String expectedDescriptionMethodName) {
        ArchUnitRunnerRunsRuleFieldsTest.verifyTestFinishedSuccessfully(runNotifier, descriptionCaptor, expectedDescriptionMethodName);
    }

    private Runnable verifyTestIgnored() {
        return () -> verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
    }

    private Runnable verifyTestRan() {
        return () -> {
            verify(runNotifier).fireTestStarted(any(Description.class));
            verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        };
    }

    // extractingResultOf(..) only looks for public methods
    private Extractor<Object, Object> resultOf(String methodName) {
        return input -> {
            Collection<Method> candidates = getAllMethods(input.getClass(), method -> method.getName().equals(methodName));
            checkState(!candidates.isEmpty(),
                    "Couldn't find any method named '%s' with hierarchy of %s",
                    methodName, input.getClass().getName());
            return invoke(candidates.iterator().next(), input);
        };
    }

    private void run(String ruleName, ArchUnitRunnerInternal runner, Runnable testVerification) {
        ArchTestExecution rule = getRule(ruleName, runner);

        runner.runChild(rule, runNotifier);

        testVerification.run();
        assertThat(descriptionCaptor.getValue().toString()).contains(ruleName);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithRuleLibrary {
        static final String someOtherMethodRuleName = "someOtherMethodRule";

        @ArchTest
        final ArchTests rules = ArchTests.in(ArchTestWithRuleSet.class);

        @ArchTest
        public static void someOtherMethodRule(JavaClasses classes) {
        }
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithRuleSet {
        @ArchTest
        final ArchTests rules = ArchTests.in(Rules.class);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithIgnoredRuleSet {
        @ArchTest
        @ArchIgnore
        public static final ArchTests rules = ArchTests.in(Rules.class);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithIgnoredRuleLibrary {
        @ArchTest
        public static final ArchTests rules = ArchTests.in(PartiallyIgnoredRules.class);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithFullyIgnoredRuleLibrary {
        @ArchTest
        public static final ArchTests rules = ArchTests.in(FullyIgnoredRules.class);
    }

    public static class Rules {
        static final String someFieldRuleName = "someFieldRule";
        static final String someMethodRuleName = "someMethodRule";

        @ArchTest
        public static final ArchRule someFieldRule = classes().should(satisfySomething());

        @ArchTest
        public static void someMethodRule(JavaClasses classes) {
        }
    }

    public static class PartiallyIgnoredRules {
        static final String someIgnoredFieldRuleName = "someIgnoredFieldRule";
        static final String someIgnoredMethodRuleName = "someIgnoredMethodRule";

        @ArchTest
        @ArchIgnore
        public static final ArchRule someIgnoredFieldRule = classes().should(satisfySomething());

        @ArchTest
        @ArchIgnore
        public static void someIgnoredMethodRule(JavaClasses classes) {
        }

        @ArchTest
        public static final ArchTests subRules = ArchTests.in(IgnoredSubRules.class);

        @ArchTest
        @ArchIgnore
        public static final ArchTests ignoredSubRules = ArchTests.in(Rules.class);
    }

    @ArchIgnore
    public static class FullyIgnoredRules extends BaseClass {
        static final String someFieldRuleInIgnoredClassName = "someFieldRuleInIgnoredClass";
        static final String someMethodRuleInIgnoredClassName = "someMethodRuleInIgnoredClass";
        static final String someFieldRuleInBaseClassOfIgnoredClassName = "someFieldRuleInBaseClass";
        static final String someMethodRuleInBaseClassOfIgnoredClassName = "someMethodRuleInBaseClass";

        @ArchTest
        public static final ArchRule someFieldRuleInIgnoredClass = classes().should(satisfySomething());

        @ArchTest
        public static void someMethodRuleInIgnoredClass(JavaClasses classes) {
        }
    }

    public static class BaseClass {
        @ArchTest
        public static final ArchRule someFieldRuleInBaseClass = classes().should(satisfySomething());

        @ArchTest
        public static void someMethodRuleInBaseClass(JavaClasses classes) {
        }

        @ArchTest
        public static final ArchTests someSubRulesInBaseClass = ArchTests.in(OtherRules.class);
    }

    public static class OtherRules {
        static final String someOtherFieldRuleName = "someOtherFieldRule";

        @ArchTest
        public static final ArchRule someOtherFieldRule = classes().should(satisfySomething());
    }

    public static class IgnoredSubRules {
        static final String someIgnoredSubFieldRuleName = "someIgnoredSubFieldRule";
        static final String someNonIgnoredSubFieldRuleName = "someNonIgnoredSubFieldRule";
        static final String someIgnoredSubMethodRuleName = "someIgnoredSubMethodRule";
        static final String someNonIgnoredSubMethodRuleName = "someNonIgnoredSubMethodRule";

        @ArchTest
        @ArchIgnore
        public static final ArchRule someIgnoredSubFieldRule = classes().should(satisfySomething());

        @ArchTest
        public static final ArchRule someNonIgnoredSubFieldRule = classes().should(satisfySomething());

        @ArchTest
        @ArchIgnore
        public static void someIgnoredSubMethodRule(JavaClasses classes) {
        }

        @ArchTest
        public static void someNonIgnoredSubMethodRule(JavaClasses classes) {
        }
    }

    private static ArchCondition<JavaClass> satisfySomething() {
        return new ArchCondition<JavaClass>("satisfy something") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
            }
        };
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithRulesWithAbstractBaseClass {
        @ArchTest
        ArchTests fieldRules = ArchTests.in(ArchUnitRunnerRunsRuleFieldsTest.ArchTestWithAbstractBaseClass.class);
        @ArchTest
        ArchTests methodRules = ArchTests.in(ArchUnitRunnerRunsMethodsTest.ArchTestWithAbstractBaseClass.class);
    }
}
