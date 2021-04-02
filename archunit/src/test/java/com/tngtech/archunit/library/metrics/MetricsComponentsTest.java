package com.tngtech.archunit.library.metrics;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.metrics.testobjects.lakos.pkg1.SomeTestClass1;
import com.tngtech.archunit.library.metrics.testobjects.lakos.pkg1.sub.SomeSubTestClass;
import com.tngtech.archunit.library.metrics.testobjects.lakos.pkg2.SomeTestClass2;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricsComponentsTest {

    @Test
    public void rejects_components_with_non_unique_identifier() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                MetricsComponents.of(
                        MetricsComponent.of("duplicate-key"),
                        MetricsComponent.of("duplicate-key"));
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple entries with same key")
                .hasMessageContaining("duplicate-key");
    }

    @Test
    public void creates_components_from_Java_packages() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(SomeTestClass1.class, SomeTestClass2.class);
        Set<JavaPackage> packages = ImmutableSet.of(
                classes.get(SomeTestClass1.class).getPackage(),
                classes.get(SomeTestClass2.class).getPackage());

        MetricsComponents<JavaClass> components = MetricsComponents.fromPackages(packages);

        assertThat(components).as("components").hasSize(2);

        MetricsComponent<JavaClass> component = components.tryGetComponent(SomeTestClass1.class.getPackage().getName()).get();
        assertThatTypes(component).as("elements of component " + component.getIdentifier())
                .matchInAnyOrder(SomeTestClass1.class, SomeSubTestClass.class);

        component = components.tryGetComponent(SomeTestClass2.class.getPackage().getName()).get();
        assertThatTypes(component).as("elements of component " + component.getIdentifier())
                .matchInAnyOrder(SomeTestClass2.class);
    }

    @Test
    public void creates_components_from_Java_classes() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(SomeTestClass1.class, SomeTestClass2.class);

        MetricsComponents<JavaClass> components = MetricsComponents.fromClasses(classes);

        assertThat(components).as("components").hasSize(3);

        MetricsComponent<JavaClass> component = components.tryGetComponent(SomeTestClass1.class.getName()).get();
        assertThatTypes(component).as("elements of component " + component.getIdentifier())
                .matchInAnyOrder(SomeTestClass1.class);

        component = components.tryGetComponent(SomeSubTestClass.class.getName()).get();
        assertThatTypes(component).as("elements of component " + component.getIdentifier())
                .matchInAnyOrder(SomeSubTestClass.class);

        component = components.tryGetComponent(SomeTestClass2.class.getName()).get();
        assertThatTypes(component).as("elements of component " + component.getIdentifier())
                .matchInAnyOrder(SomeTestClass2.class);
    }

    @Test
    public void creates_components_from_elements_partitioned_by_function() {
        MetricsComponents<Integer> components = MetricsComponents.from(ImmutableSet.of(1, 2, 3, 4), new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return input % 2 == 0 ? "even" : "odd";
            }
        });

        assertThat(components).as("components").hasSize(2);

        assertThat(components.tryGetComponent("even").get()).containsOnly(2, 4);
    }
}
