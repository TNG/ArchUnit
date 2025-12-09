package com.tngtech.archunit.core.importer;

import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccesses;
import static com.tngtech.archunit.testutil.Assertions.expectedAccess;
import static com.tngtech.archunit.testutil.assertion.AccessesAssertion.access;
import static java.util.stream.Collectors.toSet;

public class ClassFileImporterSyntheticPrivateAccessesTest {
    @Test
    public void imports_private_get_field() {
        @SuppressWarnings("unused")
        class Target {
            private String field;
        }
        @SuppressWarnings("unused")
        class Origin {
            String access(Target target) {
                return target.field;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(GET, Target.class, "field")
        );
    }

    @Test
    public void imports_private_set_field() {
        @SuppressWarnings("unused")
        class Target {
            private String field;
        }
        @SuppressWarnings("unused")
        class Origin {
            void access(Target target) {
                target.field = "new";
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(SET, Target.class, "field")
        );
    }

    @Test
    public void imports_private_increment_field() {
        @SuppressWarnings("unused")
        class Target {
            private int field;
        }
        @SuppressWarnings("unused")
        class Origin {
            void access(Target target) {
                target.field++;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(GET, Target.class, "field"),
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(SET, Target.class, "field")
        );
    }

    @Test
    public void imports_private_call_method() {
        @SuppressWarnings("unused")
        class Target {
            private void method() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            void access(Target target) {
                target.method();
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .to(Target.class, "method")
        );
    }

    private static class Data_of_imports_private_call_constructor {
        @SuppressWarnings("unused")
        static class Target {
            private Target() {
            }
        }
    }

    @Test
    public void imports_private_call_constructor() {
        @SuppressWarnings("unused")
        class Origin {
            void access(Data_of_imports_private_call_constructor.Target target) {
                new Data_of_imports_private_call_constructor.Target();
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Data_of_imports_private_call_constructor.Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        // For complexity's sake we keep the constructor with the synthetic parameter for now and see if this causes any real problem for users
                        .to(Data_of_imports_private_call_constructor.Target.class, CONSTRUCTOR_NAME)
        );
    }

    @Test
    public void imports_private_method_reference() {
        @SuppressWarnings("unused")
        class Target {
            private void method() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            Runnable access(Target target) {
                return target::method;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .to(Target.class, "method")
        );
    }

    private static class Data_of_imports_private_constructor_reference {
        @SuppressWarnings("unused")
        static class Target {
            private Target() {
            }
        }
    }

    @Test
    public void imports_private_constructor_reference() {
        @SuppressWarnings("unused")
        class Origin {
            Supplier<Data_of_imports_private_constructor_reference.Target> access() {
                return Data_of_imports_private_constructor_reference.Target::new;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Data_of_imports_private_constructor_reference.Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        // For complexity's sake we keep the constructor with the synthetic parameter for now and see if this causes any real problem for users
                        .to(Data_of_imports_private_constructor_reference.Target.class, CONSTRUCTOR_NAME)
        );
    }

    @Test
    public void imports_private_get_field_from_lambda() {
        @SuppressWarnings("unused")
        class Target {
            private String field;
        }
        @SuppressWarnings("unused")
        class Origin {
            Supplier<String> access(Target target) {
                return () -> target.field;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(GET, Target.class, "field")
                        .declaredInLambda()
        );
    }

    @Test
    public void imports_private_set_field_from_lambda() {
        @SuppressWarnings("unused")
        class Target {
            private String field;
        }
        @SuppressWarnings("unused")
        class Origin {
            Runnable access(Target target) {
                return () -> target.field = "new";
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(SET, Target.class, "field")
                        .declaredInLambda()
        );
    }

    @Test
    public void imports_private_increment_field_from_lambda() {
        @SuppressWarnings("unused")
        class Target {
            private int field;
        }
        @SuppressWarnings("unused")
        class Origin {
            Runnable access(Target target) {
                return () -> target.field++;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(GET, Target.class, "field")
                        .declaredInLambda(),
                expectedAccess()
                        .from(Origin.class, "access")
                        .toField(SET, Target.class, "field")
                        .declaredInLambda()
        );
    }

    @Test
    public void imports_private_call_method_from_lambda() {
        @SuppressWarnings("unused")
        class Target {
            private void method() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            @SuppressWarnings("Convert2MethodRef")
            Runnable access(Target target) {
                return () -> target.method();
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .to(Target.class, "method")
                        .declaredInLambda()
        );
    }

    private static class Data_of_imports_private_call_constructor_from_lambda {
        @SuppressWarnings("unused")
        static class Target {
            private Target() {
            }
        }
    }

    @Test
    public void imports_private_call_constructor_from_lambda() {
        @SuppressWarnings("unused")
        class Origin {
            @SuppressWarnings("Convert2MethodRef")
            Supplier<Data_of_imports_private_call_constructor_from_lambda.Target> access(Data_of_imports_private_call_constructor_from_lambda.Target target) {
                return () -> new Data_of_imports_private_call_constructor_from_lambda.Target();
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Data_of_imports_private_call_constructor_from_lambda.Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        // For complexity's sake we keep the constructor with the synthetic parameter for now and see if this causes any real problem for users
                        .to(Data_of_imports_private_call_constructor_from_lambda.Target.class, CONSTRUCTOR_NAME)
                        .declaredInLambda()
        );
    }

    @Test
    public void imports_private_method_reference_from_lambda() {
        @SuppressWarnings("unused")
        class Target {
            private void method() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            Supplier<Runnable> access(Target target) {
                return () -> target::method;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        .to(Target.class, "method")
                        .declaredInLambda()
        );
    }

    @Test
    public void imports_multiple_accesses_to_same_private_field() {
        @SuppressWarnings("unused")
        class Target {
            private String field;
        }
        @SuppressWarnings("unused")
        class Origin {
            String first(Target target) {
                return target.field;
            }

            String second(Target target) {
                return target.field;
            }
        }

        JavaClass origin = new ClassFileImporter().importClasses(Target.class, Origin.class).get(Origin.class);

        assertThatAccesses(origin.getAccessesFromSelf())
                .contain(expectedAccess()
                        .from(Origin.class, "first")
                        .toField(GET, Target.class, "field")
                )
                .contain(expectedAccess()
                        .from(Origin.class, "second")
                        .toField(GET, Target.class, "field"));
    }

    @Test
    public void imports_multiple_calls_to_same_private_method() {
        @SuppressWarnings("unused")
        class Target {
            private void method() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            void first(Target target) {
                target.method();
            }

            void second(Target target) {
                target.method();
            }
        }

        JavaClass origin = new ClassFileImporter().importClasses(Target.class, Origin.class).get(Origin.class);

        assertThatAccesses(origin.getAccessesFromSelf())
                .contain(expectedAccess()
                        .from(Origin.class, "first")
                        .to(Target.class, "method")
                )
                .contain(expectedAccess()
                        .from(Origin.class, "second")
                        .to(Target.class, "method"));
    }

    private static class Data_of_imports_private_constructor_reference_from_lambda {
        @SuppressWarnings("unused")
        static class Target {
            private Target() {
            }
        }
    }

    @Test
    public void imports_private_constructor_reference_from_lambda() {
        @SuppressWarnings("unused")
        class Origin {
            Supplier<Supplier<Data_of_imports_private_constructor_reference_from_lambda.Target>> access() {
                return () -> Data_of_imports_private_constructor_reference_from_lambda.Target::new;
            }
        }

        Set<JavaAccess<?>> accesses = importRelevantAccesses(Origin.class, Data_of_imports_private_constructor_reference_from_lambda.Target.class);

        assertThatAccesses(accesses).containOnly(
                expectedAccess()
                        .from(Origin.class, "access")
                        // For complexity's sake we keep the constructor with the synthetic parameter for now and see if this causes any real problem for users
                        .to(Data_of_imports_private_constructor_reference_from_lambda.Target.class, CONSTRUCTOR_NAME)
                        .declaredInLambda()
        );
    }

    /**
     * This is a special case, because for += concatenation of an outer string from an inner class
     * the compiler may create a synthetic `access$123` method that creates a new `StringBuilder()` (depending on the JDK version).
     * Before this we wrongly assumed that all that happens from such `access$123` methods
     * are field accesses and method calls, but no constructor calls.
     */
    @Test
    public void imports_synthetic_access_from_string_concatenation() {
        @SuppressWarnings("unused")
        class Outer {
            private String outerPrivateString = "Hello";

            class Inner {
                private void stringConcat() {
                    outerPrivateString += ", world!";
                }
            }
        }

        Set<JavaFieldAccess> fieldAccesses = new ClassFileImporter().importClasses(Outer.class, Outer.Inner.class)
                .get(Outer.Inner.class)
                .getFieldAccessesFromSelf();

        assertThatAccesses(fieldAccesses)
                .contain(access()
                        .fromOrigin(Outer.Inner.class, "stringConcat")
                        .toTarget(Outer.class, "outerPrivateString")
                        .withAccessType(GET))
                .contain(access()
                        .fromOrigin(Outer.Inner.class, "stringConcat")
                        .toTarget(Outer.class, "outerPrivateString")
                        .withAccessType(SET));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Set<JavaAccess<?>> importRelevantAccesses(Class<?> origin, Class<?> target) {
        return new ClassFileImporter().importClasses(origin, target).get(origin).getMethods().stream()
                .filter(m -> "access".equals(m.getName()))
                .findFirst()
                .get()
                .getAccessesFromSelf()
                .stream().filter(a -> !a.getTargetOwner().getName().startsWith("java."))
                .collect(toSet());
    }
}
