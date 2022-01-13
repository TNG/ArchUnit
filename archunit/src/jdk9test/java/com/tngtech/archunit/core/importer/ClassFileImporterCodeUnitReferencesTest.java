package com.tngtech.archunit.core.importer;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorReference;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.importer.testexamples.codeunitreferences.Origin;
import com.tngtech.archunit.core.importer.testexamples.codeunitreferences.Target;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccess;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterCodeUnitReferencesTest {

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    private static class Data_imports_static_method_references {
        interface ReferencedInterfaceTarget {
            static void staticMethodToReference() {
            }
        }

        static class ReferencedClassTarget {
            static void staticMethodToReference() {
            }
        }

        class OriginReferencingInterface {
            void accessesStaticMethodReference() {
                Runnable b = ReferencedInterfaceTarget::staticMethodToReference;
            }
        }

        class OriginReferencingClass {
            void accessesStaticMethodReference() {
                Runnable b = ReferencedClassTarget::staticMethodToReference;
            }
        }
    }

    @DataProvider
    public static Object[][] data_imports_static_method_references() {
        return $$(
                $(
                        Data_imports_static_method_references.OriginReferencingInterface.class,
                        Data_imports_static_method_references.ReferencedInterfaceTarget.class
                ),
                $(
                        Data_imports_static_method_references.OriginReferencingClass.class,
                        Data_imports_static_method_references.ReferencedClassTarget.class
                ));
    }

    @Test
    @UseDataProvider
    public void test_imports_static_method_references(Class<?> originClassInput, Class<?> targetClassInput) {
        JavaClasses javaClasses = new ClassFileImporter().importClasses(originClassInput, targetClassInput);
        JavaClass originClass = javaClasses.get(originClassInput);
        JavaClass targetClass = javaClasses.get(targetClassInput);

        assertThatAccess(getOnlyElement(originClass.getMethod("accessesStaticMethodReference").getMethodReferencesFromSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("accessesStaticMethodReference"))
                .isTo(targetClass.getMethod("staticMethodToReference"));

        assertThatAccess(getOnlyElement(targetClass.getMethod("staticMethodToReference").getReferencesToSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("accessesStaticMethodReference"))
                .isTo(targetClass.getMethod("staticMethodToReference"));
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    private static class Data_imports_instance_method_references_bound_to_instance {
        interface ReferencedInterfaceTarget {
            void instanceMethodToReference();
        }

        class ReferencedClassTarget {
            void instanceMethodToReference() {
            }
        }

        class OriginReferencingInterface {
            private ReferencedInterfaceTarget target;

            void referencesInstanceMethodBoundToInstance() {
                Runnable c = target::instanceMethodToReference;
            }
        }

        class OriginReferencingClass {
            private ReferencedClassTarget target;

            void referencesInstanceMethodBoundToInstance() {
                Runnable c = target::instanceMethodToReference;
            }
        }
    }

    @DataProvider
    public static Object[][] data_imports_instance_method_references_bound_to_instance() {
        return $$(
                $(
                        Data_imports_instance_method_references_bound_to_instance.OriginReferencingInterface.class,
                        Data_imports_instance_method_references_bound_to_instance.ReferencedInterfaceTarget.class
                ),
                $(
                        Data_imports_instance_method_references_bound_to_instance.OriginReferencingClass.class,
                        Data_imports_instance_method_references_bound_to_instance.ReferencedClassTarget.class
                ));
    }

    @Test
    @UseDataProvider
    public void test_imports_instance_method_references_bound_to_instance(Class<?> originClassInput, Class<?> targetClassInput) {
        JavaClasses javaClasses = new ClassFileImporter().importClasses(originClassInput, targetClassInput);
        JavaClass originClass = javaClasses.get(originClassInput);
        JavaClass targetClass = javaClasses.get(targetClassInput);

        assertThatAccess(getOnlyElement(originClass.getMethod("referencesInstanceMethodBoundToInstance").getMethodReferencesFromSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesInstanceMethodBoundToInstance"))
                .isTo(targetClass.getMethod("instanceMethodToReference"));

        assertThatAccess(getOnlyElement(targetClass.getMethod("instanceMethodToReference").getReferencesToSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesInstanceMethodBoundToInstance"))
                .isTo(targetClass.getMethod("instanceMethodToReference"));
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    private static class Data_imports_instance_method_references_not_bound_to_instance {
        interface ReferencedInterfaceTarget {
            void instanceMethodToReference();
        }

        class ReferencedClassTarget {
            void instanceMethodToReference() {
            }
        }

        class OriginReferencingInterface {
            void referencesInstanceMethodNotBoundToInstance() {
                Consumer<ReferencedInterfaceTarget> d = ReferencedInterfaceTarget::instanceMethodToReference;
            }
        }

        class OriginReferencingClass {
            void referencesInstanceMethodNotBoundToInstance() {
                Consumer<ReferencedClassTarget> d = ReferencedClassTarget::instanceMethodToReference;
            }
        }
    }

    @DataProvider
    public static Object[][] data_imports_instance_method_references_not_bound_to_instance() {
        return $$(
                $(
                        Data_imports_instance_method_references_not_bound_to_instance.OriginReferencingInterface.class,
                        Data_imports_instance_method_references_not_bound_to_instance.ReferencedInterfaceTarget.class
                ),
                $(
                        Data_imports_instance_method_references_not_bound_to_instance.OriginReferencingClass.class,
                        Data_imports_instance_method_references_not_bound_to_instance.ReferencedClassTarget.class
                ));
    }

    @Test
    @UseDataProvider
    public void test_imports_instance_method_references_not_bound_to_instance(Class<?> originClassInput, Class<?> targetClassInput) {
        JavaClasses javaClasses = new ClassFileImporter().importClasses(originClassInput, targetClassInput);
        JavaClass originClass = javaClasses.get(originClassInput);
        JavaClass targetClass = javaClasses.get(targetClassInput);

        assertThatAccess(getOnlyElement(originClass.getMethod("referencesInstanceMethodNotBoundToInstance").getMethodReferencesFromSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesInstanceMethodNotBoundToInstance"))
                .isTo(targetClass.getMethod("instanceMethodToReference"));

        assertThatAccess(getOnlyElement(targetClass.getMethod("instanceMethodToReference").getReferencesToSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesInstanceMethodNotBoundToInstance"))
                .isTo(targetClass.getMethod("instanceMethodToReference"));
    }

    private static class Data_imports_constructor_references {
        @SuppressWarnings("unused")
        static class ReferencedTarget {
            ReferencedTarget() {
            }
        }

        @SuppressWarnings("unused")
        static class Origin {
            void referencesConstructor() {
                Supplier<ReferencedTarget> a = ReferencedTarget::new;
            }
        }
    }

    @Test
    public void imports_constructor_references() {
        JavaClasses javaClasses = new ClassFileImporter()
                .importClasses(Data_imports_constructor_references.Origin.class, Data_imports_constructor_references.ReferencedTarget.class);
        JavaClass originClass = javaClasses.get(Data_imports_constructor_references.Origin.class);
        JavaClass targetClass = javaClasses.get(Data_imports_constructor_references.ReferencedTarget.class);

        assertThatAccess(getOnlyElement(originClass.getMethod("referencesConstructor").getConstructorReferencesFromSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesConstructor"))
                .isTo(targetClass.getConstructor());

        assertThatAccess(getOnlyElement(targetClass.getConstructor().getReferencesToSelf()))
                .isFrom(originClass.getCodeUnitWithParameterTypeNames("referencesConstructor"))
                .isTo(targetClass.getConstructor());
    }

    /**
     * A local class constructor obtains extra parameters from the outer scope that the compiler transparently adds
     * to the byte code. A reference to this local constructor will then always be translated to a lambda call.
     * Thus, in this case we do not expect a constructor reference.
     */
    @Test
    public void does_not_import_local_constructor_references() {
        @SuppressWarnings("unused")
        class ReferencedTarget {
            ReferencedTarget() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            void referencesConstructor() {
                Supplier<ReferencedTarget> a = ReferencedTarget::new;
            }
        }

        JavaClasses javaClasses = new ClassFileImporter().importClasses(Origin.class, ReferencedTarget.class);

        assertThat(javaClasses.get(Origin.class).getMethod("referencesConstructor").getConstructorReferencesFromSelf()).isEmpty();
        assertThat(javaClasses.get(ReferencedTarget.class).getConstructor(ClassFileImporterCodeUnitReferencesTest.class).getReferencesToSelf()).isEmpty();
    }

    @Test
    public void does_not_import_lambdas_as_method_or_constructor_references() {
        @SuppressWarnings("unused")
        class ReferencedTarget {
            void call() {
            }
        }
        @SuppressWarnings({"unused", "Convert2MethodRef"})
        class Origin {
            void referencesConstructor(ReferencedTarget target) {
                Runnable r = () -> target.call();
            }
        }

        JavaClasses javaClasses = new ClassFileImporter().importClasses(Origin.class, ReferencedTarget.class);

        assertThat(javaClasses.get(Origin.class).getCodeUnitReferencesFromSelf()).isEmpty();
        assertThat(javaClasses.get(ReferencedTarget.class).getCodeUnitReferencesToSelf()).isEmpty();
    }

    private static class Data_imports_method_and_constructor_references_as_accesses {
        @SuppressWarnings("unused")
        static class ReferencedTarget {
            ReferencedTarget() {
            }

            void call() {
            }
        }

        @SuppressWarnings("unused")
        static class Origin {
            void referencesConstructorsAndMethods1() {
                Supplier<ReferencedTarget> a = ReferencedTarget::new;
                Consumer<ReferencedTarget> b = ReferencedTarget::call;
                Supplier<ReferencedTarget> c = ReferencedTarget::new;
                Consumer<ReferencedTarget> d = ReferencedTarget::call;
            }

            void referencesConstructorsAndMethods2() {
                Supplier<ReferencedTarget> a = ReferencedTarget::new;
                Consumer<ReferencedTarget> b = ReferencedTarget::call;
                Supplier<ReferencedTarget> c = ReferencedTarget::new;
                Consumer<ReferencedTarget> d = ReferencedTarget::call;
            }
        }
    }

    @Test
    public void imports_method_and_constructor_references_from_self_as_accesses() {
        JavaClasses javaClasses = new ClassFileImporter()
                .importClasses(Data_imports_method_and_constructor_references_as_accesses.Origin.class, Data_imports_method_and_constructor_references_as_accesses.ReferencedTarget.class);
        JavaClass originClass = javaClasses.get(Data_imports_method_and_constructor_references_as_accesses.Origin.class);

        assertThat(originClass.getAccessesFromSelf()).containsAll(originClass.getCodeUnitAccessesFromSelf());

        assertThat(originClass.getCodeUnitAccessesFromSelf()).containsAll(originClass.getCodeUnitReferencesFromSelf());

        assertThat(originClass.getCodeUnitReferencesFromSelf())
                .hasSize(8)
                .containsAll(originClass.getMethodReferencesFromSelf())
                .containsAll(originClass.getConstructorReferencesFromSelf())
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods1").getCodeUnitReferencesFromSelf())
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods2").getCodeUnitReferencesFromSelf());

        assertThat(originClass.getConstructorReferencesFromSelf())
                .hasSize(4)
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods1").getConstructorReferencesFromSelf())
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods2").getConstructorReferencesFromSelf());

        assertThat(originClass.getMethodReferencesFromSelf())
                .hasSize(4)
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods1").getMethodReferencesFromSelf())
                .containsAll(originClass.getMethod("referencesConstructorsAndMethods2").getMethodReferencesFromSelf());
    }

    @Test
    public void imports_method_and_constructor_references_to_self_as_accesses() {
        JavaClasses javaClasses = new ClassFileImporter()
                .importClasses(Data_imports_method_and_constructor_references_as_accesses.Origin.class, Data_imports_method_and_constructor_references_as_accesses.ReferencedTarget.class);
        JavaClass targetClass = javaClasses.get(Data_imports_method_and_constructor_references_as_accesses.ReferencedTarget.class);

        assertThat(targetClass.getAccessesToSelf()).containsAll(targetClass.getCodeUnitAccessesToSelf());

        assertThat(targetClass.getCodeUnitAccessesToSelf()).containsAll(targetClass.getCodeUnitReferencesToSelf());

        assertThat(targetClass.getCodeUnitReferencesToSelf())
                .hasSize(8)
                .containsAll(targetClass.getMethodReferencesToSelf())
                .containsAll(targetClass.getConstructorReferencesToSelf())
                .containsAll(targetClass.getMethod("call").getReferencesToSelf())
                .containsAll(targetClass.getConstructor().getReferencesToSelf());

        assertThat(targetClass.getConstructorReferencesToSelf())
                .hasSize(4)
                .containsAll(targetClass.getConstructor().getReferencesToSelf());

        assertThat(targetClass.getMethodReferencesToSelf())
                .hasSize(4)
                .containsAll(targetClass.getMethod("call").getReferencesToSelf());
    }

    @Test
    public void creates_correct_description_for_references() {
        JavaClasses javaClasses = new ClassFileImporter().importClasses(Origin.class, Target.class);
        JavaClass originClass = javaClasses.get(Origin.class);
        JavaClass targetClass = javaClasses.get(Target.class);

        JavaMethod constructorReferenceOrigin = originClass.getMethod("referencesConstructor");
        JavaConstructor targetConstructor = targetClass.getConstructor();
        JavaMethod methodReferenceOrigin = originClass.getMethod("referencesMethod");
        JavaMethod targetMethod = targetClass.getMethod("call");

        JavaConstructorReference constructorReference = getOnlyElement(constructorReferenceOrigin.getConstructorReferencesFromSelf());
        assertThat(constructorReference.getDescription()).isEqualTo(String.format(
                "Method <%s> references constructor <%s> in (%s.java:%d)",
                constructorReferenceOrigin.getFullName(), targetConstructor.getFullName(), Origin.class.getSimpleName(), 9
        ));

        JavaMethodReference methodReference = getOnlyElement(originClass.getMethod("referencesMethod").getMethodReferencesFromSelf());
        assertThat(methodReference.getDescription()).isEqualTo(String.format(
                "Method <%s> references method <%s> in (%s.java:%d)",
                methodReferenceOrigin.getFullName(), targetMethod.getFullName(), Origin.class.getSimpleName(), 13
        ));
    }
}
