package com.tngtech.archunit.core.domain;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaCodeUnit.Parameter.startWithLowercase;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class JavaCodeUnitTest {

    @Test
    public void offers_all_calls_from_Self() {
        JavaMethod method = importClassWithContext(ClassAccessingOtherClass.class).getMethod("access", ClassBeingAccessed.class);

        assertThat(method.getCallsFromSelf())
                .hasSize(4)
                .containsOnlyElementsOf(union(method.getConstructorCallsFromSelf(), method.getMethodCallsFromSelf()));
    }

    @Test
    public void offers_all_accesses_from_Self() {
        JavaMethod method = importClassWithContext(ClassAccessingOtherClass.class).getMethod("access", ClassBeingAccessed.class);

        assertThat(method.getAccessesFromSelf())
                .hasSize(6)
                .containsOnlyElementsOf(ImmutableList.<JavaAccess<?>>builder()
                        .addAll(method.getConstructorCallsFromSelf())
                        .addAll(method.getMethodCallsFromSelf())
                        .addAll(method.getFieldAccesses())
                        .build());
    }

    @DataProvider
    public static Object[][] code_units_with_parameters() {
        JavaClass javaClass = new ClassFileImporter().importClass(ClassWithVariousCodeUnitParameters.class);
        return testForEach(
                javaClass.getConstructor(Object.class, String.class),
                javaClass.getConstructor(List.class, Map.class),
                javaClass.getMethod("method", Object.class, String.class),
                javaClass.getMethod("method", List.class, Map.class)
        );
    }

    @Test
    @UseDataProvider("code_units_with_parameters")
    public void creates_parameters(JavaCodeUnit codeUnit) {
        for (int i = 0; i < codeUnit.getParameters().size(); i++) {
            assertThat(codeUnit.getParameters().get(i).getRawType()).isEqualTo(codeUnit.getRawParameterTypes().get(i));
            assertThat(codeUnit.getParameters().get(i).getType()).isEqualTo(codeUnit.getParameterTypes().get(i));
        }
    }

    @Test
    public void creates_parameters_when_raw_parameter_types_do_not_match_generic_parameter_types_for_inner_class() {
        // For inner classes the raw parameter types from the bytecode do not match the generic signature from the bytecode. The Reflection API mirrors this,
        // so the generic parameter types do not contain the synthetic owner parameter for inner classes, while the raw parameter types do.
        // This complicates things, since the indexes of raw and generic parameter types don't need to match by that.
        // Note that we deviate from the Reflection API and do not mirror the raw parameter types as parameters, but the generic ones

        @SuppressWarnings({"InnerClassMayBeStatic", "unused"})
        class InnerClassCausingConstructorDeviationBetweenRawParameterTypesAndGenericParameterTypes {
            private InnerClassCausingConstructorDeviationBetweenRawParameterTypesAndGenericParameterTypes(List<String> first, List<Integer> second) {
            }
        }

        JavaConstructor constructor = new ClassFileImporter()
                .importClass(InnerClassCausingConstructorDeviationBetweenRawParameterTypesAndGenericParameterTypes.class)
                .getConstructor(getClass(), List.class, List.class);

        assertThat(constructor.getParameters()).hasSize(2);

        for (int i = 0; i < constructor.getParameters().size(); i++) {
            assertThat(constructor.getParameters().get(i).getIndex()).as("parameter index").isEqualTo(i);
            assertThat(constructor.getParameters().get(i).getType()).isEqualTo(constructor.getParameterTypes().get(i));
            assertThat(constructor.getParameters().get(i).getRawType()).isEqualTo(constructor.getRawParameterTypes().get(i + 1));
        }
    }

    @Test
    public void creates_parameters_when_raw_parameter_types_do_not_match_generic_parameter_types_for_enum() {
        // For enums the raw parameter types from the bytecode do not match the generic signature from the bytecode. The Reflection API mirrors this,
        // so the generic parameter types do not contain the synthetic parameters for name and ordinal, while the raw parameter types do.
        // This complicates things, since the indexes of raw and generic parameter types don't need to match by that.
        // Note that we deviate from the Reflection API and do not mirror the raw parameter types as parameters, but the generic ones

        JavaConstructor constructor = new ClassFileImporter()
                .importClass(NonTrivialEnum.class)
                .getConstructor(String.class, int.class, File.class, double.class);

        assertThat(constructor.getParameters()).hasSize(2);

        for (int i = 0; i < constructor.getParameters().size(); i++) {
            assertThat(constructor.getParameters().get(i).getIndex()).as("parameter index").isEqualTo(i);
            assertThat(constructor.getParameters().get(i).getType()).isEqualTo(constructor.getParameterTypes().get(i));
            assertThat(constructor.getParameters().get(i).getRawType()).isEqualTo(constructor.getRawParameterTypes().get(i + 2));
        }
    }

    @Test
    public void falls_back_to_creating_parameters_with_only_generic_types_if_match_between_raw_types_and_generic_types_cannot_be_made() {
        final List<String> nonConstant = newArrayList(getClass().getName());
        class LocalClassReferencingNonConstantFromOuterScope {
            @SuppressWarnings("unused")
            LocalClassReferencingNonConstantFromOuterScope(List<String> someParameterizedType) {
                System.out.println("Using " + nonConstant + " which causes another synthetic List<String> parameter to be appended to the constructor");
            }
        }

        JavaConstructor constructor = new ClassFileImporter()
                .importClass(LocalClassReferencingNonConstantFromOuterScope.class)
                .getConstructor(getClass(), List.class, List.class);

        assertThat(constructor.getParameters()).hasSameSizeAs(constructor.getParameterTypes());

        for (int i = 0; i < constructor.getParameters().size(); i++) {
            assertThat(constructor.getParameters().get(i).getIndex()).as("parameter index").isEqualTo(i);
            assertThat(constructor.getParameters().get(i).getType()).isEqualTo(constructor.getParameterTypes().get(i));
            assertThat(constructor.getParameters().get(i).getRawType()).isEqualTo(constructor.getParameters().get(i).getType().toErasure());
        }
    }

    @DataProvider
    public static Object[][] code_units_with_four_different_parameters() {
        @SuppressWarnings("unused")
        class SomeClass<T> {
            SomeClass(List<String> first, T second, String third, int fourth) {
            }

            void method(List<String> first, T second, String third, int fourth) {
            }
        }
        JavaClass javaClass = new ClassFileImporter().importClass(SomeClass.class);
        return testForEach(
                javaClass.getConstructor(List.class, Object.class, String.class, int.class),
                javaClass.getMethod("method", List.class, Object.class, String.class, int.class)
        );
    }

    @Test
    @UseDataProvider("code_units_with_four_different_parameters")
    public void adds_description_to_parameters_of_code_unit(JavaCodeUnit codeUnit) {
        List<JavaCodeUnit.Parameter> parameters = codeUnit.getParameters();

        assertThat(parameters.get(0).getDescription()).isEqualTo("Parameter <" + List.class.getName() + "<" + String.class.getName() + ">> of " + startWithLowercase(codeUnit.getDescription()));
        assertThat(parameters.get(1).getDescription()).isEqualTo("Parameter <T> of " + startWithLowercase(codeUnit.getDescription()));
        assertThat(parameters.get(2).getDescription()).isEqualTo("Parameter <" + String.class.getName() + "> of " + startWithLowercase(codeUnit.getDescription()));
        assertThat(parameters.get(3).getDescription()).isEqualTo("Parameter <" + int.class.getName() + "> of " + startWithLowercase(codeUnit.getDescription()));
    }

    @Test
    @UseDataProvider("code_units_with_four_different_parameters")
    public void adds_index_to_parameters_of_code_unit(JavaCodeUnit codeUnit) {
        List<JavaCodeUnit.Parameter> parameters = codeUnit.getParameters();

        assertThat(parameters.get(0).getIndex()).isEqualTo(0);
        assertThat(parameters.get(1).getIndex()).isEqualTo(1);
        assertThat(parameters.get(2).getIndex()).isEqualTo(2);
        assertThat(parameters.get(3).getIndex()).isEqualTo(3);
    }

    @DataProvider
    public static Object[][] data_adds_owner_to_parameters_of_code_unit() {
        @SuppressWarnings("unused")
        class SomeClass {
            SomeClass(String param) {
            }

            void method(String param) {
            }
        }
        JavaClass javaClass = new ClassFileImporter().importClass(SomeClass.class);
        return testForEach(
                javaClass.getConstructor(String.class),
                javaClass.getMethod("method", String.class));
    }

    @Test
    @UseDataProvider
    public void test_adds_owner_to_parameters_of_code_unit(JavaCodeUnit codeUnit) {
        for (JavaCodeUnit.Parameter parameter : codeUnit.getParameters()) {
            assertThat(parameter.getOwner()).isEqualTo(codeUnit);
        }
    }

    @Test
    public void parameter_isAnnotatedWith() {
        @SuppressWarnings("unused")
        class SomeClass {
            void method(@SomeParameterAnnotation("test") String param) {
            }
        }

        JavaCodeUnit.Parameter parameter = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", String.class).getParameters().get(0);

        assertThat(parameter.isAnnotatedWith(SomeParameterAnnotation.class))
                .as("parameter is annotated with @" + SomeParameterAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isAnnotatedWith(Deprecated.class))
                .as("parameter is annotated with @" + Deprecated.class.getSimpleName()).isFalse();

        assertThat(parameter.isAnnotatedWith(SomeParameterAnnotation.class.getName()))
                .as("parameter is annotated with @" + SomeParameterAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isAnnotatedWith(Deprecated.class.getName()))
                .as("parameter is annotated with @" + Deprecated.class.getSimpleName()).isFalse();

        assertThat(parameter.isAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(SomeParameterAnnotation.class))))
                .as("parameter is annotated with @" + SomeParameterAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(Deprecated.class))))
                .as("parameter is annotated with @" + Deprecated.class.getSimpleName()).isFalse();
    }

    @Test
    public void parameter_isMetaAnnotatedWith() {
        @SuppressWarnings("unused")
        class SomeClass {
            void method(@SomeParameterAnnotation("test") String param) {
            }
        }

        JavaCodeUnit.Parameter parameter = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", String.class).getParameters().get(0);

        assertThat(parameter.isMetaAnnotatedWith(SomeMetaAnnotation.class))
                .as("parameter is meta-annotated with @" + SomeMetaAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isMetaAnnotatedWith(Deprecated.class))
                .as("parameter is meta-annotated with @" + Deprecated.class.getSimpleName()).isFalse();

        assertThat(parameter.isMetaAnnotatedWith(SomeMetaAnnotation.class.getName()))
                .as("parameter is meta-annotated with @" + SomeMetaAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isMetaAnnotatedWith(Deprecated.class.getName()))
                .as("parameter is meta-annotated with @" + Deprecated.class.getSimpleName()).isFalse();

        assertThat(parameter.isMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(SomeMetaAnnotation.class))))
                .as("parameter is meta-annotated with @" + SomeMetaAnnotation.class.getSimpleName()).isTrue();
        assertThat(parameter.isMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(Deprecated.class))))
                .as("parameter is meta-annotated with @" + Deprecated.class.getSimpleName()).isFalse();
    }

    @Test
    public void parameter_getAnnotationOfType() {
        @SuppressWarnings("unused")
        class SomeClass {
            void method(@SomeParameterAnnotation("test") String param) {
            }
        }

        final JavaCodeUnit.Parameter parameter = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", String.class).getParameters().get(0);

        SomeParameterAnnotation annotation = parameter.getAnnotationOfType(SomeParameterAnnotation.class);
        assertThat(annotation).isInstanceOf(SomeParameterAnnotation.class);
        assertThat(annotation.value()).isEqualTo("test");
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                parameter.getAnnotationOfType(Deprecated.class);
            }
        }).isInstanceOf(IllegalArgumentException.class);

        JavaAnnotation<JavaCodeUnit.Parameter> javaAnnotation = parameter.getAnnotationOfType(SomeParameterAnnotation.class.getName());
        assertThatAnnotation(javaAnnotation).hasType(SomeParameterAnnotation.class);
        assertThat(javaAnnotation.get("value")).contains("test");
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                parameter.getAnnotationOfType(Deprecated.class.getName());
            }
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parameter_tryGetAnnotationOfType() {
        @SuppressWarnings("unused")
        class SomeClass {
            void method(@SomeParameterAnnotation("test") String param) {
            }
        }

        final JavaCodeUnit.Parameter parameter = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", String.class).getParameters().get(0);

        assertThat(parameter.tryGetAnnotationOfType(SomeParameterAnnotation.class).get()).isInstanceOf(SomeParameterAnnotation.class);
        assertThat(parameter.tryGetAnnotationOfType(Deprecated.class)).isAbsent();

        assertThatAnnotation(parameter.tryGetAnnotationOfType(SomeParameterAnnotation.class.getName()).get()).hasType(SomeParameterAnnotation.class);
        assertThat(parameter.tryGetAnnotationOfType(Deprecated.class.getName())).isAbsent();
    }

    @SuppressWarnings("unused")
    private static class ClassWithVariousCodeUnitParameters {
        ClassWithVariousCodeUnitParameters(Object simple, String noParameterizedTypes) {
        }

        ClassWithVariousCodeUnitParameters(List<String> complex, Map<?, ?> withParameterizedTypes) {
        }

        void method(Object simple, String noParameterizedTypes) {
        }

        void method(List<String> complex, Map<?, ?> withParameterizedTypes) {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingOtherClass {
        void access(ClassBeingAccessed classBeingAccessed) {
            new ClassBeingAccessed();
            new ClassBeingAccessed("");
            classBeingAccessed.field1 = "";
            classBeingAccessed.field2 = null;
            classBeingAccessed.method1();
            classBeingAccessed.method2();
        }
    }

    private static class ClassBeingAccessed {
        String field1;
        Object field2;

        ClassBeingAccessed() {
        }

        ClassBeingAccessed(String field1) {
            this.field1 = field1;
        }

        void method1() {
        }

        void method2() {
        }
    }

    @SuppressWarnings("unused")
    enum NonTrivialEnum {
        VALUE(new File("irrelevant"), 0);

        NonTrivialEnum(File file, double primitive) {
        }
    }

    @interface SomeMetaAnnotation {
    }

    @SomeMetaAnnotation
    @interface SomeParameterAnnotation {
        String value();
    }
}