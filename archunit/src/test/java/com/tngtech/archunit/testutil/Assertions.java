package com.tngtech.archunit.testutil;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
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
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.assertion.AccessToFieldAssertion;
import com.tngtech.archunit.testutil.assertion.AccessesAssertion;
import com.tngtech.archunit.testutil.assertion.ArchConditionAssertion;
import com.tngtech.archunit.testutil.assertion.ArchRuleAssertion;
import com.tngtech.archunit.testutil.assertion.CodeUnitAccessAssertion;
import com.tngtech.archunit.testutil.assertion.ConditionEventsAssertion;
import com.tngtech.archunit.testutil.assertion.DependenciesAssertion;
import com.tngtech.archunit.testutil.assertion.DependencyAssertion;
import com.tngtech.archunit.testutil.assertion.DescribedPredicateAssertion;
import com.tngtech.archunit.testutil.assertion.ExpectedAccessCreation;
import com.tngtech.archunit.testutil.assertion.InstanceofChecksAssertion;
import com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion;
import com.tngtech.archunit.testutil.assertion.JavaAnnotationsAssertion;
import com.tngtech.archunit.testutil.assertion.JavaClassAssertion;
import com.tngtech.archunit.testutil.assertion.JavaClassDescriptorAssertion;
import com.tngtech.archunit.testutil.assertion.JavaCodeUnitAssertion;
import com.tngtech.archunit.testutil.assertion.JavaConstructorAssertion;
import com.tngtech.archunit.testutil.assertion.JavaEnumConstantAssertion;
import com.tngtech.archunit.testutil.assertion.JavaEnumConstantsAssertion;
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
import com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion;
import com.tngtech.archunit.testutil.assertion.ThrowsClauseAssertion;
import com.tngtech.archunit.testutil.assertion.ThrowsDeclarationAssertion;
import com.tngtech.archunit.testutil.assertion.TryCatchBlockAssertion;

public class Assertions extends org.assertj.core.api.Assertions {
    public static <T> ArchConditionAssertion<T> assertThat(ArchCondition<T> archCondition) {
        return new ArchConditionAssertion<>(archCondition);
    }

    public static ConditionEventsAssertion assertThat(ConditionEvents events) {
        return new ConditionEventsAssertion(events);
    }

    public static ArchRuleAssertion assertThatRule(ArchRule rule) {
        return new ArchRuleAssertion(rule);
    }

    public static <T> DescribedPredicateAssertion<T> assertThat(DescribedPredicate<T> predicate) {
        return new DescribedPredicateAssertion<>(predicate);
    }

    public static JavaTypeAssertion assertThatType(JavaType javaType) {
        return new JavaTypeAssertion(javaType);
    }

    public static JavaTypeVariableAssertion assertThatTypeVariable(JavaTypeVariable<?> typeVariable) {
        return new JavaTypeVariableAssertion(typeVariable);
    }

    public static JavaTypesAssertion assertThatTypes(Iterable<? extends JavaType> javaTypes) {
        return new JavaTypesAssertion(javaTypes);
    }

    public static JavaTypesAssertion assertThatTypeErasuresOf(Iterable<? extends JavaType> javaTypes) {
        ImmutableList.Builder<JavaClass> erasures = ImmutableList.builder();
        for (JavaType javaType : javaTypes) {
            erasures.add(javaType.toErasure());
        }
        return new JavaTypesAssertion(erasures.build());
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

    public static JavaCodeUnitAssertion<JavaCodeUnit, ?> assertThatCodeUnit(JavaCodeUnit codeUnit) {
        return new JavaCodeUnitAssertion<>(codeUnit, JavaCodeUnitAssertion.class);
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

    public static JavaClassAssertion assertThat(JavaClass javaClass) {
        return new JavaClassAssertion(javaClass);
    }

    public static JavaFieldAssertion assertThat(FieldAccessTarget target) {
        return assertThat(target.resolveMember().get());
    }

    public static JavaFieldAssertion assertThat(JavaField field) {
        return new JavaFieldAssertion(field);
    }

    public static ReferencedClassObjectsAssertion assertThatReferencedClassObjects(Set<ReferencedClassObject> referencedClassObjects) {
        return new ReferencedClassObjectsAssertion(referencedClassObjects);
    }

    public static InstanceofChecksAssertion assertThatInstanceofChecks(Set<InstanceofCheck> instanceofChecks) {
        return new InstanceofChecksAssertion(instanceofChecks);
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

    public static ThrowsClauseAssertion assertThatThrowsClause(ThrowsClause<?> throwsClause) {
        return new ThrowsClauseAssertion(throwsClause);
    }

    public static JavaAnnotationAssertion assertThatAnnotation(JavaAnnotation<?> annotation) {
        return new JavaAnnotationAssertion(annotation);
    }

    public static JavaAnnotationsAssertion assertThatAnnotations(Set<? extends JavaAnnotation<?>> annotation) {
        return new JavaAnnotationsAssertion(annotation);
    }

    @SuppressWarnings("unchecked") // covariant
    public static AccessesAssertion assertThatAccesses(Collection<? extends JavaAccess<?>> accesses) {
        return new AccessesAssertion((Collection<JavaAccess<?>>) accesses);
    }

    public static DependencyAssertion assertThatDependency(Dependency dependency) {
        return new DependencyAssertion(dependency);
    }

    public static DependenciesAssertion assertThatDependencies(Collection<Dependency> dependencies) {
        return new DependenciesAssertion(dependencies);
    }

    public static ExpectedAccessCreation expectedAccess() {
        return new ExpectedAccessCreation();
    }

    public static AccessToFieldAssertion assertThatAccess(JavaFieldAccess access) {
        return new AccessToFieldAssertion(access);
    }

    public static CodeUnitAccessAssertion assertThatAccess(JavaCodeUnitAccess<?> reference) {
        return new CodeUnitAccessAssertion(reference);
    }

    public static CodeUnitAccessAssertion assertThatCall(JavaMethodCall call) {
        return assertThatAccess(call);
    }

    public static CodeUnitAccessAssertion assertThatCall(JavaConstructorCall call) {
        return assertThatAccess(call);
    }

    public static TryCatchBlockAssertion assertThatTryCatchBlock(TryCatchBlock tryCatchBlock) {
        return new TryCatchBlockAssertion(tryCatchBlock);
    }
}
