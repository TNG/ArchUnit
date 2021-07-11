package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.genericArray;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.parameterizedTypeArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.typeVariableArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteWildcardType.wildcardType;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterGenericCodeUnitParameterTypesTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    @DataProvider
    public static Object[][] data_imports_non_generic_code_unit_parameter_type() {
        class NonGenericParameterType {
        }
        @SuppressWarnings("unused")
        class NoGenericSignatureOnConstructor {
            NoGenericSignatureOnConstructor(NonGenericParameterType param) {
            }
        }
        @SuppressWarnings("unused")
        class NoGenericSignatureOnMethod {
            void method(NonGenericParameterType param) {
            }
        }
        Object[][] testCases = testCasesFromSameGenericSignatureOnConstructorAndMethod(
                NoGenericSignatureOnConstructor.class,
                NoGenericSignatureOnMethod.class,
                NonGenericParameterType.class);
        return $$(
                $(testCases[0][0], NonGenericParameterType.class),
                $(testCases[1][0], NonGenericParameterType.class)
        );
    }

    @Test
    @UseDataProvider
    public void test_imports_non_generic_code_unit_parameter_type(JavaCodeUnit codeUnit, Class<?> expectedParameterType) {
        JavaType parameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(parameterType).as("parameter type").matches(expectedParameterType);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_one_type_argument() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<String> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<String> param) {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_one_type_argument(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type")
                .hasErasure(ClassParameterWithSingleTypeParameter.class)
                .hasActualTypeArguments(String.class);
    }

    @Test
    public void imports_raw_parameter_types_and_generic_parameter_types_of_inner_class_constructor_like_Reflection_API() {
        @SuppressWarnings("unused")
        class LocalClassThatWillHaveEnclosingClassAsFirstRawConstructorParameter {
            public LocalClassThatWillHaveEnclosingClassAsFirstRawConstructorParameter(ClassParameterWithSingleTypeParameter<String> param) {
            }
        }

        JavaConstructor constructor = new ClassFileImporter()
                .importClasses(
                        LocalClassThatWillHaveEnclosingClassAsFirstRawConstructorParameter.class,
                        getClass(), ClassParameterWithSingleTypeParameter.class, String.class)
                .get(LocalClassThatWillHaveEnclosingClassAsFirstRawConstructorParameter.class)
                .getConstructor(getClass(), ClassParameterWithSingleTypeParameter.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(getClass(), ClassParameterWithSingleTypeParameter.class);

        assertThat(constructor.getParameterTypes()).hasSize(1);
        assertThatType(constructor.getParameterTypes().get(0)).matches(
                parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_raw_generic_code_unit_parameter_type_as_JavaClass_instead_of_JavaParameterizedType() {
        @SuppressWarnings({"unused", "rawtypes"})
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter param) {
            }
        }
        @SuppressWarnings({"unused", "rawtypes"})
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_raw_generic_code_unit_parameter_type_as_JavaClass_instead_of_JavaParameterizedType(JavaCodeUnit codeUnit) {
        JavaType rawGenericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(rawGenericParameterType).as("raw generic parameter type").matches(ClassParameterWithSingleTypeParameter.class);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<String[]> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<String[]> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_array_type_argument(JavaCodeUnit codeUnit) {
        JavaType genericMethodParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericMethodParameterType).as("generic parameter type")
                .hasErasure(ClassParameterWithSingleTypeParameter.class)
                .hasActualTypeArguments(String[].class);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_primitive_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<int[]> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<int[]> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_primitive_array_type_argument(JavaCodeUnit codeUnit) {
        JavaType genericMethodParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericMethodParameterType).as("generic parameter type")
                .hasErasure(ClassParameterWithSingleTypeParameter.class)
                .hasActualTypeArguments(int[].class);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_multiple_type_arguments() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<String, Serializable, File> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithThreeTypeParameters<String, Serializable, File> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, Serializable.class, File.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_multiple_type_arguments(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type")
                .hasErasure(ClassParameterWithThreeTypeParameters.class)
                .hasActualTypeArguments(String.class, Serializable.class, File.class);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_single_actual_type_argument_parameterized_with_concrete_class() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<String>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<String>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_single_actual_type_argument_parameterized_with_concrete_class(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_multiple_actual_type_arguments_parameterized_with_concrete_classes() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<
                    ClassParameterWithSingleTypeParameter<File>,
                    InterfaceParameterWithSingleTypeParameter<Serializable>,
                    InterfaceParameterWithSingleTypeParameter<String>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithThreeTypeParameters<
                    ClassParameterWithSingleTypeParameter<File>,
                    InterfaceParameterWithSingleTypeParameter<Serializable>,
                    InterfaceParameterWithSingleTypeParameter<String>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, ClassParameterWithSingleTypeParameter.class, InterfaceParameterWithSingleTypeParameter.class,
                File.class, Serializable.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_multiple_actual_type_arguments_parameterized_with_concrete_classes(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(File.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(Serializable.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_single_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<?> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<?> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_single_unbound_wildcard(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(wildcardType());
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_single_actual_type_argument_parameterized_with_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<?>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<?>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_single_actual_type_argument_parameterized_with_unbound_wildcard(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameter()
        );
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_actual_type_arguments_parameterized_with_bounded_wildcards() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<? extends String>,
                    ClassParameterWithSingleTypeParameter<? super File>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<? extends String>,
                    ClassParameterWithSingleTypeParameter<? super File>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, File.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_actual_type_arguments_parameterized_with_bounded_wildcards(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(String.class),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(File.class)
        );
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_actual_type_arguments_with_multiple_wildcards_with_various_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<Map<? extends Serializable, ? super File>>,
                    ClassParameterWithSingleTypeParameter<Reference<? super String>>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<Map<? extends Serializable, ? super File>>,
                    ClassParameterWithSingleTypeParameter<Reference<? super String>>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, Map.class, Serializable.class, File.class, Reference.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_actual_type_arguments_with_multiple_wildcards_with_various_bounds(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(parameterizedType(Map.class)
                                .withWildcardTypeParameters(
                                        wildcardType().withUpperBound(Serializable.class),
                                        wildcardType().withLowerBound(File.class))),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(parameterizedType(Reference.class)
                                .withWildcardTypeParameterWithLowerBound(String.class))
        );
    }

    @DataProvider
    public static Object[][] data_imports_type_variable_as_generic_code_unit_parameter_type() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<T extends String> {
            GenericSignatureOnConstructor(T param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<T extends String> {
            void method(T param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_type_variable_as_generic_code_unit_parameter_type(JavaCodeUnit codeUnit) {
        JavaType genericMethodParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericMethodParameterType).as("generic parameter type")
                .isInstanceOf(JavaTypeVariable.class)
                .hasErasure(String.class);
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<OF_CLASS> {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<OF_CLASS> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<OF_CLASS> {
            void method(ClassParameterWithSingleTypeParameter<OF_CLASS> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_parameterized_with_type_variable(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(typeVariable("OF_CLASS"));
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_actual_type_argument_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<OF_CLASS> {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<OF_CLASS>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<OF_CLASS> {
            void method(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<OF_CLASS>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, ClassParameterWithSingleTypeParameter.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_actual_type_argument_parameterized_with_type_variable(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS"))
        );
    }

    @DataProvider
    public static Object[][] data_references_type_variable_assigned_to_actual_type_argument_of_generic_code_unit_parameter_type() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<OF_CLASS extends String> {
            GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<OF_CLASS>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<OF_CLASS extends String> {
            void method(ClassParameterWithSingleTypeParameter<ClassParameterWithSingleTypeParameter<OF_CLASS>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_references_type_variable_assigned_to_actual_type_argument_of_generic_code_unit_parameter_type(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS").withUpperBounds(String.class))
        );
    }

    @DataProvider
    public static Object[][] data_references_outer_type_variable_assigned_to_actual_type_argument_of_generic_code_unit_parameter_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class GenericSignatureOnConstructor {
                    GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<OUTER> param) {
                    }
                }

                class GenericSignatureOnMethod {
                    void method(ClassParameterWithSingleTypeParameter<OUTER> param) {
                    }
                }
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                OuterWithTypeParameter.SomeInner.GenericSignatureOnConstructor.class,
                OuterWithTypeParameter.SomeInner.GenericSignatureOnMethod.class,
                OuterWithTypeParameter.class, OuterWithTypeParameter.SomeInner.class, ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_references_outer_type_variable_assigned_to_actual_type_argument_of_generic_code_unit_parameter_type_of_inner_class(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                typeVariable("OUTER").withUpperBounds(String.class)
        );
    }

    @DataProvider
    public static Object[][] data_creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context_for_generic_code_unit_parameter_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class GenericSignatureOnConstructor {
                    GenericSignatureOnConstructor(ClassParameterWithSingleTypeParameter<OUTER> param) {
                    }
                }

                class GenericSignatureOnMethod {
                    void method(ClassParameterWithSingleTypeParameter<OUTER> param) {
                    }
                }
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                OuterWithTypeParameter.SomeInner.GenericSignatureOnConstructor.class,
                OuterWithTypeParameter.SomeInner.GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context_for_generic_code_unit_parameter_type_of_inner_class(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                typeVariable("OUTER").withoutUpperBounds()
        );
    }

    @DataProvider
    public static Object[][] data_considers_hierarchy_of_methods_and_classes_for_type_parameter_context() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class Level1<T1 extends String> {
            <T2 extends T1> void level2() {
                class GenericSignatureOnConstructor<T3 extends T2> {
                    <T4 extends T3> GenericSignatureOnConstructor(T4 param) {
                    }
                }
                class GenericSignatureOnMethod<T3 extends T2> {
                    <T4 extends T3> void method(T4 param) {
                    }
                }
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Class.forName(Level1.class.getName() + "$1GenericSignatureOnConstructor"),
                Class.forName(Level1.class.getName() + "$1GenericSignatureOnMethod"),
                Level1.class, ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_considers_hierarchy_of_methods_and_classes_for_type_parameter_context(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type")
                .matches(
                        typeVariable("T4").withUpperBounds(
                                typeVariable("T3").withUpperBounds(
                                        typeVariable("T2").withUpperBounds(
                                                typeVariable("T1").withUpperBounds(String.class)))));
    }

    @DataProvider
    public static Object[][] data_imports_wildcards_of_generic_code_unit_parameter_type_bound_by_type_variables() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<FIRST extends String, SECOND extends Serializable> {
            GenericSignatureOnConstructor(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<? extends FIRST>,
                    ClassParameterWithSingleTypeParameter<? super SECOND>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<FIRST extends String, SECOND extends Serializable> {
            void method(ClassParameterWithTwoTypeParameters<
                    ClassParameterWithSingleTypeParameter<? extends FIRST>,
                    ClassParameterWithSingleTypeParameter<? super SECOND>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_wildcards_of_generic_code_unit_parameter_type_bound_by_type_variables(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("FIRST").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("SECOND").withUpperBounds(Serializable.class))
        );
    }

    @DataProvider
    public static Object[][] data_imports_wildcards_of_generic_code_unit_parameter_type_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class GenericSignatureOnConstructor {
                    GenericSignatureOnConstructor(ClassParameterWithTwoTypeParameters<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> param) {
                    }
                }

                class GenericSignatureOnMethod {
                    void method(ClassParameterWithTwoTypeParameters<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> param) {
                    }
                }
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                OuterWithTypeParameter.SomeInner.GenericSignatureOnConstructor.class,
                OuterWithTypeParameter.SomeInner.GenericSignatureOnMethod.class,
                OuterWithTypeParameter.class, OuterWithTypeParameter.SomeInner.class, ClassParameterWithTwoTypeParameters.class,
                ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_wildcards_of_generic_code_unit_parameter_type_bound_by_type_variables_of_enclosing_classes(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withUpperBounds(Serializable.class))
        );
    }

    @DataProvider
    public static Object[][] data_creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class GenericSignatureOnConstructor {
                    GenericSignatureOnConstructor(ClassParameterWithTwoTypeParameters<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> param) {
                    }
                }

                class GenericSignatureOnMethod {
                    void method(ClassParameterWithTwoTypeParameters<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> param) {
                    }
                }
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                OuterWithTypeParameter.SomeInner.GenericSignatureOnConstructor.class,
                OuterWithTypeParameter.SomeInner.GenericSignatureOnMethod.class,
                ClassParameterWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withoutUpperBounds()),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withoutUpperBounds())
        );
    }

    @DataProvider
    public static Object[][] data_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_self_referencing_type_definitions() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<
                    // assigned to ClassParameterWithThreeTypeParameters<A,_,_>
                    List<? extends FIRST>,
                    // assigned to ClassParameterWithThreeTypeParameters<_,B,_>
                    Map<
                            Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                    // assigned to ClassParameterWithThreeTypeParameters<_,_,C>
                    Comparable<GenericSignatureOnConstructor<FIRST, SECOND>>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            void method(ClassParameterWithThreeTypeParameters<
                    // assigned to ClassParameterWithThreeTypeParameters<A,_,_>
                    List<? extends FIRST>,
                    // assigned to ClassParameterWithThreeTypeParameters<_,B,_>
                    Map<
                            Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                    // assigned to ClassParameterWithThreeTypeParameters<_,_,C>
                    Comparable<GenericSignatureOnMethod<FIRST, SECOND>>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, String.class, Serializable.class, Cloneable.class, List.class,
                Map.class, Map.Entry.class, Set.class, Iterable.class, Comparable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_self_referencing_type_definitions(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        // @formatter:off
        assertThatType(genericParameterType).as("generic parameter type").hasActualTypeArguments(
            // assigned to ClassParameterWithThreeTypeParameters<A,_,_>
            parameterizedType(List.class)
                .withWildcardTypeParameterWithUpperBound(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class)),
            // assigned to ClassParameterWithThreeTypeParameters<_,B,_>
            parameterizedType(Map.class).withTypeArguments(
                parameterizedType(Map.Entry.class).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    parameterizedType(Map.Entry.class).withTypeArguments(
                        concreteClass(String.class),
                        typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))),
                parameterizedType(Map.class).withTypeArguments(
                    wildcardType().withUpperBound(String.class),
                    parameterizedType(Map.class).withTypeArguments(
                        wildcardType().withUpperBound(Serializable.class),
                        parameterizedType(List.class).withTypeArguments(
                            parameterizedType(List.class).withTypeArguments(
                                wildcardType().withUpperBound(
                                    parameterizedType(Set.class).withTypeArguments(
                                        wildcardType().withLowerBound(
                                            parameterizedType(Iterable.class).withTypeArguments(
                                                wildcardType().withLowerBound(
                                                    parameterizedType(Map.class).withTypeArguments(
                                                        typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class),
                                                        wildcardType()))))))))))),
            // assigned to ClassParameterWithThreeTypeParameters<_,_,C>
            parameterizedType(Comparable.class).withTypeArguments(
                parameterizedType(codeUnit.getOwner().reflect()).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))));
        // @formatter:on
    }

    @DataProvider
    public static Object[][] data_imports_complex_generic_array_code_unit_parameter_type() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(List<Map<? super String, Map<Map<? super String, ?>, Serializable>>>[] param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(List<Map<? super String, Map<Map<? super String, ?>, Serializable>>>[] param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                List.class, Serializable.class, Map.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_generic_array_code_unit_parameter_type(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).matches(
                genericArray(
                        List.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", "
                                + Map.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", ?>, "
                                + Serializable.class.getName() + ">>>[]"
                ).withComponentType(
                        parameterizedType(List.class).withTypeArguments(
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withLowerBound(String.class),
                                        parameterizedType(Map.class).withTypeArguments(
                                                parameterizedType(Map.class).withTypeArguments(
                                                        wildcardType().withLowerBound(String.class),
                                                        wildcardType()),
                                                concreteClass(Serializable.class))))));
    }

    @DataProvider
    public static Object[][] data_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_concrete_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<
                    List<Serializable[]>,
                    List<? extends Serializable[][]>,
                    Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithThreeTypeParameters<
                    List<Serializable[]>,
                    List<? extends Serializable[][]>,
                    Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, List.class, Serializable.class, Map.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_concrete_array_bounds(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).hasActualTypeArguments(
                parameterizedType(List.class).withTypeArguments(Serializable[].class),
                parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(Serializable[][].class),
                parameterizedType(Map.class).withTypeArguments(
                        wildcardType().withLowerBound(String[].class),
                        parameterizedType(Map.class).withTypeArguments(
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withLowerBound(String[][][].class),
                                        wildcardType()),
                                concreteClass(Serializable[][].class))));
    }

    @DataProvider
    public static Object[][] data_imports_generic_code_unit_parameter_type_with_parameterized_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            void method(ClassParameterWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_generic_code_unit_parameter_type_with_parameterized_array_bounds(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).hasActualTypeArguments(
                genericArray(parameterizedTypeArrayName(List.class, String.class, 1)).withComponentType(
                        parameterizedType(List.class).withTypeArguments(String.class)),
                genericArray(parameterizedTypeArrayName(List.class, String[].class, 2)).withComponentType(
                        genericArray(parameterizedTypeArrayName(List.class, String[].class, 1)).withComponentType(
                                parameterizedType(List.class).withTypeArguments(String[].class))),
                genericArray(parameterizedTypeArrayName(List.class, String[][].class, 3)).withComponentType(
                        genericArray(parameterizedTypeArrayName(List.class, String[][].class, 2)).withComponentType(
                                genericArray(parameterizedTypeArrayName(List.class, String[][].class, 1)).withComponentType(
                                        parameterizedType(List.class).withTypeArguments(String[][].class)))));
    }

    @DataProvider
    public static Object[][] data_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_generic_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<X extends Serializable, Y extends String> {
            GenericSignatureOnConstructor(ClassParameterWithThreeTypeParameters<
                    List<X[]>,
                    List<? extends X[][]>,
                    Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> param) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<X extends Serializable, Y extends String> {
            void method(ClassParameterWithThreeTypeParameters<
                    List<X[]>,
                    List<? extends X[][]>,
                    Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> param) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithThreeTypeParameters.class, List.class, Serializable.class, Map.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_generic_code_unit_parameter_type_with_multiple_nested_actual_type_arguments_with_generic_array_bounds(JavaCodeUnit codeUnit) {
        JavaType genericParameterType = codeUnit.getParameterTypes().get(0);

        assertThatType(genericParameterType).hasActualTypeArguments(
                parameterizedType(List.class).withTypeArguments(
                        genericArray(typeVariableArrayName("X", 1)).withComponentType(
                                typeVariable("X").withUpperBounds(Serializable.class))),
                parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                        genericArray(typeVariableArrayName("X", 2)).withComponentType(
                                genericArray(typeVariableArrayName("X", 1)).withComponentType(
                                        typeVariable("X").withUpperBounds(Serializable.class)))),
                parameterizedType(Map.class).withTypeArguments(
                        wildcardType().withLowerBound(
                                genericArray(typeVariableArrayName("Y", 1)).withComponentType(
                                        typeVariable("Y").withUpperBounds(String.class))),
                        parameterizedType(Map.class).withTypeArguments(
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withLowerBound(
                                                genericArray(typeVariableArrayName("Y", 3)).withComponentType(
                                                        genericArray(typeVariableArrayName("Y", 2)).withComponentType(
                                                                genericArray(typeVariableArrayName("Y", 1)).withComponentType(
                                                                        typeVariable("Y").withUpperBounds(String.class))))),
                                        wildcardType()),
                                genericArray(typeVariableArrayName("X", 2)).withComponentType(
                                        genericArray(typeVariableArrayName("X", 1)).withComponentType(
                                                typeVariable("X").withUpperBounds(Serializable.class)))))
        );
    }

    @DataProvider
    public static Object[][] data_imports_multiple_generic_code_unit_parameter_types() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            GenericSignatureOnConstructor(
                    ClassParameterWithSingleTypeParameter<List<? extends FIRST>> first,
                    ClassParameterWithSingleTypeParameter<
                            Map<
                                    Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                                    Map<? extends String,
                                            Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>
                                    >
                            > second,
                    ClassParameterWithSingleTypeParameter<Comparable<GenericSignatureOnConstructor<FIRST, SECOND>>> third) {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            void method(
                    ClassParameterWithSingleTypeParameter<List<? extends FIRST>> first,
                    ClassParameterWithSingleTypeParameter<
                            Map<
                                    Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                                    Map<? extends String,
                                            Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>
                                    >
                            > second,
                    ClassParameterWithSingleTypeParameter<Comparable<GenericSignatureOnMethod<FIRST, SECOND>>> third) {
            }
        }

        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class, Cloneable.class,
                List.class, Map.class, Map.Entry.class, Set.class, Iterable.class, Comparable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_generic_code_unit_parameter_types(JavaCodeUnit codeUnit) {
        List<JavaType> genericParameterTypes = codeUnit.getParameterTypes();

        // @formatter:off
        assertThatType(genericParameterTypes.get(0)).as("first generic parameter type").hasActualTypeArguments(
            parameterizedType(List.class)
                .withWildcardTypeParameterWithUpperBound(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class)));

        assertThatType(genericParameterTypes.get(1)).as("second generic parameter type").hasActualTypeArguments(
            parameterizedType(Map.class).withTypeArguments(
                parameterizedType(Map.Entry.class).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    parameterizedType(Map.Entry.class).withTypeArguments(
                        concreteClass(String.class),
                        typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))),
                parameterizedType(Map.class).withTypeArguments(
                    wildcardType().withUpperBound(String.class),
                    parameterizedType(Map.class).withTypeArguments(
                        wildcardType().withUpperBound(Serializable.class),
                        parameterizedType(List.class).withTypeArguments(
                            parameterizedType(List.class).withTypeArguments(
                                wildcardType().withUpperBound(
                                    parameterizedType(Set.class).withTypeArguments(
                                        wildcardType().withLowerBound(
                                            parameterizedType(Iterable.class).withTypeArguments(
                                                wildcardType().withLowerBound(
                                                    parameterizedType(Map.class).withTypeArguments(
                                                        typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class),
                                                        wildcardType()))))))))))));

        assertThatType(genericParameterTypes.get(2)).as("third generic parameter type").hasActualTypeArguments(
            parameterizedType(Comparable.class).withTypeArguments(
                parameterizedType(codeUnit.getOwner().reflect()).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))));
        // @formatter:on
    }

    private static Object[][] testCasesFromSameGenericSignatureOnConstructorAndMethod(
            Class<?> genericSignatureOnConstructor,
            Class<?> genericSignatureOnMethod,
            Class<?>... additionalImports) {
        final List<Class<?>> toImport = FluentIterable.from(additionalImports).append(genericSignatureOnConstructor).append(genericSignatureOnMethod).toList();
        JavaClasses classes = ArchConfigurationRule.resetConfigurationAround(new Callable<JavaClasses>() {
            @Override
            public JavaClasses call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                return new ClassFileImporter().importClasses(toImport);
            }
        });

        return testForEach(
                getOnlyElement(classes.get(genericSignatureOnConstructor).getConstructors()),
                getOnlyElement(classes.get(genericSignatureOnMethod).getMethods())
        );
    }

    @SuppressWarnings("unused")
    public static class ClassParameterWithSingleTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    public static class ClassParameterWithTwoTypeParameters<A, B> {
    }

    @SuppressWarnings("unused")
    public static class ClassParameterWithThreeTypeParameters<A, B, C> {
    }

    @SuppressWarnings("unused")
    public interface InterfaceParameterWithSingleTypeParameter<T> {
    }
}
