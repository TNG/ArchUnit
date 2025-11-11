package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.FilterInputStream;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaConstructorReference;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.CheckingInstanceofFromLambda;
import com.tngtech.archunit.core.importer.testexamples.referencedclassobjects.ReferencingClassObjectsFromLambda;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporterTestUtils.isLambdaMethodName;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccess;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccesses;
import static com.tngtech.archunit.testutil.Assertions.assertThatCall;
import static com.tngtech.archunit.testutil.Assertions.assertThatInstanceofChecks;
import static com.tngtech.archunit.testutil.Assertions.assertThatReferencedClassObjects;
import static com.tngtech.archunit.testutil.assertion.AccessesAssertion.access;
import static com.tngtech.archunit.testutil.assertion.InstanceofChecksAssertion.instanceofCheck;
import static com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion.referencedClassObject;
import static com.tngtech.archunit.testutil.assertion.TryCatchBlockAssertion.tryCatchBlock;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.stream.Collectors.toSet;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterLambdaDependenciesTest {
    @Test
    public void imports_method_call_from_lambda_without_parameter() {
        class Target {
            void target() {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Runnable call() {
                return () -> target.target();
            }
        }

        JavaMethodCall call = getOnlyElement(
                new ClassFileImporter().importClasses(Target.class, Caller.class)
                        .get(Caller.class)
                        .getMethod("call")
                        .getMethodCallsFromSelf());

        assertThatCall(call).isFrom("call").isTo(Target.class, "target");
    }

    @Test
    public void imports_constructor_call_from_lambda_without_parameter() {
        class Target {
        }

        @SuppressWarnings({"unused", "Convert2MethodRef"})
        class Caller {
            Runnable call() {
                return () -> new Target();
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        JavaConstructorCall call = getOnlyElement(
                filterOriginByName(classes.get(Caller.class).getConstructorCallsFromSelf(), "call"));

        assertThatCall(call).isFrom("call").isTo(Target.class, CONSTRUCTOR_NAME, getClass());
    }

    @Test
    public void imports_field_access_from_lambda_without_parameter() {
        class Target {
        }

        @SuppressWarnings("unused")
        class Caller {
            Target target;

            Consumer<Target> call() {
                return (target) -> this.target = target;
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        JavaFieldAccess access = getOnlyElement(filterOriginByName(classes.get(Caller.class).getFieldAccessesFromSelf(), "call"));

        assertThatAccess(access).isFrom("call").isTo(Caller.class, "target");
    }

    @Test
    public void imports_method_reference_from_lambda_without_parameter() {
        class Target {
            void target() {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            Supplier<Consumer<Target>> call() {
                return () -> Target::target;
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        JavaMethodReference reference = getOnlyElement(classes.get(Caller.class).getMethodReferencesFromSelf());

        assertThatAccess(reference).isFrom("call").isTo(Target.class, "target");
    }

    private static class Data_of_imports_constructor_reference_from_lambda_without_parameter {
        static class Target {
        }
    }

    @Test
    public void imports_constructor_reference_from_lambda_without_parameter() {
        @SuppressWarnings("unused")
        class Caller {
            Supplier<Supplier<Data_of_imports_constructor_reference_from_lambda_without_parameter.Target>> call() {
                return () -> Data_of_imports_constructor_reference_from_lambda_without_parameter.Target::new;
            }
        }

        JavaClasses classes = new ClassFileImporter()
                .importClasses(Data_of_imports_constructor_reference_from_lambda_without_parameter.Target.class, Caller.class);
        JavaConstructorReference reference = getOnlyElement(classes.get(Caller.class).getConstructorReferencesFromSelf());

        assertThatAccess(reference).isFrom("call")
                .isTo(Data_of_imports_constructor_reference_from_lambda_without_parameter.Target.class, CONSTRUCTOR_NAME);
    }

    @Test
    public void imports_method_call_from_lambda_with_parameter() {
        @SuppressWarnings("unused")
        class Target {
            void target(String param) {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Consumer<String> call() {
                return s -> target.target(s);
            }
        }

        JavaMethodCall call = getOnlyElement(
                new ClassFileImporter().importClasses(Target.class, Caller.class)
                        .get(Caller.class)
                        .getMethod("call")
                        .getMethodCallsFromSelf());

        assertThatCall(call).isFrom("call").isTo(Target.class, "target", String.class);
    }

    @Test
    public void imports_method_call_from_lambda_with_return_types() {
        @SuppressWarnings("unused")
        class Target {
            String target() {
                return "";
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Supplier<String> call() {
                return () -> target.target();
            }
        }

        JavaMethodCall call = getOnlyElement(
                new ClassFileImporter().importClasses(Target.class, Caller.class)
                        .get(Caller.class)
                        .getMethod("call")
                        .getMethodCallsFromSelf());

        assertThatCall(call).isFrom("call").isTo(Target.class, "target");
    }

    @Test
    public void imports_method_call_from_lambda_with_overloads() {
        @SuppressWarnings("unused")
        class Target {
            void target() {
            }

            void target(String param) {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Consumer<String> call() {
                return s -> target.target();
            }

            Consumer<String> callParam() {
                return s -> target.target(s);
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        Set<JavaMethodCall> calls = classes.get(Caller.class).getMethodCallsFromSelf();

        assertThatCall(singleByName(calls, "call")).isFrom("call").isTo(Target.class, "target");
        assertThatCall(singleByName(calls, "callParam")).isFrom("callParam").isTo(Target.class, "target", String.class);
    }

    @Test
    public void imports_multiple_method_calls_from_single_lambda() {
        class Target {
            void target() {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Runnable call() {
                return () -> {
                    target.target();
                    target.target();
                };
            }
        }

        Set<JavaMethodCall> calls = new ClassFileImporter().importClasses(Target.class, Caller.class)
                .get(Caller.class)
                .getMethod("call")
                .getMethodCallsFromSelf();

        assertThat(calls).hasSize(2);
        calls.forEach(call -> assertThatCall(call).isFrom("call").isTo(Target.class, "target"));
    }

    @Test
    public void imports_multiple_method_calls_from_multiple_lambda() {
        @SuppressWarnings("unused")
        class Target {
            Target target() {
                return this;
            }

            Target target(String s) {
                return this;
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Runnable call1() {
                return () -> target.target().target().target();
            }

            Function<String, Target> call2() {
                return s -> target.target(s).target();
            }
        }

        Set<JavaMethodCall> calls = new ClassFileImporter().importClasses(Target.class, Caller.class)
                .get(Caller.class)
                .getMethodCallsFromSelf()
                .stream()
                .filter(call -> call.getOrigin().isMethod())
                .collect(toSet());
        assertThat(calls).hasSize(5);

        assertThat(filterOriginByName(calls, "call1"))
                .haveAtLeast(3, access().fromOrigin(Caller.class, "call1").toTarget(Target.class, "target"))
                .hasSize(3);

        assertThat(filterOriginByName(calls, "call2"))
                .extracting(JavaMethodCall::getTarget)
                .areExactly(1, targetCodeUnit("target"))
                .areExactly(1, targetCodeUnit("target", String.class))
                .hasSize(2);
    }

    @Test
    public void imports_multiple_method_calls_from_multiple_lambda_in_one_method() {
        @SuppressWarnings("unused")
        class Target {
            Target target(String s) {
                return this;
            }

            Target target(int i) {
                return this;
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            Function<String, Target> call(int i) {
                Function<Target, Target> function = t -> t.target(i);
                return s -> target.target(s);
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        Set<JavaMethodCall> calls = classes.get(Caller.class).getMethodCallsFromSelf();

        assertThat(filterOriginByName(calls, "call"))
                .extracting(JavaMethodCall::getTarget)
                .areExactly(1, targetCodeUnit("target", String.class))
                .areExactly(1, targetCodeUnit("target", int.class))
                .hasSize(2);
    }

    @Test
    public void imports_method_call_from_lambdas_from_multiple_callers() {
        @SuppressWarnings("unused")
        class Target {
            String target() {
                return "";
            }
        }

        @SuppressWarnings("unused")
        class Caller1 {
            private Target target;

            Supplier<String> call() {
                return () -> target.target();
            }
        }

        @SuppressWarnings("unused")
        class Caller2 {
            private Target target;

            Supplier<String> call() {
                return () -> target.target();
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller1.class, Caller2.class);

        JavaMethodCall call1 = getOnlyElement(classes.get(Caller1.class)
                .getMethod("call")
                .getMethodCallsFromSelf());
        assertThatCall(call1).isFrom("call").isTo(Target.class, "target");

        JavaMethodCall call2 = getOnlyElement(classes.get(Caller2.class)
                .getMethod("call")
                .getMethodCallsFromSelf());
        assertThatCall(call2).isFrom("call").isTo(Target.class, "target");
    }

    @Test
    public void imports_complex_combination_of_lambda_accesses_to_nested_class() {
        @SuppressWarnings("unused")
        class Caller {
            private Target target;

            void call() {
                Supplier<Supplier<Supplier<Target>>> quiteNestedConstructorCallSupplier = () -> () -> Target::new;
                Function<String, Supplier<Supplier<Supplier<Target>>>> quiteNestedMethodCallSupplier = s -> () -> () -> () -> target.inner.method(s);
                Supplier<Supplier<Supplier<String>>> quiteNestedFieldSupplier = () -> () -> () -> target.inner.field;
            }

            class Target {
                final Inner inner = new Inner();

                class Inner {
                    String field;

                    Target method(String s) {
                        return Target.this;
                    }
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Caller.class, Caller.Target.class);
        Set<JavaAccess<?>> accesses = classes.get(Caller.class).getAccessesFromSelf();
        assertThatAccesses(accesses)
                .contain(access()
                        .fromOrigin(Caller.class, "call")
                        .toTarget(Caller.Target.class, CONSTRUCTOR_NAME)
                )
                .contain(access()
                        .fromOrigin(Caller.class, "call")
                        .toTarget(Caller.Target.Inner.class, "method")
                )
                .contain(access()
                        .fromOrigin(Caller.class, "call")
                        .toTarget(Caller.Target.Inner.class, "field")
                );
    }

    @Test
    public void imports_try_blocks_within_lambda() {
        class Target {
            public void target() {
            }
        }
        @SuppressWarnings({"unused"})
        class Origin {
            void call(Target target) {
                execute(() -> {
                    try {
                        target.target();
                    } finally {
                        System.out.println("Irrelevant statement");
                    }
                });
            }

            private void execute(Runnable runnable) {
            }
        }

        JavaClass origin = new ClassFileImporter().importClasses(Origin.class, Target.class).get(Origin.class);

        JavaMethod originMethod = origin.getMethod("call", Target.class);
        Set<TryCatchBlock> tryCatchBlocks = originMethod.getTryCatchBlocks();
        assertThat(tryCatchBlocks).areExactly(1, tryCatchBlock().declaredIn(originMethod).catchingNoThrowables().declaredInLambda());

        assertThatAccesses(getOnlyElement(tryCatchBlocks).getAccessesContainedInTryBlock())
                .contain(access().fromOrigin(Origin.class, "call").toTarget(Target.class, "target").declaredInLambda());
    }

    @Test
    public void does_not_add_synthetic_lambda_methods_to_classes() {
        class Target {
            void target() {
            }
        }

        @SuppressWarnings("unused")
        class Caller {
            @SuppressWarnings("Convert2MethodRef")
            Runnable call(Target target) {
                return () -> target.target();
            }
        }

        JavaClass caller = new ClassFileImporter().importClasses(Target.class, Caller.class).get(Caller.class);

        assertThat(caller.getMethods())
                .doNotHave(syntheticLambdaMethods())
                .hasSize(1);
    }

    private static class Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses {
        static class Target {
            Object field;

            void method() {
            }
        }
    }

    @DataProvider
    public static Object[][] data_adds_information_about_being_declared_inside_a_lambda_to_accesses() {
        @SuppressWarnings("unused")
        class FieldAccessCase {
            Object fieldAccessDirect(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                return target.field;
            }

            Supplier<Object> fieldAccessFromLambda(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                return () -> target.field;
            }
        }
        @SuppressWarnings("unused")
        class MethodCallCase {
            void methodCallDirect(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                target.method();
            }

            @SuppressWarnings("Convert2MethodRef")
            Runnable methodCallFromLambda(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                return () -> target.method();
            }
        }
        @SuppressWarnings("unused")
        class ConstructorCallCase {
            Object constructorCallDirect() {
                return new Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target();
            }

            @SuppressWarnings("Convert2MethodRef")
            Supplier<Object> constructorCallFromLambda() {
                return () -> new Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target();
            }
        }
        @SuppressWarnings("unused")
        class MethodReferenceCase {
            Runnable methodReferenceDirect(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                return target::method;
            }

            Supplier<Runnable> methodReferenceFromLambda(Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target target) {
                return () -> target::method;
            }
        }
        @SuppressWarnings("unused")
        class ConstructorReferenceCase {
            Supplier<Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target> constructorReferenceDirect() {
                return Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target::new;
            }

            Supplier<Supplier<Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target>> constructorReferenceFromLambda() {
                return () -> Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target::new;
            }
        }

        return $$(
                $(FieldAccessCase.class, Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target.class),
                $(MethodCallCase.class, Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target.class),
                $(ConstructorCallCase.class, Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target.class),
                $(MethodReferenceCase.class, Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target.class),
                $(ConstructorReferenceCase.class, Data_of_adds_information_about_being_declared_inside_a_lambda_to_accesses.Target.class)
        );
    }

    @Test
    @UseDataProvider
    public void test_adds_information_about_being_declared_inside_a_lambda_to_accesses(Class<?> caller, Class<?> target) {
        Set<JavaAccess<?>> accesses = new ClassFileImporter()
                .importClasses(caller, target)
                .get(caller).getAccessesFromSelf().stream()
                .filter(a -> a.getTargetOwner().isEquivalentTo(target))
                .collect(toSet());

        assertThat(accesses.stream().filter(a -> a.getOrigin().getName().contains("FromLambda")))
                .isNotEmpty()
                .allMatch(JavaAccess::isDeclaredInLambda);
        assertThat(accesses.stream().filter(a -> a.getOrigin().getName().contains("Direct")))
                .isNotEmpty()
                .allMatch(javaAccess -> !javaAccess.isDeclaredInLambda());
    }

    @Test
    public void imports_referenced_class_object_in_lambda() {
        JavaClasses classes = new ClassFileImporter().importClasses(ReferencingClassObjectsFromLambda.class);
        Set<ReferencedClassObject> referencedClassObjects = classes.get(ReferencingClassObjectsFromLambda.class).getReferencedClassObjects();

        assertThatReferencedClassObjects(referencedClassObjects).containReferencedClassObjects(
                referencedClassObject(FilterInputStream.class, 10).declaredInLambda(),
                referencedClassObject(File.class, 14).declaredInLambda()
        );
    }

    @Test
    public void imports_instanceof_checks_in_lambda() {
        JavaClasses classes = new ClassFileImporter().importClasses(CheckingInstanceofFromLambda.class);
        Set<InstanceofCheck> instanceofChecks = classes.get(CheckingInstanceofFromLambda.class).getInstanceofChecks();

        assertThatInstanceofChecks(instanceofChecks).containInstanceofChecks(
                instanceofCheck(FilterInputStream.class, 11).declaredInLambda(),
                instanceofCheck(File.class, 15).declaredInLambda()
        );
    }

    private Condition<JavaMethod> syntheticLambdaMethods() {
        return new Condition<>(method -> isLambdaMethodName(method.getName()), "synthetic lambda methods");
    }

    @SuppressWarnings("SameParameterValue")
    private Condition<MethodCallTarget> targetCodeUnit(String codeUnitName, Class<?>... parameterTypes) {
        Predicate<MethodCallTarget> targetMatches = target ->
                target.getName().equals(codeUnitName)
                        && namesOf(target.getRawParameterTypes()).equals(formatNamesOf(parameterTypes));

        return new Condition<>(targetMatches, String.format("%s(%s)", codeUnitName, String.join(", ", formatNamesOf(parameterTypes))));
    }

    private JavaMethodCall singleByName(Set<JavaMethodCall> calls, String methodName) {
        return getOnlyElement(filterOriginByName(calls, methodName));
    }

    private <ACCESS extends JavaAccess<?>> Set<ACCESS> filterOriginByName(Set<ACCESS> calls, String methodName) {
        return calls.stream()
                .filter(call -> call.getOrigin().getName().equals(methodName))
                .collect(toSet());
    }
}
