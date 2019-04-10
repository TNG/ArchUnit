package com.tngtech.archunit.lang.conditions;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.assertion.ConditionEventsAssertion;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaAccessConditionTest {
    @Test
    public void matches_field_access() {
        JavaClass clazz = importToCheck(ClassAccessingField.class);

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysTrue()))
                .containNoViolation();

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysFalse()))
                .haveOneViolationMessageContaining(ClassAccessingField.class.getSimpleName() + ".access()")
                .haveOneViolationMessageContaining(SomeClass.class.getSimpleName() + ".field");
    }

    @Test
    public void matches_constructor_call() {
        JavaClass clazz = importToCheck(ClassCallingConstructor.class);

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysTrue()))
                .containNoViolation();

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysFalse()))
                .haveOneViolationMessageContaining(ClassCallingConstructor.class.getSimpleName() + ".call()")
                .haveOneViolationMessageContaining(SomeClass.class.getSimpleName() + "." + CONSTRUCTOR_NAME);
    }

    @Test
    public void matches_method_call() {
        JavaClass clazz = importToCheck(ClassCallingMethod.class);

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysTrue()));

        assertThatOnlyAccessToSomeClassFor(clazz, new JavaAccessCondition<>(alwaysFalse()))
                .haveOneViolationMessageContaining(ClassCallingMethod.class.getSimpleName() + ".call()")
                .haveOneViolationMessageContaining(SomeClass.class.getSimpleName() + ".method");
    }

    @Test
    public void description_is_correct() {
        JavaAccessCondition<?> condition = new JavaAccessCondition<>(alwaysTrue().as("some description"));

        assertThat(condition.getDescription()).isEqualTo("access target where some description");
    }

    private ConditionEventsAssertion assertThatOnlyAccessToSomeClassFor(JavaClass clazz, JavaAccessCondition<JavaAccess<?>> condition) {
        Set<JavaAccess<?>> accesses = filterByTarget(clazz.getAccessesFromSelf(), SomeClass.class);
        ConditionEvents events = new ConditionEvents();
        for (JavaAccess<?> access : accesses) {
            condition.check(access, events);
        }
        return assertThat(events);
    }

    private <T extends JavaAccess<?>> Set<T> filterByTarget(Set<T> accesses, Class<?> targetOwner) {
        Set<T> result = new HashSet<>();
        for (T access : accesses) {
            if (access.getTargetOwner().isEquivalentTo(targetOwner)) {
                result.add(access);
            }
        }
        return result;
    }

    private JavaClass importToCheck(Class<?> clazz) {
        return importClasses(SomeClass.class, clazz).get(clazz);
    }

    private static class SomeClass {
        String field;

        SomeClass() {
        }

        void method() {
        }
    }

    private static class ClassAccessingField {
        SomeClass someClass;

        void access() {
            someClass.field = "foo";
        }
    }

    private static class ClassCallingConstructor {
        void call() {
            new SomeClass();
        }
    }

    private static class ClassCallingMethod {
        SomeClass someClass;

        void call() {
            someClass.method();
        }
    }
}