package com.tngtech.archunit.testutil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
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
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.assertion.ArchConditionAssertion;
import com.tngtech.archunit.testutil.assertion.ArchRuleAssertion;
import com.tngtech.archunit.testutil.assertion.ConditionEventsAssertion;
import com.tngtech.archunit.testutil.assertion.DependenciesAssertion;
import com.tngtech.archunit.testutil.assertion.DependencyAssertion;
import com.tngtech.archunit.testutil.assertion.DescribedPredicateAssertion;
import com.tngtech.archunit.testutil.assertion.JavaCodeUnitAssertion;
import com.tngtech.archunit.testutil.assertion.JavaConstructorAssertion;
import com.tngtech.archunit.testutil.assertion.JavaFieldAssertion;
import com.tngtech.archunit.testutil.assertion.JavaFieldsAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMemberAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMembersAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMethodAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMethodsAssertion;
import com.tngtech.archunit.testutil.assertion.JavaPackagesAssertion;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ObjectAssertFactory;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.domain.Formatters.formatMethodSimple;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.resolvedTargetFrom;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.propertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaPackagesAssertion.sortByName;

public class Assertions extends org.assertj.core.api.Assertions {
    public static <T> ArchConditionAssertion<T> assertThat(ArchCondition<T> archCondition) {
        return new ArchConditionAssertion<>(archCondition);
    }

    public static ConditionEventsAssertion assertThat(ConditionEvents events) {
        return new ConditionEventsAssertion(events);
    }

    public static ArchRuleAssertion assertThat(ArchRule rule) {
        return new ArchRuleAssertion(rule);
    }

    public static <T> org.assertj.guava.api.OptionalAssert<T> assertThat(Optional<T> optional) {
        return org.assertj.guava.api.Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    public static <T> DescribedPredicateAssertion<T> assertThat(DescribedPredicate<T> predicate) {
        return new DescribedPredicateAssertion<>(predicate);
    }

    public static JavaClassAssertion assertThat(JavaClass javaClass) {
        return new JavaClassAssertion(javaClass);
    }

    public static JavaClassesAssertion assertThatClasses(Iterable<JavaClass> javaClasses) {
        return new JavaClassesAssertion(javaClasses);
    }

    public static JavaPackagesAssertion assertThatPackages(Iterable<JavaPackage> javaPackages) {
        return new JavaPackagesAssertion(javaPackages);
    }

    public static JavaMemberAssertion<?, ?> assertThat(JavaMember member) {
        return new JavaMemberAssertion<>(member, JavaMemberAssertion.class);
    }

    public static JavaCodeUnitAssertion<?, ?> assertThat(JavaCodeUnit codeUnit) {
        return new JavaCodeUnitAssertion<>(codeUnit, JavaCodeUnitAssertion.class);
    }

    public static JavaMethodAssertion assertThat(JavaMethod method) {
        return new JavaMethodAssertion(method);
    }

    public static JavaConstructorAssertion assertThat(JavaConstructor constructor) {
        return new JavaConstructorAssertion(constructor);
    }

    public static JavaMembersAssertion assertThatMembers(Iterable<? extends JavaMember> members) {
        return new JavaMembersAssertion(members);
    }

    public static JavaMethodsAssertion assertThatMethods(Iterable<JavaMethod> methods) {
        return new JavaMethodsAssertion(methods);
    }

    public static JavaFieldsAssertion assertThatFields(Iterable<JavaField> fields) {
        return new JavaFieldsAssertion(fields);
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

    public static DependencyAssertion assertThatDependency(Dependency dependency) {
        return new DependencyAssertion(dependency);
    }

    public static DependenciesAssertion assertThatDependencies(Iterable<Dependency> dependencies) {
        return new DependenciesAssertion(dependencies);
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
            sortByName(result);
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
            assertThat(TestUtils.namesOf(actual)).as("classes").containsExactlyElementsOf(namesOf(classes));
            for (int i = 0; i < actual.length; i++) {
                assertThat(actual[i]).as("Element %d", i).matches(classes[i]);
            }
        }

        public JavaClassesAssertion contain(Class<?>... classes) {
            contain(ImmutableSet.copyOf(classes));
            return this;
        }

        public void doNotContain(Class<?>... classes) {
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
            assertThat(actual.getPackage().getName()).as("Package of " + actual)
                    .isEqualTo(getExpectedPackageName(clazz));
            assertThat(actual.getPackageName()).as("Package name of " + actual)
                    .isEqualTo(getExpectedPackageName(clazz));
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
            assertThat(actual.getRawType()).as("Type of " + actual)
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
            String expectedPackageName = getExpectedPackageName(clazz);
            assertThat(actual.getPackageName()).as("package").isEqualTo(expectedPackageName);
        }
    }

    private static String getExpectedPackageName(Class<?> clazz) {
        if (!clazz.isArray()) {
            return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        }
        return getExpectedPackageName(clazz.getComponentType());
    }
}
