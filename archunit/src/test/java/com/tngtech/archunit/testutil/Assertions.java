package com.tngtech.archunit.testutil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.lang.CollectsLines;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ObjectAssertFactory;
import org.objectweb.asm.Type;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.domain.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.domain.Formatters.formatMethodSimple;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.NATIVE;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNCHRONIZED;
import static com.tngtech.archunit.core.domain.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.domain.JavaModifier.VOLATILE;
import static com.tngtech.archunit.core.domain.TestUtils.resolvedTargetFrom;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static com.tngtech.archunit.testutil.TestUtils.invoke;

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

    public static JavaClassesAssertion assertThatClasses(Iterable<JavaClass> javaClasses) {
        return new JavaClassesAssertion(javaClasses);
    }

    public static JavaMemberAssertion<?, ?> assertThat(JavaMember member) {
        return new JavaMemberAssertion<>(member, JavaMemberAssertion.class);
    }

    public static JavaCodeUnitAssertion<?, ?> assertThat(JavaCodeUnit codeUnit) {
        return new JavaCodeUnitAssertion<>(codeUnit, JavaCodeUnitAssertion.class);
    }

    public static JavaMembersAssertion assertThatMembers(Iterable<? extends JavaMember> members) {
        return new JavaMembersAssertion(members);
    }

    public static JavaClassesAssertion assertThat(JavaClass[] javaClasses) {
        return new JavaClassesAssertion(javaClasses);
    }

    public static JavaClassListAssertion assertThat(JavaClassList javaClasses) {
        return new JavaClassListAssertion(javaClasses);
    }

    public static JavaFieldAssertion assertThat(FieldAccessTarget target) {
        return assertThat(target.resolveField().get());
    }

    public static JavaFieldAssertion assertThat(JavaField field) {
        return new JavaFieldAssertion(field);
    }

    public static JavaEnumConstantAssertion assertThat(JavaEnumConstant enumConstant) {
        return new JavaEnumConstantAssertion(enumConstant);
    }

    public static JavaEnumConstantsAssertion assertThat(JavaEnumConstant[] enumConstants) {
        return new JavaEnumConstantsAssertion(enumConstants);
    }

    public static JavaTypeAssertion assertThat(JavaType javaType) {
        return new JavaTypeAssertion(javaType);
    }

    public static ThrowsDeclarationAssertion assertThat(ThrowsDeclaration<?> throwsDeclaration) {
        return new ThrowsDeclarationAssertion(throwsDeclaration);
    }

    public static ThrowsClauseAssertion assertThat(ThrowsClause<?> throwsClause) {
        return new ThrowsClauseAssertion(throwsClause);
    }

    @SuppressWarnings("unchecked") // covariant
    public static AccessesAssertion assertThatAccesses(Collection<? extends JavaAccess<?>> accesses) {
        return new AccessesAssertion((Collection<JavaAccess<?>>) accesses);
    }

    public static ExpectedAccessCreation expectedAccess() {
        return new ExpectedAccessCreation();
    }

    public static class ExpectedAccessCreation {
        private ExpectedAccessCreation() {
        }

        public Step2 from(Class<?> originClass, String codeUnitName) {
            return new Step2(originClass, codeUnitName);
        }

        public class Step2 {
            private final Class<?> originClass;
            private final String originCodeUnitName;

            private Step2(Class<?> originClass, String originCodeUnitName) {
                this.originClass = originClass;
                this.originCodeUnitName = originCodeUnitName;
            }

            public Condition<JavaAccess<?>> to(final Class<?> targetClass, final String targetName) {
                return new Condition<JavaAccess<?>>(
                        String.format("%s from %s.%s to %s.%s",
                                JavaAccess.class.getSimpleName(),
                                originClass.getName(), originCodeUnitName,
                                targetClass.getSimpleName(), targetName)) {
                    @Override
                    public boolean matches(JavaAccess<?> access) {
                        return access.getOriginOwner().isEquivalentTo(originClass) &&
                                access.getOrigin().getName().equals(originCodeUnitName) &&
                                access.getTargetOwner().isEquivalentTo(targetClass) &&
                                access.getTarget().getName().equals(targetName);
                    }
                };
            }

            public Condition<JavaAccess<?>> toConstructor(final Class<?> targetClass, final Class<?>... paramTypes) {
                final List<String> paramTypeNames = namesOf(paramTypes);
                return new Condition<JavaAccess<?>>(
                        String.format("%s from %s.%s to %s",
                                JavaAccess.class.getSimpleName(),
                                originClass.getName(), originCodeUnitName,
                                formatMethodSimple(targetClass.getSimpleName(), CONSTRUCTOR_NAME, paramTypeNames))) {
                    @Override
                    public boolean matches(JavaAccess<?> access) {
                        return to(targetClass, CONSTRUCTOR_NAME).matches(access) &&
                                ((ConstructorCallTarget) access.getTarget()).getRawParameterTypes().getNames().equals(paramTypeNames);
                    }
                };
            }
        }
    }

    public static AccessToFieldAssertion assertThatAccess(JavaFieldAccess access) {
        return new AccessToFieldAssertion(access);
    }

    public static MethodCallAssertion assertThatCall(JavaMethodCall call) {
        return new MethodCallAssertion(call);
    }

    public static ConstructorCallAssertion assertThatCall(JavaConstructorCall call) {
        return new ConstructorCallAssertion(call);
    }

    public static class JavaClassesAssertion extends AbstractObjectAssert<JavaClassesAssertion, JavaClass[]> {
        private JavaClassesAssertion(JavaClass[] actual) {
            super(actual, JavaClassesAssertion.class);
        }

        private JavaClassesAssertion(Iterable<JavaClass> actual) {
            super(sort(actual), JavaClassesAssertion.class);
        }

        private static JavaClass[] sort(Iterable<JavaClass> actual) {
            JavaClass[] result = Iterables.toArray(actual, JavaClass.class);
            Arrays.sort(result, new Comparator<JavaClass>() {
                @Override
                public int compare(JavaClass o1, JavaClass o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return result;
        }

        public void matchInAnyOrder(Iterable<Class<?>> classes) {
            Class<?>[] sorted = Iterables.toArray(classes, Class.class);
            Arrays.sort(sorted, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            matchExactly(sorted);
        }

        public void matchInAnyOrder(Class<?>... classes) {
            matchInAnyOrder(ImmutableSet.copyOf(classes));
        }

        public void matchExactly(Class<?>... classes) {
            assertThat((Object[]) actual).as("classes").hasSize(classes.length);
            for (int i = 0; i < actual.length; i++) {
                assertThat(actual[i]).as("Element %d", i).matches(classes[i]);
            }
        }

        public void contain(Class<?>... classes) {
            contain(ImmutableSet.copyOf(classes));
        }

        public void dontContain(Class<?>... classes) {
            assertThat(actualNames()).doesNotContainAnyElementsOf(JavaClass.namesOf(classes));
        }

        public void contain(Iterable<Class<?>> classes) {
            List<String> expectedNames = JavaClass.namesOf(Lists.newArrayList(classes));
            assertThat(actualNames()).as("actual classes").containsAll(expectedNames);
        }

        private Set<String> actualNames() {
            Set<String> actualNames = new HashSet<>();
            for (JavaClass javaClass : actual) {
                actualNames.add(javaClass.getName());
            }
            return actualNames;
        }
    }

    public static class JavaClassAssertion extends AbstractObjectAssert<JavaClassAssertion, JavaClass> {
        private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\[+)(.*)");

        private JavaClassAssertion(JavaClass javaClass) {
            super(javaClass, JavaClassAssertion.class);
        }

        public void matches(Class<?> clazz) {
            assertThat(actual.getName()).as("Name of " + actual)
                    .isEqualTo(clazz.getName());
            assertThat(actual.getSimpleName()).as("Simple name of " + actual)
                    .isEqualTo(ensureArrayName(clazz.getSimpleName()));
            assertThat(actual.getPackage()).as("Package of " + actual)
                    .isEqualTo(clazz.getPackage() != null ? clazz.getPackage().getName() : "");
            assertThat(actual.getPackageName()).as("Package name of " + actual)
                    .isEqualTo(clazz.getPackage() != null ? clazz.getPackage().getName() : "");
            assertThat(actual.getModifiers()).as("Modifiers of " + actual)
                    .isEqualTo(JavaModifier.getModifiersForClass(clazz.getModifiers()));
            assertThat(propertiesOf(actual.getAnnotations())).as("Annotations of " + actual)
                    .isEqualTo(propertiesOf(clazz.getAnnotations()));
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

    public static class JavaClassListAssertion
            extends AbstractListAssert<JavaClassListAssertion, List<? extends JavaClass>, JavaClass, ObjectAssert<JavaClass>> {
        private JavaClassListAssertion(JavaClassList javaClasses) {
            super(javaClasses, JavaClassListAssertion.class);
        }

        public void matches(Class<?>... classes) {
            assertThat(actual).as("JavaClasses").hasSize(classes.length);
            for (int i = 0; i < actual.size(); i++) {
                assertThat(actual.get(i)).as("Element %d", i).matches(classes[i]);
            }
        }

        @Override
        protected ObjectAssert<JavaClass> toAssert(JavaClass value, String description) {
            return new ObjectAssertFactory<JavaClass>().createAssert(value).as(description);
        }
    }

    public static class JavaMembersAssertion extends AbstractObjectAssert<JavaMembersAssertion, List<JavaMember>> {
        private JavaMembersAssertion(Iterable<? extends JavaMember> javaMembers) {
            super(sort(javaMembers), JavaMembersAssertion.class);
        }

        private static List<JavaMember> sort(Iterable<? extends JavaMember> actual) {
            List<JavaMember> result = newArrayList(actual);
            Collections.sort(result, new Comparator<JavaMember>() {
                @Override
                public int compare(JavaMember o1, JavaMember o2) {
                    return o1.getFullName().compareTo(o2.getFullName());
                }
            });
            return result;
        }

        public <M extends Member & AnnotatedElement> void matchInAnyOrder(Collection<M> expectedMembers) {
            assertThat(actual).as("Actual JavaMembers").hasSameSizeAs(expectedMembers);
            List<M> sorted = sort(expectedMembers);
            for (int i = 0; i < sorted.size(); i++) {
                assertThat(actual.get(i).getName()).as("Sorted member number " + i).isEqualTo(getExpectedNameOf(sorted.get(i)));
                assertThat(actual.get(i)).isEquivalentTo(sorted.get(i));
            }
        }

        private <M extends Member & AnnotatedElement> List<M> sort(Collection<M> expectedMembers) {
            List<M> sorted = new ArrayList<>(expectedMembers);
            Collections.sort(sorted, new Comparator<M>() {
                @Override
                public int compare(M o1, M o2) {
                    return getExpectedFullNameOf(o1).compareTo(getExpectedFullNameOf(o2));
                }
            });
            return sorted;
        }

        public void matchInAnyOrderMembersOf(Class<?>... classes) {
            Set<Member> members = new HashSet<>();
            for (Class<?> clazz : classes) {
                members.addAll(ImmutableSet.copyOf(clazz.getDeclaredFields()));
                members.addAll(ImmutableSet.copyOf(clazz.getDeclaredMethods()));
                members.addAll(ImmutableSet.copyOf(clazz.getDeclaredConstructors()));
            }

            matchCasted(members);
        }

        // We know this is compatible since every Member also implements AnnotatedElement
        // Unfortunately the Java type system does not make it easy here
        @SuppressWarnings("rawtypes")
        private void matchCasted(Set<Member> members) {
            matchInAnyOrder((Set) members);
        }
    }

    public static class JavaMemberAssertion<T extends JavaMember, SELF extends JavaMemberAssertion<T, SELF>>
            extends AbstractObjectAssert<SELF, T> {
        private JavaMemberAssertion(T javaMember, Class<SELF> selfType) {
            super(javaMember, selfType);
        }

        public <M extends Member & AnnotatedElement> void isEquivalentTo(M member) {
            assertEquivalent(actual, member);
            String expectedName = getExpectedNameOf(member);
            assertThat(actual.getName()).isEqualTo(expectedName);
            assertThat(actual.getFullName()).isEqualTo(getExpectedFullNameOf(member));
        }

    }

    private static String getExpectedNameOf(Member member) {
        return member instanceof Constructor ? CONSTRUCTOR_NAME : member.getName();
    }

    public static class JavaCodeUnitAssertion<T extends JavaCodeUnit, SELF extends JavaCodeUnitAssertion<T, SELF>>
            extends JavaMemberAssertion<T, SELF> {
        private JavaCodeUnitAssertion(T javaMember, Class<SELF> selfType) {
            super(javaMember, selfType);
        }

        public void isEquivalentTo(Method method) {
            super.isEquivalentTo(method);
            assertThat(actual.getParameters()).matches(method.getParameterTypes());
            assertThat(actual.getRawParameterTypes()).matches(method.getParameterTypes());
            assertThat(actual.getReturnType()).matches(method.getReturnType());
            assertThat(actual.getRawReturnType()).matches(method.getReturnType());
        }

        public void isEquivalentTo(Constructor<?> constructor) {
            super.isEquivalentTo(constructor);
            assertThat(actual.getParameters()).matches(constructor.getParameterTypes());
            assertThat(actual.getRawParameterTypes()).matches(constructor.getParameterTypes());
            assertThat(actual.getReturnType()).matches(void.class);
            assertThat(actual.getRawReturnType()).matches(void.class);
        }
    }

    public static class JavaFieldAssertion extends JavaMemberAssertion<JavaField, JavaFieldAssertion> {
        private JavaFieldAssertion(JavaField javaField) {
            super(javaField, JavaFieldAssertion.class);
        }

        public void isEquivalentTo(Field field) {
            super.isEquivalentTo(field);
            assertThat(actual.getType()).matches(field.getType());
        }
    }

    public static class JavaEnumConstantAssertion extends AbstractObjectAssert<JavaEnumConstantAssertion, JavaEnumConstant> {
        private JavaEnumConstantAssertion(JavaEnumConstant enumConstant) {
            super(enumConstant, JavaEnumConstantAssertion.class);
        }

        public void isEquivalentTo(Enum<?> enumConstant) {
            assertThat(actual).as(JavaEnumConstant.class.getSimpleName()).isNotNull();
            assertThat(actual.getDeclaringClass().getName()).isEqualTo(enumConstant.getDeclaringClass().getName());
            assertThat(actual.name()).isEqualTo(enumConstant.name());
        }
    }

    public static class JavaEnumConstantsAssertion extends AbstractObjectAssert<JavaEnumConstantsAssertion, JavaEnumConstant[]> {
        private JavaEnumConstantsAssertion(JavaEnumConstant[] enumConstants) {
            super(enumConstants, JavaEnumConstantsAssertion.class);
        }

        public void matches(Enum<?>... enumConstants) {
            assertThat((Object[]) actual).as("Enum constants").hasSize(enumConstants.length);
            for (int i = 0; i < actual.length; i++) {
                assertThat(actual[i]).as("Element %d", i).isEquivalentTo(enumConstants[i]);
            }
        }
    }

    public static class ThrowsDeclarationAssertion extends AbstractObjectAssert<ThrowsDeclarationAssertion, ThrowsDeclaration<?>> {
        private ThrowsDeclarationAssertion(ThrowsDeclaration<?> throwsDeclaration) {
            super(throwsDeclaration, ThrowsDeclarationAssertion.class);
        }

        public void matches(Class<?> clazz) {
            assertThat(actual.getType()).as("Type of " + actual)
                    .matches(clazz);
        }
    }

    public static class ThrowsClauseAssertion extends
            AbstractIterableAssert<ThrowsClauseAssertion, ThrowsClause<?>, ThrowsDeclaration<?>, ObjectAssert<ThrowsDeclaration<?>>> {
        private ThrowsClauseAssertion(ThrowsClause<?> throwsClause) {
            super(throwsClause, ThrowsClauseAssertion.class);
        }

        public void matches(Class<?>... classes) {
            assertThat(actual).as("ThrowsClause").hasSize(classes.length);
            for (int i = 0; i < actual.size(); i++) {
                assertThat(Iterables.get(actual, i)).as("Element %d", i).matches(classes[i]);
            }
        }

        @Override
        protected ObjectAssert<ThrowsDeclaration<?>> toAssert(ThrowsDeclaration<?> value, String description) {
            return new ObjectAssertFactory<ThrowsDeclaration<?>>().createAssert(value).as(description);
        }
    }

    private static <T extends Member & AnnotatedElement> void assertEquivalent(JavaMember javaMember, T member) {
        assertThat(javaMember.getOwner().reflect()).isEqualTo(member.getDeclaringClass());
        assertModifiersMatch(javaMember, member);
        assertThat(propertiesOf(javaMember.getAnnotations())).isEqualTo(propertiesOf(member.getAnnotations()));
    }

    private static <T extends Member> void assertModifiersMatch(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(ABSTRACT))
                .as("member %s is abstract", javaMember.getFullName())
                .isEqualTo(Modifier.isAbstract(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(FINAL))
                .as("member %s is final", javaMember.getFullName())
                .isEqualTo(Modifier.isFinal(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PRIVATE))
                .as("member %s is private", javaMember.getFullName())
                .isEqualTo(Modifier.isPrivate(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PROTECTED))
                .as("member %s is protected", javaMember.getFullName())
                .isEqualTo(Modifier.isProtected(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PUBLIC))
                .as("member %s is public", javaMember.getFullName())
                .isEqualTo(Modifier.isPublic(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(STATIC))
                .as("member %s is static", javaMember.getFullName())
                .isEqualTo(Modifier.isStatic(member.getModifiers()));

        if (javaMember instanceof JavaCodeUnit) {
            assertCodeUnitModifiers(javaMember, member);
        }

        if (javaMember instanceof JavaField) {
            assertFieldModifiers(javaMember, member);
        }
    }

    private static <T extends Member> void assertCodeUnitModifiers(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(NATIVE))
                .as("member %s is native", javaMember.getFullName())
                .isEqualTo(Modifier.isNative(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(SYNCHRONIZED))
                .as("member %s is synchronized", javaMember.getFullName())
                .isEqualTo(Modifier.isSynchronized(member.getModifiers()));
    }

    private static <T extends Member> void assertFieldModifiers(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(TRANSIENT))
                .as("member %s is transient", javaMember.getFullName())
                .isEqualTo(Modifier.isTransient(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(VOLATILE))
                .as("member %s is volatile", javaMember.getFullName())
                .isEqualTo(Modifier.isVolatile(member.getModifiers()));
    }

    private static <T extends Member & AnnotatedElement> String getExpectedFullNameOf(T member) {
        String base = member.getDeclaringClass().getName() + "." + getExpectedNameOf(member);
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

    @SuppressWarnings("rawtypes")
    private static Set<Map<String, Object>> propertiesOf(Set<JavaAnnotation> annotations) {
        List<Annotation> converted = new ArrayList<>();
        for (JavaAnnotation annotation : annotations) {
            converted.add(annotation.as((Class) annotation.getType().reflect()));
        }
        return propertiesOf(converted.toArray(new Annotation[0]));
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
            return new SimpleTypeReference(((Class<?>) value).getName());
        }
        if (value instanceof Class[]) {
            return SimpleTypeReference.allOf((Class<?>[]) value);
        }
        if (value instanceof Enum) {
            return new SimpleEnumConstantReference((Enum<?>) value);
        }
        if (value instanceof Enum[]) {
            return SimpleEnumConstantReference.allOf((Enum<?>[]) value);
        }
        if (value instanceof Annotation) {
            return propertiesOf((Annotation) value);
        }
        if (value instanceof Annotation[]) {
            return propertiesOf((Annotation[]) value);
        }
        return value;
    }

    private static class SimpleTypeReference {
        private final String typeName;

        private SimpleTypeReference(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleTypeReference other = (SimpleTypeReference) obj;
            return Objects.equals(this.typeName, other.typeName);
        }

        @Override
        public String toString() {
            return typeName;
        }

        static List<SimpleTypeReference> allOf(Class<?>[] value) {
            ImmutableList.Builder<SimpleTypeReference> result = ImmutableList.builder();
            for (Class<?> c : value) {
                result.add(new SimpleTypeReference(c.getName()));
            }
            return result.build();
        }
    }

    private static class SimpleEnumConstantReference {
        private final SimpleTypeReference type;
        private final String name;

        SimpleEnumConstantReference(Enum<?> value) {
            this.type = new SimpleTypeReference(value.getDeclaringClass().getName());
            this.name = value.name();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleEnumConstantReference other = (SimpleEnumConstantReference) obj;
            return Objects.equals(this.type, other.type)
                    && Objects.equals(this.name, other.name);
        }

        @Override
        public String toString() {
            return type + "." + name;
        }

        static List<SimpleEnumConstantReference> allOf(Enum<?>[] values) {
            ImmutableList.Builder<SimpleEnumConstantReference> result = ImmutableList.builder();
            for (Enum<?> value : values) {
                result.add(new SimpleEnumConstantReference(value));
            }
            return result.build();
        }
    }

    public static class ConditionEventsAssert
            extends AbstractIterableAssert<ConditionEventsAssert, ConditionEvents, ConditionEvent, ObjectAssert<ConditionEvent>> {
        ConditionEventsAssert(ConditionEvents actual) {
            super(actual, ConditionEventsAssert.class);
        }

        public void containViolations(String violation, String... additional) {
            assertThat(actual.containViolation()).as("Condition is violated").isTrue();

            List<String> expected = concat(violation, additional);
            if (!sorted(messagesOf(actual.getViolating())).equals(sorted(expected))) {
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

        private List<String> messagesOf(Collection<? extends ConditionEvent> events) {
            final List<String> result = new ArrayList<>();
            CollectsLines messages = new CollectsLines() {
                @Override
                public void add(String message) {
                    result.add(message);
                }
            };
            for (ConditionEvent event : events) {
                event.describeTo(messages);
            }
            return result;
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
            assertThat(messagesOf(actual.getViolating())).as("Violating messages").isEmpty();
        }

        public ConditionEventsAssert haveOneViolationMessageContaining(String... messageParts) {
            return haveOneViolationMessageContaining(ImmutableSet.copyOf(messageParts));
        }

        public ConditionEventsAssert haveOneViolationMessageContaining(Set<String> messageParts) {
            assertThat(messagesOf(actual.getViolating())).as("Number of violations").hasSize(1);
            AbstractCharSequenceAssert<?, String> assertion = assertThat(getOnlyElement(messagesOf(actual.getViolating())));
            for (String part : messageParts) {
                assertion.as("Violation message").contains(part);
            }
            return this;
        }

        public ConditionEventsAssert haveAtLeastOneViolationMessageMatching(String regex) {
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            for (String message : messagesOf(actual.getViolating())) {
                if (pattern.matcher(message).matches()) {
                    return this;
                }
            }
            throw new AssertionError(String.format("No message matches pattern '%s'", regex));
        }

        @Override
        protected ObjectAssert<ConditionEvent> toAssert(ConditionEvent value, String description) {
            return new ObjectAssertFactory<ConditionEvent>().createAssert(value).as(description);
        }
    }

    public static class AccessesAssertion {
        private final Set<JavaAccess<?>> actualRemaining;

        AccessesAssertion(Collection<JavaAccess<?>> accesses) {
            this.actualRemaining = new HashSet<>(accesses);
        }

        public AccessesAssertion contain(Condition<? super JavaAccess<?>> condition) {
            for (Iterator<JavaAccess<?>> iterator = actualRemaining.iterator(); iterator.hasNext(); ) {
                if (condition.matches(iterator.next())) {
                    iterator.remove();
                    return this;
                }
            }
            throw new AssertionError("No access matches " + condition);
        }

        @SafeVarargs
        public final AccessesAssertion containOnly(Condition<? super JavaAccess<?>>... conditions) {
            for (Condition<? super JavaAccess<?>> condition : conditions) {
                contain(condition);
            }
            assertThat(actualRemaining).as("Unexpected " + JavaAccess.class.getSimpleName()).isEmpty();
            return this;
        }
    }

    protected abstract static class BaseAccessAssertion<
            SELF extends BaseAccessAssertion<SELF, ACCESS, TARGET>,
            ACCESS extends JavaAccess<TARGET>,
            TARGET extends AccessTarget> {

        ACCESS access;

        BaseAccessAssertion(ACCESS access) {
            this.access = access;
        }

        public SELF isFrom(String name, Class<?>... parameterTypes) {
            return isFrom(access.getOrigin().getOwner().getCodeUnitWithParameterTypes(name, parameterTypes));
        }

        public SELF isFrom(JavaCodeUnit codeUnit) {
            assertThat(access.getOrigin()).as("Origin of field access").isEqualTo(codeUnit);
            return newAssertion(access);
        }

        public SELF isTo(TARGET target) {
            assertThat(access.getTarget()).as("Target of " + access.getName()).isEqualTo(target);
            return newAssertion(access);
        }

        public SELF isTo(Condition<TARGET> target) {
            assertThat(access.getTarget()).as("Target of " + access.getName()).is(target);
            return newAssertion(access);
        }

        public void inLineNumber(int number) {
            assertThat(access.getLineNumber())
                    .as("Line number of access to " + access.getName())
                    .isEqualTo(number);
        }

        protected abstract SELF newAssertion(ACCESS access);
    }

    public static class AccessToFieldAssertion extends BaseAccessAssertion<AccessToFieldAssertion, JavaFieldAccess, FieldAccessTarget> {
        private AccessToFieldAssertion(JavaFieldAccess access) {
            super(access);
        }

        @Override
        protected AccessToFieldAssertion newAssertion(JavaFieldAccess access) {
            return new AccessToFieldAssertion(access);
        }

        public AccessToFieldAssertion isTo(String name) {
            return isTo(access.getTarget().getOwner().getField(name));
        }

        public AccessToFieldAssertion isTo(JavaField field) {
            return isTo(targetFrom(field));
        }

        public AccessToFieldAssertion isOfType(JavaFieldAccess.AccessType type) {
            assertThat(access.getAccessType()).isEqualTo(type);
            return newAssertion(access);
        }
    }

    public static class MethodCallAssertion extends BaseAccessAssertion<MethodCallAssertion, JavaMethodCall, MethodCallTarget> {
        private MethodCallAssertion(JavaMethodCall call) {
            super(call);
        }

        public MethodCallAssertion isTo(JavaMethod target) {
            return isTo(resolvedTargetFrom(target));
        }

        @Override
        protected MethodCallAssertion newAssertion(JavaMethodCall call) {
            return new MethodCallAssertion(call);
        }
    }

    public static class ConstructorCallAssertion extends BaseAccessAssertion<ConstructorCallAssertion, JavaConstructorCall, ConstructorCallTarget> {
        private ConstructorCallAssertion(JavaConstructorCall call) {
            super(call);
        }

        public ConstructorCallAssertion isTo(JavaConstructor target) {
            return isTo(targetFrom(target));
        }

        @Override
        protected ConstructorCallAssertion newAssertion(JavaConstructorCall call) {
            return new ConstructorCallAssertion(call);
        }
    }

    public static class JavaTypeAssertion extends AbstractObjectAssert<JavaTypeAssertion, JavaType> {
        private JavaTypeAssertion(JavaType actual) {
            super(actual, JavaTypeAssertion.class);
        }

        public void isEquivalentTo(Class<?> clazz) {
            assertThat(actual.getName()).as("name").isEqualTo(clazz.getName());
            assertThat(actual.getSimpleName()).as("simple name").isEqualTo(clazz.getSimpleName());
            assertThat(actual.getPackageName()).as("package").isEqualTo(clazz.getPackage() != null ? clazz.getPackage().getName() : "");
        }
    }
}
