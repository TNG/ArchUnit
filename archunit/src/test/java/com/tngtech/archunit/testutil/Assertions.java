package com.tngtech.archunit.testutil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClassList;
import com.tngtech.archunit.core.JavaConstructor;
import com.tngtech.archunit.core.JavaField;
import com.tngtech.archunit.core.JavaMember;
import com.tngtech.archunit.core.JavaMethod;
import com.tngtech.archunit.core.Optional;
import com.tngtech.archunit.core.TypeDetails;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.FailureMessages;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.cglib.asm.Type;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.JavaModifier.FINAL;
import static com.tngtech.archunit.core.JavaModifier.NATIVE;
import static com.tngtech.archunit.core.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.JavaModifier.STATIC;
import static com.tngtech.archunit.core.JavaModifier.SYNCHRONIZED;
import static com.tngtech.archunit.core.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.JavaModifier.VOLATILE;
import static com.tngtech.archunit.core.ReflectionUtils.namesOf;
import static com.tngtech.archunit.core.TestUtils.classForName;
import static com.tngtech.archunit.core.TestUtils.enumConstant;
import static com.tngtech.archunit.core.TestUtils.invoke;

public class Assertions extends org.assertj.core.api.Assertions {
    public static ConditionEventsAssert assertThat(ConditionEvents events) {
        return new ConditionEventsAssert(events);
    }

