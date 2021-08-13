package com.tngtech.archunit.core.domain;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodReferenceTarget;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class AccessTargetNewerJavaVersionTest {

    private static class Data_function_resolve_member {
        static class Target {
            String field;

            void method() {
            }
        }
    }

    @Test
    public void function_resolve_member() {
        @SuppressWarnings("unused")
        class Origin {
            String access() {
                Data_function_resolve_member.Target target = new Data_function_resolve_member.Target();
                Supplier<Data_function_resolve_member.Target> supplier = Data_function_resolve_member.Target::new;
                Consumer<Data_function_resolve_member.Target> consumer = Data_function_resolve_member.Target::method;
                target.method();
                return target.field;
            }
        }
        JavaClass targetClass = new ClassFileImporter().importClasses(Origin.class, Data_function_resolve_member.Target.class).get(Data_function_resolve_member.Target.class);
        MethodCallTarget methodCallTarget = findTargetWithType(targetClass.getAccessesToSelf(), MethodCallTarget.class);

        assertThat(AccessTarget.Functions.RESOLVE_MEMBER.apply(methodCallTarget))
                .contains(methodCallTarget.resolveMember().get());
        assertThat(AccessTarget.Functions.RESOLVE.apply(methodCallTarget))
                .isEqualTo(ImmutableSet.of(methodCallTarget.resolveMember().get()));

        assertThat(CodeUnitAccessTarget.Functions.RESOLVE_MEMBER.apply(methodCallTarget))
                .contains(methodCallTarget.resolveMember().get());
        assertThat(CodeUnitAccessTarget.Functions.RESOLVE.apply(methodCallTarget))
                .isEqualTo(ImmutableSet.of(methodCallTarget.resolveMember().get()));

        assertThat(MethodCallTarget.Functions.RESOLVE_MEMBER.apply(methodCallTarget))
                .contains(methodCallTarget.resolveMember().get());
        assertThat(MethodCallTarget.Functions.RESOLVE.apply(methodCallTarget))
                .isEqualTo(ImmutableSet.of(methodCallTarget.resolveMember().get()));

        MethodReferenceTarget methodReferenceTarget = findTargetWithType(targetClass.getAccessesToSelf(), MethodReferenceTarget.class);

        assertThat(MethodReferenceTarget.Functions.RESOLVE_MEMBER.apply(methodReferenceTarget))
                .contains(methodReferenceTarget.resolveMember().get());

        ConstructorCallTarget constructorCallTarget = findTargetWithType(targetClass.getAccessesToSelf(), ConstructorCallTarget.class);

        assertThat(ConstructorCallTarget.Functions.RESOLVE_MEMBER.apply(constructorCallTarget))
                .contains(constructorCallTarget.resolveMember().get());
        assertThat(ConstructorCallTarget.Functions.RESOLVE.apply(constructorCallTarget))
                .isEqualTo(ImmutableSet.of(constructorCallTarget.resolveMember().get()));

        ConstructorReferenceTarget constructorReferenceTarget = findTargetWithType(targetClass.getAccessesToSelf(), ConstructorReferenceTarget.class);

        assertThat(ConstructorReferenceTarget.Functions.RESOLVE_MEMBER.apply(constructorReferenceTarget))
                .contains(constructorReferenceTarget.resolveMember().get());

        FieldAccessTarget fieldAccessTarget = findTargetWithType(targetClass.getAccessesToSelf(), FieldAccessTarget.class);

        assertThat(FieldAccessTarget.Functions.RESOLVE_MEMBER.apply(fieldAccessTarget))
                .contains(fieldAccessTarget.resolveMember().get());
        assertThat(FieldAccessTarget.Functions.RESOLVE.apply(fieldAccessTarget))
                .isEqualTo(ImmutableSet.of(fieldAccessTarget.resolveMember().get()));

    }

    @SuppressWarnings("unchecked")
    private <T extends AccessTarget> T findTargetWithType(Set<JavaAccess<?>> set, Class<T> type) {
        for (JavaAccess<?> access : set) {
            if (type.isInstance(access.getTarget())) {
                return (T) access.getTarget();
            }
        }
        throw new AssertionError(String.format("Set %s does not contain element of type %s", set, type.getName()));
    }
}
