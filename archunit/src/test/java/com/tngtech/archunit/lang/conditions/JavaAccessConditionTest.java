package com.tngtech.archunit.lang.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.testutil.assertion.ConditionEventsAssertion;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.stream.Collectors.toSet;

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
    public void events_are_equivalent_to_SimpleConditionEvents() {
        JavaClass clazz = importToCheck(ClassAccessingField.class);
        JavaAccess<?> access = filterByTarget(clazz.getAccessesFromSelf(), SomeClass.class).iterator().next();

        for (boolean satisfied : new boolean[]{true, false}) {
            ViolatedAndSatisfiedConditionEvents events = new ViolatedAndSatisfiedConditionEvents();
            new JavaAccessCondition<>(satisfied ? alwaysTrue() : alwaysFalse()).check(access, events);
            ConditionEvent actual = (satisfied ? events.getAllowed() : events.getViolating()).iterator().next();
            ConditionEvent expected = new SimpleConditionEvent(access, satisfied, access.getDescription());

            assertEquivalent(actual, expected);
            assertEquivalent(actual.invert(), expected.invert());
        }
    }

    private void assertEquivalent(ConditionEvent actual, ConditionEvent expected) {
        assertThat(actual.isViolation()).isEqualTo(expected.isViolation());
        assertThat(actual.getDescriptionLines()).isEqualTo(expected.getDescriptionLines());
        assertThat(actual.toString()).contains(expected.getDescriptionLines().get(0));
        assertThat(handled(actual)).isEqualTo(handled(expected));
    }

    private List<Object> handled(ConditionEvent event) {
        List<Object> result = new ArrayList<>();
        event.handleWith((correspondingObjects, message) -> {
            result.add(correspondingObjects);
            result.add(message);
        });
        return result;
    }

    @Test
    public void description_is_correct() {
        JavaAccessCondition<?> condition = new JavaAccessCondition<>(alwaysTrue().as("some description"));

        assertThat(condition.getDescription()).isEqualTo("access target where some description");
    }

    private ConditionEventsAssertion assertThatOnlyAccessToSomeClassFor(JavaClass clazz, JavaAccessCondition<JavaAccess<?>> condition) {
        Set<JavaAccess<?>> accesses = filterByTarget(clazz.getAccessesFromSelf(), SomeClass.class);
        ConditionEvents events = ConditionEvents.Factory.create();
        for (JavaAccess<?> access : accesses) {
            condition.check(access, events);
        }
        return assertThat(events);
    }

    private <T extends JavaAccess<?>> Set<T> filterByTarget(Set<T> accesses, Class<?> targetOwner) {
        return accesses.stream().filter(access -> access.getTargetOwner().isEquivalentTo(targetOwner)).collect(toSet());
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
