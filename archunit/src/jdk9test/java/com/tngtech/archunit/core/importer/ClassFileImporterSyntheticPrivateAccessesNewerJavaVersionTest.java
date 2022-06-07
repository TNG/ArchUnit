package com.tngtech.archunit.core.importer;

import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ClassFileImporterSyntheticPrivateAccessesNewerJavaVersionTest {

    @Test
    public void removes_synthetic_access_methods_from_JavaClass() {
        @SuppressWarnings("unused")
        class Target {
            private int field;

            private void method() {
            }
        }

        @SuppressWarnings({"unused", "UnusedAssignment", "Convert2MethodRef"})
        class Origin {
            void access(Target target) {
                int number = target.field;
                target.field = 42;
                target.field++;
                target.method();
                new Target();

                Runnable runnable = target::method;
                Supplier<Target> supplier = Target::new;
                Supplier<Integer> supplier2 = () -> target.field;
                runnable = () -> target.field = 42;
                runnable = () -> target.field++;
                runnable = () -> target.method();
                supplier = () -> new Target();
                Supplier<Runnable> supplier3 = () -> target::method;
                Supplier<Supplier<Target>> supplier4 = () -> Target::new;
            }
        }

        Set<JavaMethod> methods = new ClassFileImporter().importClasses(Origin.class, Target.class).get(Target.class).getMethods();

        assertThat(methods).extracting(JavaMethod::getName).as("method names")
                .noneSatisfy(name -> assertThat(name).matches("access\\$\\d+"));
    }
}
