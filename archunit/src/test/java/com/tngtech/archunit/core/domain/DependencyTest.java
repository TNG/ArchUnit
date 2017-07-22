package com.tngtech.archunit.core.domain;

import com.google.common.base.MoreObjects;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static org.assertj.core.api.Assertions.assertThat;

public class DependencyTest {
    @Test
    public void Dependency_from_access() {
        JavaMethodCall call = simulateCall().from(getClass(), "toString").to(Object.class, "toString");

        Dependency dependency = Dependency.from(call);
        assertThat(dependency.getTargetClass()).as("target class").isEqualTo(call.getTargetOwner());
        assertThat(dependency.getDescription())
                .as("description").isEqualTo(call.getDescription());
    }

    @Test
    public void Dependency_from_origin_and_target() {
        JavaClass target = javaClassViaReflection(Object.class);

        Dependency dependency = Dependency.fromExtends(javaClassViaReflection(getClass()), target);
        assertDependency(target, dependency, "extends");

        dependency = Dependency.fromImplements(javaClassViaReflection(getClass()), target);
        assertDependency(target, dependency, "implements");
    }

    private void assertDependency(JavaClass target, Dependency dependency, String dependencyType) {
        assertThat(dependency.getTargetClass()).as("target class").isEqualTo(target);
        assertThat(dependency.getDescription()).as("description").isEqualTo(
                getClass().getName() + " " + dependencyType + " " + Object.class.getName() +
                        " in (" + getClass().getSimpleName() + ".java:0)");
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}