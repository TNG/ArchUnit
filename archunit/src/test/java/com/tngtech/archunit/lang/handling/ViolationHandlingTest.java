package com.tngtech.archunit.lang.handling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.ViolationHandler;
import org.assertj.core.api.Condition;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccesses;
import static com.tngtech.archunit.testutil.Assertions.expectedAccess;

public class ViolationHandlingTest {
    @Test
    public void field_accesses_are_handled() {
        EvaluationResult result = noClasses().should()
                .accessTargetWhere(target(
                        Get.<JavaClass>owner().is(equivalentTo(Target.class))))
                .evaluate(importClasses(Origin.class, Target.class));

        FieldAccessRecorder fieldAccessRecorder = new FieldAccessRecorder();
        result.handleViolations(fieldAccessRecorder);

        assertThat(fieldAccessRecorder.getRecords()).as("number of violations").hasSize(3);
        assertThatAccesses(fieldAccessRecorder.getAccesses()).containOnly(
                accessToTargetField("fieldOne"),
                accessToTargetField("fieldTwo"),
                accessToTargetField("fieldThree"));

    }

    @Test
    public void method_calls_are_handled() {
        EvaluationResult result = noClasses().should().accessClassesThat().haveFullyQualifiedName(Target.class.getName())
                .evaluate(importClasses(Origin.class, Target.class));

        MethodCallRecorder methodCallRecorder = new MethodCallRecorder();
        result.handleViolations(methodCallRecorder);

        assertThatAccesses(methodCallRecorder.getAccesses()).containOnly(
                expectedAccess()
                        .from(Origin.class, "call")
                        .to(Target.class, "callMeOne"),
                expectedAccess()
                        .from(Origin.class, "call")
                        .to(Target.class, "callMeTwo"));
    }

    @Test
    public void constructor_calls_are_handled() {
        EvaluationResult result = noClasses().should().accessClassesThat().haveFullyQualifiedName(Target.class.getName())
                .evaluate(importClasses(Origin.class, Target.class));

        ConstructorCallRecorder constructorCallRecorder = new ConstructorCallRecorder();
        result.handleViolations(constructorCallRecorder);

        assertThatAccesses(constructorCallRecorder.getAccesses()).containOnly(
                expectedAccess()
                        .from(Origin.class, "callConstructor")
                        .toConstructor(Target.class),
                expectedAccess()
                        .from(Origin.class, "callConstructor")
                        .toConstructor(Target.class, String.class));
    }

    @Test
    public void accesses_are_handled() {
        EvaluationResult result = noClasses().should().accessClassesThat().haveFullyQualifiedName(Target.class.getName())
                .evaluate(importClasses(Origin.class, Target.class));

        CallRecorder callRecorder = new CallRecorder();
        result.handleViolations(callRecorder);

        assertThat(callRecorder.getRecords()).as("Recorded calls").hasSize(4);

        AccessRecorder accessRecorder = new AccessRecorder();
        result.handleViolations(accessRecorder);

        assertThat(accessRecorder.getRecords()).as("Recorded accesses").hasSize(7);
    }

    @Test
    public void multiple_accesses_necessary_for_violation_are_reported_together() {
        EvaluationResult result = classes().should().accessField(Target.class, "notExisting")
                .evaluate(importClasses(Origin.class, Target.class));

        FieldAccessRecorder fieldAccessRecorder = new FieldAccessRecorder();
        result.handleViolations(fieldAccessRecorder);

        assertThat(fieldAccessRecorder.getRecords()).as("number of violations").hasSize(1);
        ReportedViolation<JavaFieldAccess> reportedViolation = getOnlyElement(fieldAccessRecorder.getRecords());
        assertThatAccesses(reportedViolation.accesses)
                .contain(accessToTargetField("fieldOne"))
                .contain(accessToTargetField("fieldTwo"))
                .contain(accessToTargetField("fieldThree"));
    }

    private Condition<JavaAccess<?>> accessToTargetField(String fieldOne) {
        return expectedAccess()
                .from(Origin.class, CONSTRUCTOR_NAME)
                .to(Target.class, fieldOne);
    }

    private static class Origin {
        Target target;

        public Origin() {
            target.fieldOne = "changed";
            target.fieldTwo = "changed";
            target.fieldThree = 42;
        }

        void call() {
            target.callMeOne();
            target.callMeTwo();
        }

        void callConstructor() {
            new Target();
            new Target("fieldOne");
        }
    }

    private static class Target {
        String fieldOne;
        Object fieldTwo;
        int fieldThree;

        Target() {
        }

        Target(String fieldOne) {
        }

        void callMeOne() {
        }

        void callMeTwo() {

        }
    }

    private static class ReportedViolation<T extends JavaAccess<?>> {
        final Collection<T> accesses;
        final String message;

        ReportedViolation(Collection<? extends T> accesses, String message) {
            this.accesses = ImmutableList.copyOf(accesses);
            this.message = message;
        }

        @Override
        public String toString() {
            return "ReportedViolation{" +
                    "accesses=" + accesses +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    private static class AbstractAccessRecorder<T extends JavaAccess<?>> implements ViolationHandler<T> {
        private final List<ReportedViolation<T>> reportedViolations = new ArrayList<>();

        @Override
        public void handle(Collection<T> violatingObjects, String message) {
            reportedViolations.add(new ReportedViolation<>(violatingObjects, message));
        }

        List<ReportedViolation<T>> getRecords() {
            return reportedViolations;
        }

        Set<T> getAccesses() {
            Set<T> result = new HashSet<>();
            for (ReportedViolation<T> violation : reportedViolations) {
                result.addAll(violation.accesses);
            }
            return result;
        }
    }

    private static class AccessRecorder extends AbstractAccessRecorder<JavaAccess<?>> {
    }

    private static class CallRecorder extends AbstractAccessRecorder<JavaCall<?>> {
    }

    private static class FieldAccessRecorder extends AbstractAccessRecorder<JavaFieldAccess> {
    }

    private static class MethodCallRecorder extends AbstractAccessRecorder<JavaMethodCall> {
    }

    private static class ConstructorCallRecorder extends AbstractAccessRecorder<JavaConstructorCall> {
    }
}
