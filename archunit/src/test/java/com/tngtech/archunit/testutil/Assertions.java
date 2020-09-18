package com.tngtech.archunit.testutil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
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
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
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
import com.tngtech.archunit.testutil.assertion.JavaClassDescriptorAssertion;
import com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion;
import com.tngtech.archunit.testutil.assertion.JavaCodeUnitAssertion;
import com.tngtech.archunit.testutil.assertion.JavaConstructorAssertion;
import com.tngtech.archunit.testutil.assertion.JavaFieldAssertion;
import com.tngtech.archunit.testutil.assertion.JavaFieldsAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMemberAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMembersAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMethodAssertion;
import com.tngtech.archunit.testutil.assertion.JavaMethodsAssertion;
import com.tngtech.archunit.testutil.assertion.JavaPackagesAssertion;
import com.tngtech.archunit.testutil.assertion.JavaTypeAssertion;
import com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion;
import com.tngtech.archunit.testutil.assertion.JavaTypesAssertion;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ObjectAssertFactory;

import static com.google.common.base.Strings.emptyToNull;
import static com.tngtech.archunit.core.domain.Formatters.formatMethodSimple;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.resolvedTargetFrom;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;

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

    public static JavaTypeAssertion assertThatType(JavaType javaType) {
        return new JavaTypeAssertion(javaType);
    }

    public static JavaTypeVariableAssertion assertThatTypeVariable(JavaTypeVariable typeVariable) {
        return new JavaTypeVariableAssertion(typeVariable);
    }

    public static JavaTypesAssertion assertThatTypes(Iterable<? extends JavaType> javaTypes) {
        return new JavaTypesAssertion(javaTypes);
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

    public static JavaTypesAssertion assertThat(JavaType[] javaTypes) {
        return new JavaTypesAssertion(javaTypes);
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

    public static JavaClassDescriptorAssertion assertThat(JavaClassDescriptor javaClassDescriptor) {
        return new JavaClassDescriptorAssertion(javaClassDescriptor);
    }

    public static ThrowsDeclarationAssertion assertThat(ThrowsDeclaration<?> throwsDeclaration) {
        return new ThrowsDeclarationAssertion(throwsDeclaration);
    }

    public static ThrowsClauseAssertion assertThat(ThrowsClause<?> throwsClause) {
        return new ThrowsClauseAssertion(throwsClause);
    }

    public static JavaAnnotationAssertion assertThatAnnotation(JavaAnnotation<?> annotation) {
        return new JavaAnnotationAssertion(annotation);
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

        public static class Step2 {
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

    public static class JavaClassListAssertion
            extends AbstractListAssert<JavaClassListAssertion, List<? extends JavaClass>, JavaClass, ObjectAssert<JavaClass>> {
        private JavaClassListAssertion(JavaClassList javaClasses) {
            super(javaClasses, JavaClassListAssertion.class);
        }

        public void matches(Class<?>... classes) {
            assertThat(actual).as("JavaClasses").hasSize(classes.length);
            for (int i = 0; i < actual.size(); i++) {
                assertThatType(actual.get(i)).as("Element %d", i).matches(classes[i]);
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
            assertThat(actual).as(describePartialAssertion()).isNotNull();
            assertThat(actual.getDeclaringClass().getName()).as(describePartialAssertion("type")).isEqualTo(enumConstant.getDeclaringClass().getName());
            assertThat(actual.name()).as(describePartialAssertion("name")).isEqualTo(enumConstant.name());
        }

        private String describePartialAssertion() {
            return describePartialAssertion("");
        }

        private String describePartialAssertion(String partialAssertionDescription) {
            return Joiner.on(": ").skipNulls().join(emptyToNull(descriptionText()), emptyToNull(partialAssertionDescription));
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
            assertThatType(actual.getRawType()).as("Type of " + actual)
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

}
