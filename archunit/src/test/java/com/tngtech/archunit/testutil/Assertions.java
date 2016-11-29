package com.tngtech.archunit.testutil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaField;
import com.tngtech.archunit.core.Optional;
import com.tngtech.archunit.core.TypeDetails;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.FailureMessages;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.JavaModifier.getModifiersFor;
import static com.tngtech.archunit.core.TestUtils.enumConstant;

public class Assertions extends org.assertj.core.api.Assertions {
    public static ConditionEventsAssert assertThat(ConditionEvents events) {
        return new ConditionEventsAssert(events);
    }

    public static <T> org.assertj.guava.api.OptionalAssert<T> assertThat(Optional<T> optional) {
        return org.assertj.guava.api.Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    public static JavaFieldAssertion assertThat(FieldAccessTarget target) {
        return assertThat(target.getJavaField());
    }

    public static JavaFieldAssertion assertThat(JavaField field) {
        return new JavaFieldAssertion(field);
    }

    public static class JavaFieldAssertion {
        private final JavaField javaField;

        private JavaFieldAssertion(JavaField javaField) {
            this.javaField = javaField;
        }

        public void isEquivalentTo(Field field) {
            assertThat(javaField.getName()).isEqualTo(field.getName());
            assertThat(javaField.getFullName()).isEqualTo(field.getDeclaringClass().getName() + "." + field.getName());
            assertThat(javaField.getOwner().reflect()).isEqualTo(field.getDeclaringClass());
            assertThat(javaField.getType()).isEqualTo(field.getType());
            assertThat(javaField.getModifiers()).isEqualTo(getModifiersFor(field.getModifiers()));
            assertThat(propertiesOf(javaField.getAnnotations())).isEqualTo(propertiesOf(field.getAnnotations()));
        }

        private Set<Map<String, Object>> propertiesOf(Set<JavaAnnotation> annotations) {
            Set<Map<String, Object>> result = new HashSet<>();
            for (JavaAnnotation annotation : annotations) {
                result.add(annotation.getProperties());
            }
            return result;
        }

        private Set<Map<String, Object>> propertiesOf(Annotation[] annotations) {
            Set<Map<String, Object>> result = new HashSet<>();
            for (Annotation annotation : annotations) {
                result.add(propertiesOf(annotation));
            }
            return result;
        }

        private Map<String, Object> propertiesOf(Annotation annotation) {
            Map<String, Object> props = new HashMap<>();
            for (Method method : annotation.annotationType().getDeclaredMethods()) {
                Object returnValue = invoke(method, annotation);
                props.put(method.getName(), valueOf(returnValue));
            }
            return props;
        }

        private Object valueOf(Object value) {
            if (value instanceof Class) {
                return TypeDetails.of((Class<?>) value);
            }
            if (value instanceof Class[]) {
                return TypeDetails.allOf((Class<?>[]) value);
            }
            if (value instanceof Enum) {
                return enumConstant((Enum<?>) value);
            }
            if (value instanceof Enum[]) {
                return enumConstants((Enum[]) value);
            }
            return value;
        }

        private Object enumConstants(Enum[] enums) {
            List<Object> result = new ArrayList<>();
            for (Enum e : enums) {
                result.add(enumConstant(e));
            }
            return result;
        }

        private Object invoke(Method method, Object owner) {
            try {
                return method.invoke(owner);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ConditionEventsAssert extends AbstractIterableAssert<ConditionEventsAssert, ConditionEvents, ConditionEvent> {
        protected ConditionEventsAssert(ConditionEvents actual) {
            super(actual, ConditionEventsAssert.class);
        }

        public void containViolations(String violation, String... additional) {
            assertThat(actual.containViolation()).as("Condition is violated").isTrue();

            List<String> expected = concat(violation, additional);
            if (!sorted(violatingMessages()).equals(sorted(expected))) {
                failWithMessage("Expected %s to contain only violations %s", actual, expected);
            }
        }

        public void containAllowed(String message, String... additional) {
            assertThat(actual.getAllowed().size()).as("Allowed events occurred").isGreaterThan(0);

            List<String> expected = concat(message, additional);
            if (!sorted(messagesOf(actual.getAllowed())).equals(sorted(expected))) {
                failWithMessage("Expected %s to contain only allowed events %s", actual, expected);
            }
        }

        private List<String> violatingMessages() {
            return messagesOf(actual.getViolating());
        }

        private List<String> messagesOf(Collection<ConditionEvent> events) {
            FailureMessages messages = new FailureMessages();
            for (ConditionEvent event : events) {
                event.describeTo(messages);
            }
            return newArrayList(messages);
        }

        private List<String> concat(String violation, String[] additional) {
            ArrayList<String> list = newArrayList(additional);
            list.add(0, violation);
            return list;
        }

        private List<String> sorted(Collection<String> collection) {
            ArrayList<String> result = new ArrayList<>(collection);
            Collections.sort(result);
            return result;
        }

        public void containNoViolation() {
            assertThat(actual.containViolation()).as("Condition is violated").isFalse();
            assertThat(violatingMessages()).as("No violating messages").isEmpty();
        }

        public void haveOneViolationMessageContaining(Set<String> messageParts) {
            assertThat(violatingMessages()).as("Number of violations").hasSize(1);
            AbstractCharSequenceAssert<?, String> assertion = assertThat(getOnlyElement(violatingMessages()));
            for (String part : messageParts) {
                assertion.as("violation message containing " + part).contains(part);
            }
        }
    }
}
