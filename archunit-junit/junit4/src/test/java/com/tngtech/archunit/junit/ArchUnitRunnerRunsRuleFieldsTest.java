package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.ArchTestWithPrivateInstanceField.PRIVATE_RULE_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.IgnoredArchTest.RULE_ONE_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.IgnoredArchTest.RULE_TWO_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.FAILING_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.IGNORED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.SATISFIED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.WrongArchTestWrongFieldType.NO_RULE_AT_ALL_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.BE_SATISFIED;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.NEVER_BE_SATISFIED;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.newRunnerFor;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsRuleFieldsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

    @Captor
    private ArgumentCaptor<Failure> failureCaptor;

    @InjectMocks
    private ArchUnitRunner runner = ArchUnitRunnerTestUtils.newRunnerFor(SomeArchTest.class);

    private JavaClasses cachedClasses = importClassesWithContext(Object.class);

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyzeFor(any(Class.class), any(ClassAnalysisRequest.class))).thenReturn(cachedClasses);
    }

    @Test
    public void should_find_children() {
        assertThat(runner.getChildren()).as("Rules defined in Test Class").hasSize(3);
    }

    @Test
    public void should_start_rule() {
        ArchTestExecution satisfiedRule = getRule(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestStarted(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_accept_satisfied_rule() {
        ArchTestExecution satisfiedRule = getRule(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verifyTestFinishedSuccessfully(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_allow_instance_fields_of_all_visibility() {
        ArchUnitRunner runner = newRunnerFor(ArchTestWithPrivateInstanceField.class, cache);

        runner.runChild(ArchUnitRunnerTestUtils.getRule(PRIVATE_RULE_FIELD_NAME, runner), runNotifier);

        verifyTestFinishedSuccessfully(PRIVATE_RULE_FIELD_NAME);
    }

    @Test
    public void should_allow_instance_field_in_abstract_base_class() {
        ArchUnitRunner runner = newRunnerFor(ArchTestWithAbstractBaseClass.class, cache);

        runner.runChild(ArchUnitRunnerTestUtils.getRule(AbstractBaseClass.INSTANCE_FIELD_NAME, runner), runNotifier);

        verifyTestFinishedSuccessfully(AbstractBaseClass.INSTANCE_FIELD_NAME);
    }

    @Test
    public void should_fail_on_wrong_field_type() {
        ArchUnitRunner runner = newRunnerFor(WrongArchTestWrongFieldType.class, cache);

        thrown.expectMessage("Rule field " +
                WrongArchTestWrongFieldType.class.getSimpleName() + "." + NO_RULE_AT_ALL_FIELD_NAME +
                " to check must be of type " + ArchRule.class.getSimpleName());

        runner.runChild(ArchUnitRunnerTestUtils.getRule(NO_RULE_AT_ALL_FIELD_NAME, runner), runNotifier);
    }

    @Test
    public void should_fail_unsatisfied_rule() {
        ArchTestExecution satisfiedRule = getRule(FAILING_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        Failure failure = failureCaptor.getValue();
        assertThat(failure.getDescription().toString()).contains(FAILING_FIELD_NAME);
        assertThat(failure.getException()).isInstanceOf(AssertionError.class);
    }

    @Test
    public void should_skip_ignored_rule() {
        ArchTestExecution satisfiedRule = getRule(IGNORED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(IGNORED_FIELD_NAME);
    }

    @Test
    public void should_skip_ignored_test() {
        ArchUnitRunner runner = newRunnerFor(IgnoredArchTest.class, cache);

        runner.runChild(ArchUnitRunnerTestUtils.getRule(RULE_ONE_IN_IGNORED_TEST, runner), runNotifier);
        runner.runChild(ArchUnitRunnerTestUtils.getRule(RULE_TWO_IN_IGNORED_TEST, runner), runNotifier);

        verify(runNotifier, times(2)).fireTestIgnored(descriptionCaptor.capture());

        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(RULE_ONE_IN_IGNORED_TEST, RULE_TWO_IN_IGNORED_TEST);
    }

    private ArchTestExecution getRule(String name) {
        return ArchUnitRunnerTestUtils.getRule(name, runner);
    }

    private void verifyTestFinishedSuccessfully(String expectedDescriptionMethodName) {
        verifyTestFinishedSuccessfully(runNotifier, descriptionCaptor, expectedDescriptionMethodName);
    }

    static void verifyTestFinishedSuccessfully(RunNotifier runNotifier, ArgumentCaptor<Description> descriptionCaptor,
            String expectedDescriptionMethodName) {
        verify(runNotifier, never()).fireTestFailure(any(Failure.class));
        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        Description description = descriptionCaptor.getValue();
        assertThat(description.getMethodName()).isEqualTo(expectedDescriptionMethodName);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class SomeArchTest {
        static final String SATISFIED_FIELD_NAME = "someSatisfiedRule";
        static final String FAILING_FIELD_NAME = "someFailingRule";
        static final String IGNORED_FIELD_NAME = "someIgnoredRule";

        @ArchTest
        public static final ArchRule someSatisfiedRule = classes().should(BE_SATISFIED);

        @ArchTest
        public static final ArchRule someFailingRule = classes().should(NEVER_BE_SATISFIED);

        @ArchIgnore
        @ArchTest
        public static final ArchRule someIgnoredRule = classes().should(NEVER_BE_SATISFIED);
    }

    @SuppressWarnings("WeakerAccess")
    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithPrivateInstanceField {
        static final String PRIVATE_RULE_FIELD_NAME = "privateField";

        @ArchTest
        private ArchRule privateField = classes().should(BE_SATISFIED);
    }

    @SuppressWarnings("WeakerAccess")
    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithAbstractBaseClass extends AbstractBaseClass {
    }

    abstract static class AbstractBaseClass {
        static final String INSTANCE_FIELD_NAME = "abstractBaseClassInstanceField";

        @ArchTest
        ArchRule abstractBaseClassInstanceField = classes().should(BE_SATISFIED);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class WrongArchTestWrongFieldType {
        static final String NO_RULE_AT_ALL_FIELD_NAME = "noRuleAtAll";

        @ArchTest
        public static Object noRuleAtAll = new Object();
    }

    @ArchIgnore
    @AnalyzeClasses(packages = "some.pkg")
    public static class IgnoredArchTest {
        static final String RULE_ONE_IN_IGNORED_TEST = "someRuleOne";
        static final String RULE_TWO_IN_IGNORED_TEST = "someRuleTwo";

        @ArchTest
        public static final ArchRule someRuleOne = classes().should(NEVER_BE_SATISFIED);

        @ArchTest
        public static final ArchRule someRuleTwo = classes().should(NEVER_BE_SATISFIED);
    }
}