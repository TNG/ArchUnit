package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.OpenArchRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.DescribedPredicate.all;
import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.IgnoredArchTest.RULE_ONE_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.IgnoredArchTest.RULE_TWO_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.SomeArchTest.FAILING_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.SomeArchTest.IGNORED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.SomeArchTest.SATISFIED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.WrongArchTest.NO_RULE_AT_ALL_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerTest.WrongArchTest.WRONG_MODIFIER_FIELD_NAME;
import static com.tngtech.archunit.lang.ArchRule.rule;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SharedCache cache;

    @Mock
    private RunNotifier runNotifier;

    @Captor
    private ArgumentCaptor<Description> descriptionCaptor;

    @Captor
    private ArgumentCaptor<Failure> failureCaptor;

    @InjectMocks
    private ArchUnitRunner runner = newRunner();

    @Before
    public void setUp() {
        ClassCache classCache = mock(ClassCache.class);
        when(cache.get()).thenReturn(classCache);
        JavaClasses classes = javaClasses(ArchUnitRunnerTest.class);
        when(classCache.getClassesToAnalyseFor(any(Class.class))).thenReturn(classes);
    }

    @Test
    public void should_find_children() throws Exception {
        assertThat(runner.getChildren()).as("Rules defined in Test Class").hasSize(3);
    }

    @Test
    public void should_start_rule() {
        ArchRuleToTest satisfiedRule = getRuleForField(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestStarted(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_accept_satisfied_rule() {
        ArchRuleToTest satisfiedRule = getRuleForField(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_fail_on_wrong_field_visibility() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(WrongArchTest.class);

        runner.runChild(getRuleForField(WRONG_MODIFIER_FIELD_NAME, runner), runNotifier);

        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getMessage()).contains("access");
        assertThat(failureCaptor.getValue().getMessage()).contains("field");
        assertThat(failureCaptor.getValue().getMessage()).contains(WRONG_MODIFIER_FIELD_NAME);
    }

    @Test
    public void should_fail_on_wrong_field_type() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(WrongArchTest.class);

        runner.runChild(getRuleForField(NO_RULE_AT_ALL_FIELD_NAME, runner), runNotifier);

        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getMessage()).contains("type");
        assertThat(failureCaptor.getValue().getMessage()).contains(OpenArchRule.class.getSimpleName());
        assertThat(failureCaptor.getValue().getMessage()).contains("@" + ArchTest.class.getName());
        assertThat(failureCaptor.getValue().getMessage()).contains(NO_RULE_AT_ALL_FIELD_NAME);
    }

    @Test
    public void should_fail_unsatisfied_rule() {
        ArchRuleToTest satisfiedRule = getRuleForField(FAILING_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        Failure failure = failureCaptor.getValue();
        assertThat(failure.getDescription().toString()).contains(FAILING_FIELD_NAME);
        assertThat(failure.getException()).isInstanceOf(AssertionError.class);
    }

    @Test
    public void should_skip_ignored_rule() {
        ArchRuleToTest satisfiedRule = getRuleForField(IGNORED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(IGNORED_FIELD_NAME);
    }

    @Test
    public void should_skip_ignored_test() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(IgnoredArchTest.class);

        runner.runChild(getRuleForField(RULE_ONE_IN_IGNORED_TEST, runner), runNotifier);
        runner.runChild(getRuleForField(RULE_TWO_IN_IGNORED_TEST, runner), runNotifier);

        verify(runNotifier, times(2)).fireTestIgnored(descriptionCaptor.capture());

        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(RULE_ONE_IN_IGNORED_TEST, RULE_TWO_IN_IGNORED_TEST);
    }

    private ArchRuleToTest getRuleForField(String fieldName) {
        return getRuleForField(fieldName, runner);
    }

    private ArchRuleToTest getRuleForField(String fieldName, ArchUnitRunner runner) {
        for (ArchRuleToTest ruleToTest : runner.getChildren()) {
            if (fieldName.equals(ruleToTest.getField().getName())) {
                return ruleToTest;
            }
        }
        throw new RuntimeException(String.format("Couldn't find Rule for field name '%s'", fieldName));
    }

    private ArchUnitRunner newRunner() {
        try {
            return new ArchUnitRunner(SomeArchTest.class);
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class SomeArchTest {
        public static final String SATISFIED_FIELD_NAME = "someSatisfiedRule";
        public static final String FAILING_FIELD_NAME = "someFailingRule";
        public static final String IGNORED_FIELD_NAME = "someIgnoredRule";

        @ArchTest
        public static final ArchRule<JavaClass> someSatisfiedRule =
                rule(all(JavaClass.class)).should("satisfy something").assertedBy(ALWAYS_SATISFIED);

        @ArchTest
        public static final ArchRule<JavaClass> someFailingRule =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ALWAYS_SATISFIED));

        @ArchIgnore
        @ArchTest
        public static final ArchRule<JavaClass> someIgnoredRule =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ALWAYS_SATISFIED));
    }

    private static final ArchCondition<JavaClass> ALWAYS_SATISFIED = new ArchCondition<JavaClass>() {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            events.add(new ConditionEvent(true, "I'm always satisfied"));
        }
    };

    @AnalyseClasses(packages = "some.pkg")
    public static class WrongArchTest {
        public static final String WRONG_MODIFIER_FIELD_NAME = "ruleWithWrongModifier";
        public static final String NO_RULE_AT_ALL_FIELD_NAME = "noRuleAtAll";

        @ArchTest
        private ArchRule<JavaClass> ruleWithWrongModifier =
                rule(all(JavaClass.class)).should("satisfy something").assertedBy(ALWAYS_SATISFIED);

        @ArchTest
        public static Object noRuleAtAll = new Object();
    }

    @ArchIgnore
    @AnalyseClasses(packages = "some.pkg")
    public static class IgnoredArchTest {
        public static final String RULE_ONE_IN_IGNORED_TEST = "someRuleOne";
        public static final String RULE_TWO_IN_IGNORED_TEST = "someRuleTwo";

        @ArchTest
        public static final ArchRule<JavaClass> someRuleOne =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ALWAYS_SATISFIED));

        @ArchTest
        public static final ArchRule<JavaClass> someRuleTwo =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ALWAYS_SATISFIED));
    }
}