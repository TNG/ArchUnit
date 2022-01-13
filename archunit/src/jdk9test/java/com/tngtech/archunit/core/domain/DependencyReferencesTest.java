package com.tngtech.archunit.core.domain;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class DependencyReferencesTest {

    static class Data_dependency_from_access {
        static class Target {
            String field;

            void callMe() {
            }
        }
    }

    @DataProvider
    public static Object[][] data_dependency_from_access() {
        @SuppressWarnings("unused")
        class Origin {
            String fieldAccess(Data_dependency_from_access.Target target) {
                return target.field;
            }

            void constructorCall() {
                new Data_dependency_from_access.Target();
            }

            void methodCall(Data_dependency_from_access.Target target) {
                target.callMe();
            }

            Supplier<Data_dependency_from_access.Target> constructorReference() {
                return Data_dependency_from_access.Target::new;
            }

            Consumer<Data_dependency_from_access.Target> methodReference() {
                return Data_dependency_from_access.Target::callMe;
            }
        }
        JavaClass origin = new ClassFileImporter().importClasses(Origin.class, Data_dependency_from_access.Target.class).get(Origin.class);
        return testForEach(
                getOnlyElement(origin.getMethod("fieldAccess", Data_dependency_from_access.Target.class).getFieldAccesses()),
                getOnlyElement(origin.getMethod("constructorCall").getConstructorCallsFromSelf()),
                getOnlyElement(origin.getMethod("methodCall", Data_dependency_from_access.Target.class).getMethodCallsFromSelf()),
                getOnlyElement(origin.getMethod("constructorReference").getConstructorReferencesFromSelf()),
                getOnlyElement(origin.getMethod("methodReference").getMethodReferencesFromSelf())
        );
    }

    @Test
    @UseDataProvider
    public void test_dependency_from_access(JavaAccess<?> access) {
        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAccess(access));
        assertThatType(dependency.getTargetClass()).as("target class").isEqualTo(access.getTargetOwner());
        assertThat(dependency.getDescription()).as("description").isEqualTo(access.getDescription());
    }
}