    public static <T> org.assertj.guava.api.OptionalAssert<T> assertThat(Optional<T> optional) {
        return org.assertj.guava.api.Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    public static JavaClassAssertion assertThat(JavaClass javaClass) {
        return new JavaClassAssertion(javaClass);
    }

    public static JavaClassListAssertion assertThat(JavaClassList javaClasses) {
        return new JavaClassListAssertion(javaClasses);
    }

    public static JavaFieldAssertion assertThat(FieldAccessTarget target) {
        return assertThat(target.resolve().get());
    }

    public static JavaFieldAssertion assertThat(JavaField field) {
        return new JavaFieldAssertion(field);
    }

    public static JavaMethodAssertion assertThat(JavaMethod method) {
        return new JavaMethodAssertion(method);
    }

    public static JavaConstructorAssertion assertThat(JavaConstructor constructor) {
        return new JavaConstructorAssertion(constructor);
    }

    public static class JavaClassAssertion extends AbstractObjectAssert<JavaClassAssertion, JavaClass> {
        private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\[+)(.*)");

        private JavaClassAssertion(JavaClass javaClass) {
            super(javaClass, JavaClassAssertion.class);
        }

        public void matches(Class<?> clazz) {
            assertThat(actual.getName()).isEqualTo(ensureArrayName(clazz.getName()));
            assertThat(actual.getSimpleName()).isEqualTo(ensureArrayName(clazz.getSimpleName()));
            assertThat(propertiesOf(actual.getAnnotations())).isEqualTo(propertiesOf(clazz.getAnnotations()));
        }

        private String ensureArrayName(String name) {
            String suffix = "";
            Matcher matcher = ARRAY_PATTERN.matcher(name);
            if (matcher.matches()) {
                name = Type.getType(matcher.group(2)).getClassName();
                suffix = Strings.repeat("[]", matcher.group(1).length());
            }
            return name + suffix;
        }
    }

    public static class JavaClassListAssertion extends AbstractListAssert<JavaClassListAssertion, List<? extends JavaClass>, JavaClass> {
        private JavaClassListAssertion(JavaClassList javaClasses) {
            super(javaClasses, JavaClassListAssertion.class);
        }

        public void matches(Class<?>... classes) {
            assertThat(actual).as("JavaClasses").hasSize(classes.length);
            for (int i = 0; i < actual.size(); i++) {
                assertThat(actual.get(i)).as("Element %d", i).matches(classes[i]);
            }
        }
    }

    public static class JavaFieldAssertion extends AbstractObjectAssert<JavaFieldAssertion, JavaField> {
        private JavaFieldAssertion(JavaField javaField) {
            super(javaField, JavaFieldAssertion.class);
        }

        public void isEquivalentTo(Field field) {
            assertEquivalent(actual, field);
            assertThat(actual.getName()).isEqualTo(field.getName());
            assertThat(actual.getFullName()).isEqualTo(getExpectedNameOf(field, field.getName()));
            assertThat(actual.getType()).matches(field.getType());
        }
    }

    public static class JavaMethodAssertion extends AbstractObjectAssert<JavaMethodAssertion, JavaMethod> {
        private JavaMethodAssertion(JavaMethod javaMethod) {
            super(javaMethod, JavaMethodAssertion.class);
        }

        public void isEquivalentTo(Method method) {
            assertEquivalent(actual, method);
            assertThat(actual.getName()).isEqualTo(method.getName());
            assertThat(actual.getFullName()).isEqualTo(getExpectedNameOf(method, method.getName()));
            assertThat(actual.getParameters()).matches(method.getParameterTypes());
            assertThat(actual.getReturnType()).matches(method.getReturnType());
        }
    }

    public static class JavaConstructorAssertion extends AbstractObjectAssert<JavaConstructorAssertion, JavaConstructor> {
        private JavaConstructorAssertion(JavaConstructor javaConstructor) {
            super(javaConstructor, JavaConstructorAssertion.class);
        }

        public void isEquivalentTo(Constructor<?> constructor) {
            assertEquivalent(actual, constructor);
            assertThat(actual.getName()).isEqualTo(CONSTRUCTOR_NAME);
            assertThat(actual.getFullName()).isEqualTo(getExpectedNameOf(constructor, CONSTRUCTOR_NAME));
            assertThat(actual.getParameters()).matches(constructor.getParameterTypes());
            assertThat(actual.getReturnType()).matches(void.class);
        }
    }

    private static <T extends Member & AnnotatedElement> void assertEquivalent(JavaMember javaMember, T member) {
        assertThat(javaMember.getOwner().reflect()).isEqualTo(member.getDeclaringClass());
        assertModifiersMatch(javaMember, member);
        assertThat(propertiesOf(javaMember.getAnnotations())).isEqualTo(propertiesOf(member.getAnnotations()));
    }

    private static <T extends Member> void assertModifiersMatch(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(ABSTRACT))
                .as("member is abstract")
                .isEqualTo(Modifier.isAbstract(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(FINAL))
                .as("member is final")
                .isEqualTo(Modifier.isFinal(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(NATIVE))
                .as("member is native")
                .isEqualTo(Modifier.isNative(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PRIVATE))
                .as("member is private")
                .isEqualTo(Modifier.isPrivate(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PROTECTED))
                .as("member is protected")
                .isEqualTo(Modifier.isProtected(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PUBLIC))
                .as("member is public")
                .isEqualTo(Modifier.isPublic(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(STATIC))
                .as("member is static")
                .isEqualTo(Modifier.isStatic(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(SYNCHRONIZED))
                .as("member is synchronized")
                .isEqualTo(Modifier.isSynchronized(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(TRANSIENT))
                .as("member is transient")
                .isEqualTo(Modifier.isTransient(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(VOLATILE))
                .as("member is volatile")
                .isEqualTo(Modifier.isVolatile(member.getModifiers()));
    }

    private static <T extends Member & AnnotatedElement> String getExpectedNameOf(T member, String name) {
        String base = member.getDeclaringClass().getName() + "." + name;
        if (member instanceof Method) {
            return base + expectedParametersOf(((Method) member).getParameterTypes());
        }
        if (member instanceof Constructor<?>) {
            return base + expectedParametersOf(((Constructor<?>) member).getParameterTypes());
        }
        return base;
    }

    private static String expectedParametersOf(Class<?>[] parameterTypes) {
        return String.format("(%s)", formatMethodParameterTypeNames(namesOf(parameterTypes)));
    }

    private static Set<Map<String, Object>> propertiesOf(Set<JavaAnnotation> annotations) {
        List<Annotation> converted = new ArrayList<>();
        for (JavaAnnotation annotation : annotations) {
            converted.add(annotation.as((Class) classForName(annotation.getType().getName())));
        }
        return propertiesOf(converted.toArray(new Annotation[converted.size()]));
    }

    private static Set<Map<String, Object>> propertiesOf(Annotation[] annotations) {
        Set<Map<String, Object>> result = new HashSet<>();
        for (Annotation annotation : annotations) {
            result.add(propertiesOf(annotation));
        }
        return result;
    }

    private static Map<String, Object> propertiesOf(Annotation annotation) {
        Map<String, Object> props = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Object returnValue = invoke(method, annotation);
            props.put(method.getName(), valueOf(returnValue));
        }
        return props;
    }

    private static Object valueOf(Object value) {
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
        if (value instanceof Annotation) {
            return propertiesOf((Annotation) value);
        }
        if (value instanceof Annotation[]) {
            return propertiesOf((Annotation[]) value);
        }
        return value;
    }

    private static Object enumConstants(Enum[] enums) {
        List<Object> result = new ArrayList<>();
        for (Enum e : enums) {
            result.add(enumConstant(e));
        }
        return result;
    }

    public static class ConditionEventsAssert extends AbstractIterableAssert<ConditionEventsAssert, ConditionEvents, ConditionEvent> {
        ConditionEventsAssert(ConditionEvents actual) {
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
