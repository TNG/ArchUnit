package com.tngtech.archunit.core.importer;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatCodeUnit;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.genericArray;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.parameterizedTypeArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.typeVariableArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteWildcardType.wildcardType;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterGenericMethodSignaturesTest {

    @DataProvider
    public static Object[][] data_imports_empty_list_of_type_parameters_for_non_generic_code_unit() {
        @SuppressWarnings("unused")
        class NoGenericSignatureOnConstructor {
            NoGenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class NoGenericSignatureOnMethod {
            void noGenericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(NoGenericSignatureOnConstructor.class, NoGenericSignatureOnMethod.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_empty_list_of_type_parameters_for_non_generic_code_unit(JavaCodeUnit codeUnit) {
        assertThat(codeUnit.getTypeParameters()).as("type parameters of non generic code unit").isEmpty();
    }

    @DataProvider
    public static Object[][] data_imports_single_generic_type_parameter_of_code_unit() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                Object.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_generic_type_parameter_of_code_unit(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T").withBoundsMatching(Object.class);
    }

    @DataProvider
    public static Object[][] data_imports_multiple_generic_type_parameters_of_code_unit() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <A, B, C> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <A, B, C> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_generic_type_parameters_of_code_unit(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("A", "B", "C");
    }

    @DataProvider
    public static Object[][] data_imports_simple_class_bound_of_type_variable() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends String> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends String> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_simple_class_bound_of_type_variable(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T").withBoundsMatching(String.class);
    }

    @DataProvider
    public static Object[][] data_imports_single_simple_class_bounds_of_multiple_type_variables() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <A extends String, B extends System, C extends File> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <A extends String, B extends System, C extends File> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                String.class, System.class, File.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_simple_class_bounds_of_multiple_type_variables(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(String.class)
                .hasTypeParameter("B").withBoundsMatching(System.class)
                .hasTypeParameter("C").withBoundsMatching(File.class);
    }

    @DataProvider
    public static Object[][] data_imports_simple_interface_bound_of_single_type_variable() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends Serializable> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends Serializable> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_simple_interface_bound_of_single_type_variable(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T").withBoundsMatching(Serializable.class);
    }

    @DataProvider
    public static Object[][] data_imports_multiple_simple_bounds_of_single_type_variable() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends String & Serializable & Runnable> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends String & Serializable & Runnable> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                String.class, Serializable.class, Runnable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_simple_bounds_of_single_type_variable(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T").withBoundsMatching(String.class, Serializable.class, Runnable.class);
    }

    @DataProvider
    public static Object[][] data_imports_multiple_simple_bounds_of_multiple_type_variables() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <A extends String & Serializable, B extends System & Runnable, C extends File & Serializable & Closeable> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <A extends String & Serializable, B extends System & Runnable, C extends File & Serializable & Closeable> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                String.class, Serializable.class, System.class, Runnable.class, File.class, Serializable.class, Closeable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_simple_bounds_of_multiple_type_variables(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(String.class, Serializable.class)
                .hasTypeParameter("B").withBoundsMatching(System.class, Runnable.class)
                .hasTypeParameter("C").withBoundsMatching(File.class, Serializable.class, Closeable.class);
    }

    @DataProvider
    public static Object[][] data_imports_single_class_bound_with_single_type_parameter_assigned_to_concrete_class() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends ClassParameterWithSingleTypeParameter<String>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends ClassParameterWithSingleTypeParameter<String>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_class_bound_with_single_type_parameter_assigned_to_concrete_class(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_single_class_bound_with_single_type_parameter_assigned_to_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends ClassParameterWithSingleTypeParameter<String[]>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends ClassParameterWithSingleTypeParameter<String[]>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_class_bound_with_single_type_parameter_assigned_to_array_type_argument(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String[].class));
    }

    @DataProvider
    public static Object[][] data_imports_single_class_bound_with_single_type_parameter_assigned_to_primitive_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends ClassParameterWithSingleTypeParameter<int[]>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends ClassParameterWithSingleTypeParameter<int[]>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_class_bound_with_single_type_parameter_assigned_to_primitive_array_type_argument(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(int[].class));
    }

    @DataProvider
    public static Object[][] data_imports_multiple_class_bounds_with_single_type_parameters_assigned_to_concrete_types() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <
                    A extends ClassParameterWithSingleTypeParameter<File>,
                    B extends InterfaceParameterWithSingleTypeParameter<Serializable>,
                    C extends InterfaceParameterWithSingleTypeParameter<String>
                    > GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <
                    A extends ClassParameterWithSingleTypeParameter<File>,
                    B extends InterfaceParameterWithSingleTypeParameter<Serializable>,
                    C extends InterfaceParameterWithSingleTypeParameter<String>
                    > void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, File.class, InterfaceParameterWithSingleTypeParameter.class, Serializable.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_class_bounds_with_single_type_parameters_assigned_to_concrete_types(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(File.class))
                .hasTypeParameter("B").withBoundsMatching(parameterizedType(InterfaceParameterWithSingleTypeParameter.class).withTypeArguments(Serializable.class))
                .hasTypeParameter("C").withBoundsMatching(parameterizedType(InterfaceParameterWithSingleTypeParameter.class).withTypeArguments(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_multiple_class_bounds_with_multiple_type_parameters_assigned_to_concrete_types() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <
                    A extends ClassParameterWithSingleTypeParameter<String> & InterfaceParameterWithSingleTypeParameter<Serializable>,
                    B extends Map<String, Serializable> & Iterable<File> & Function<Integer, Long>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <
                    A extends ClassParameterWithSingleTypeParameter<String> & InterfaceParameterWithSingleTypeParameter<Serializable>,
                    B extends Map<String, Serializable> & Iterable<File> & Function<Integer, Long>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassParameterWithSingleTypeParameter.class, InterfaceParameterWithSingleTypeParameter.class,
                Map.class, Iterable.class, Function.class, String.class, Serializable.class, File.class, Integer.class, Long.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_class_bounds_with_multiple_type_parameters_assigned_to_concrete_types(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("A", "B")
                .hasTypeParameter("A")
                .withBoundsMatching(
                        parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String.class),
                        parameterizedType(InterfaceParameterWithSingleTypeParameter.class).withTypeArguments(Serializable.class))
                .hasTypeParameter("B")
                .withBoundsMatching(
                        parameterizedType(Map.class).withTypeArguments(String.class, Serializable.class),
                        parameterizedType(Iterable.class).withTypeArguments(File.class),
                        parameterizedType(Function.class).withTypeArguments(Integer.class, Long.class));
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends List<?>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends List<?>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class, List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_unbound_wildcard(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T").withBoundsMatching(parameterizedType(List.class).withWildcardTypeParameter());
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_wildcard_upper_bound_by_class() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends List<? extends String>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends List<? extends String>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_wildcard_upper_bound_by_class(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T")
                .withBoundsMatching(parameterizedType(List.class)
                        .withWildcardTypeParameterWithUpperBound(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_wildcard_upper_bound_by_interface() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends List<? extends Serializable>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends List<? extends Serializable>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_wildcard_upper_bound_by_interface(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T")
                .withBoundsMatching(parameterizedType(List.class)
                        .withWildcardTypeParameterWithUpperBound(Serializable.class));
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_wildcard_lower_bound_by_class() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends List<? super String>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends List<? super String>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_wildcard_lower_bound_by_class(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T")
                .withBoundsMatching(parameterizedType(List.class)
                        .withWildcardTypeParameterWithLowerBound(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_wildcard_lower_bound_by_interface() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends List<? super Serializable>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends List<? super Serializable>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, Serializable.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_wildcard_lower_bound_by_interface(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T")
                .withBoundsMatching(parameterizedType(List.class)
                        .withWildcardTypeParameterWithLowerBound(Serializable.class));
    }

    @DataProvider
    public static Object[][] data_imports_multiple_type_bounds_with_multiple_wildcards_with_various_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <A extends Map<? extends Serializable, ? super File>, B extends Reference<? super String> & Map<?, ?>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <A extends Map<? extends Serializable, ? super File>, B extends Reference<? super String> & Map<?, ?>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                Map.class, Serializable.class, File.class, Reference.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_multiple_type_bounds_with_multiple_wildcards_with_various_bounds(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("A", "B")
                .hasTypeParameter("A")
                .withBoundsMatching(
                        parameterizedType(Map.class)
                                .withWildcardTypeParameters(
                                        wildcardType().withUpperBound(Serializable.class),
                                        wildcardType().withLowerBound(File.class)
                                ))
                .hasTypeParameter("B")
                .withBoundsMatching(
                        parameterizedType(Reference.class)
                                .withWildcardTypeParameters(
                                        wildcardType().withLowerBound(String.class)
                                ),
                        parameterizedType(Map.class)
                                .withWildcardTypeParameters(
                                        wildcardType(),
                                        wildcardType()
                                ));
    }

    @DataProvider
    public static Object[][] data_references_type_variable_bound() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <U extends T, T extends String, V extends T> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <U extends T, T extends String, V extends T> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_references_type_variable_bound(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("U", "T", "V")
                .hasTypeParameter("U").withBoundsMatching(typeVariable("T").withUpperBounds(String.class))
                .hasTypeParameter("V").withBoundsMatching(typeVariable("T").withUpperBounds(String.class));
    }

    @DataProvider
    public static Object[][] data_references_type_variable_bound_for_inner_classes() {
        @SuppressWarnings("unused")
        class Outer<U extends T, T extends String> {
            class SomeInner {
                class EvenMoreInnerDeclaringOwn<V extends U, MORE_INNER1, MORE_INNER2 extends U> {
                    @SuppressWarnings("unused")
                    class GenericSignatureOnConstructor {
                        <MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> GenericSignatureOnConstructor() {
                        }
                    }

                    @SuppressWarnings("unused")
                    class GenericSignatureOnMethod {
                        <MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> void genericSignatureOnMethod() {
                        }
                    }
                }
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Outer.SomeInner.EvenMoreInnerDeclaringOwn.GenericSignatureOnConstructor.class,
                Outer.SomeInner.EvenMoreInnerDeclaringOwn.GenericSignatureOnMethod.class,
                Outer.class,
                Outer.SomeInner.class,
                Outer.SomeInner.EvenMoreInnerDeclaringOwn.class,
                String.class);
    }

    @Test
    @UseDataProvider
    public void test_references_type_variable_bound_for_inner_classes(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(
                        typeVariable("T").withUpperBounds(String.class))
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(
                        typeVariable("MORE_INNER2").withUpperBounds(
                                typeVariable("U").withUpperBounds(
                                        typeVariable("T").withUpperBounds(String.class))));
    }

    @DataProvider
    public static Object[][] data_creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class Outer<U extends T, T extends String> {
            @SuppressWarnings("InnerClassMayBeStatic")
            class SomeInner {
                class EvenMoreInnerDeclaringOwn<V extends U, MORE_INNER1, MORE_INNER2 extends U> {
                    @SuppressWarnings("unused")
                    class GenericSignatureOnConstructor {
                        <MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> GenericSignatureOnConstructor() {
                        }
                    }

                    @SuppressWarnings("unused")
                    class GenericSignatureOnMethod {
                        <MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> void genericSignatureOnMethod() {
                        }
                    }
                }
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Outer.SomeInner.EvenMoreInnerDeclaringOwn.GenericSignatureOnConstructor.class,
                Outer.SomeInner.EvenMoreInnerDeclaringOwn.GenericSignatureOnMethod.class);
    }

    @Test
    @UseDataProvider
    public void test_creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(typeVariable("T").withoutUpperBounds())
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(typeVariable("MORE_INNER2").withoutUpperBounds());
    }

    @DataProvider
    public static Object[][] data_considers_hierarchy_of_methods_and_classes_for_type_parameter_context() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class Level1<T1 extends String> {
            <T2 extends T1> void level2() {
                class Level3<T3 extends T2> {
                    @SuppressWarnings("unused")
                    class GenericSignatureOnConstructor {
                        <T41 extends T3, T42 extends T1> GenericSignatureOnConstructor() {
                        }
                    }

                    @SuppressWarnings("unused")
                    class GenericSignatureOnMethod {
                        <T41 extends T3, T42 extends T1> void genericSignatureOnMethod() {
                        }
                    }
                }
            }
        }
        String level3ClassName = Level1.class.getName() + "$1Level3";
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Class.forName(level3ClassName + "$GenericSignatureOnConstructor"),
                Class.forName(level3ClassName + "$GenericSignatureOnMethod"),
                Class.forName(level3ClassName), Level1.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_considers_hierarchy_of_methods_and_classes_for_type_parameter_context(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("T41", "T42")
                .hasTypeParameter("T41")
                .withBoundsMatching(
                        typeVariable("T3").withUpperBounds(
                                typeVariable("T2").withUpperBounds(
                                        typeVariable("T1").withUpperBounds(String.class))))
                .hasTypeParameter("T42")
                .withBoundsMatching(typeVariable("T1").withUpperBounds(String.class));
    }

    @DataProvider
    public static Object[][] data_imports_wild_cards_bound_by_type_variables() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <T extends String, U extends List<? extends T>, V extends List<? super T>> GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <T extends String, U extends List<? extends T>, V extends List<? super T>> void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                GenericSignatureOnConstructor.class,
                GenericSignatureOnMethod.class,
                List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_wild_cards_bound_by_type_variables(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("T", "U", "V")
                .hasTypeParameter("U")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                typeVariable("T").withUpperBounds(String.class)))
                .hasTypeParameter("V")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                typeVariable("T").withUpperBounds(String.class)));
    }

    @DataProvider
    public static Object[][] data_imports_wild_cards_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class Outer<T extends String, U extends List<? extends T>, V extends List<? super T>> {
            class Inner<MORE_INNER extends List<? extends U>> {
                @SuppressWarnings("unused")
                class GenericSignatureOnConstructor {
                    <MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> GenericSignatureOnConstructor() {
                    }
                }

                @SuppressWarnings("unused")
                class GenericSignatureOnMethod {
                    <MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> void genericSignatureOnMethod() {
                    }
                }
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Outer.Inner.GenericSignatureOnConstructor.class,
                Outer.Inner.GenericSignatureOnMethod.class,
                Outer.class,
                Outer.Inner.class,
                List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_wild_cards_bound_by_type_variables_of_enclosing_classes(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                typeVariable("T").withUpperBounds(String.class)))
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                typeVariable("V").withUpperBounds(
                                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                                typeVariable("T").withUpperBounds(String.class)))));
    }

    @DataProvider
    public static Object[][] data_creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class Outer<T extends String, U extends List<? extends T>, V extends List<? super T>> {
            class Inner<MORE_INNER extends List<? extends U>> {
                @SuppressWarnings("unused")
                class GenericSignatureOnConstructor {
                    <MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> GenericSignatureOnConstructor() {
                    }
                }

                @SuppressWarnings("unused")
                class GenericSignatureOnMethod {
                    <MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> void genericSignatureOnMethod() {
                    }
                }
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(
                Outer.Inner.GenericSignatureOnConstructor.class,
                Outer.Inner.GenericSignatureOnMethod.class,
                List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                typeVariable("T").withoutUpperBounds()))
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                typeVariable("V").withoutUpperBounds()));
    }

    @DataProvider
    public static Object[][] data_imports_complex_type_with_multiple_nested_parameters_with_various_bounds_and_recursive_type_definitions() {
        @SuppressWarnings({"unused", "rawtypes"})
        class GenericSignatureOnConstructor {
            <
                    A extends List<?> & Serializable & Comparable<A>,
                    B extends A,
                    C extends Map<
                            Map.Entry<A, Map.Entry<String, B>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<B, ?>>>>>>>>,
                    SELF extends GenericSignatureOnConstructor,
                    D,
                    RAW extends List
                    > GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings({"unused", "rawtypes"})
        class GenericSignatureOnMethod {
            <
                    A extends List<?> & Serializable & Comparable<A>,
                    B extends A,
                    C extends Map<
                            Map.Entry<A, Map.Entry<String, B>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<B, ?>>>>>>>>,
                    SELF extends GenericSignatureOnMethod,
                    D,
                    RAW extends List
                    > void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, Serializable.class, Comparable.class, Map.class, Map.Entry.class, String.class, Set.class, Iterable.class, Object.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_type_with_multiple_nested_parameters_with_various_bounds_and_recursive_type_definitions(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("A")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameter(),
                        concreteClass(Serializable.class),
                        parameterizedType(Comparable.class).withTypeArguments(typeVariable("A")))
                .hasTypeParameter("B")
                .withBoundsMatching(typeVariable("A"))
                .hasTypeParameter("C")
                .withBoundsMatching(
                        parameterizedType(Map.class).withTypeArguments(
                                parameterizedType(Map.Entry.class).withTypeArguments(
                                        typeVariable("A"),
                                        parameterizedType(Map.Entry.class).withTypeArguments(
                                                concreteClass(String.class), typeVariable("B"))),
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withUpperBound(String.class),
                                        parameterizedType(Map.class).withTypeArguments(
                                                wildcardType().withUpperBound(Serializable.class),
                                                parameterizedType(List.class).withTypeArguments(
                                                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                                                parameterizedType(Set.class).withWildcardTypeParameterWithLowerBound(
                                                                        parameterizedType(Iterable.class).withWildcardTypeParameterWithLowerBound(
                                                                                parameterizedType(Map.class).withTypeArguments(
                                                                                        typeVariable("B"), wildcardType())))))
                                        )
                                )
                        ))
                .hasTypeParameter("SELF")
                .withBoundsMatching(codeUnit.getOwner().reflect())
                .hasTypeParameter("D").withBoundsMatching(Object.class)
                .hasTypeParameter("RAW").withBoundsMatching(List.class);
    }

    @DataProvider
    public static Object[][] data_imports_complex_type_with_multiple_nested_parameters_with_concrete_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <
                    A extends List<Serializable[]>,
                    B extends List<? extends Serializable[][]>,
                    C extends Map<? super String[], Map<Map<? super String[][][], ?>, int[][]>>
                    > GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <
                    A extends List<Serializable[]>,
                    B extends List<? extends Serializable[][]>,
                    C extends Map<? super String[], Map<Map<? super String[][][], ?>, int[][]>>
                    > void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, Serializable.class, Map.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_type_with_multiple_nested_parameters_with_concrete_array_bounds(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("A")
                .withBoundsMatching(
                        parameterizedType(List.class).withTypeArguments(Serializable[].class))
                .hasTypeParameter("B")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(Serializable[][].class))
                .hasTypeParameter("C")
                .withBoundsMatching(
                        parameterizedType(Map.class).withTypeArguments(
                                wildcardType().withLowerBound(String[].class),
                                parameterizedType(Map.class).withTypeArguments(
                                        parameterizedType(Map.class).withTypeArguments(
                                                wildcardType().withLowerBound(String[][][].class),
                                                wildcardType()),
                                        concreteClass(int[][].class)))
                );
    }

    @DataProvider
    public static Object[][] data_imports_type_with_parameterized_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <
                    T extends ClassWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]>
                    > GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <
                    T extends ClassWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]>
                    > void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                ClassWithThreeTypeParameters.class, List.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_type_with_parameterized_array_bounds(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("T")
                .withBoundsMatching(
                        parameterizedType(ClassWithThreeTypeParameters.class).withTypeArguments(
                                genericArray(parameterizedTypeArrayName(List.class, String.class, 1)).withComponentType(
                                        parameterizedType(List.class).withTypeArguments(String.class)),
                                genericArray(parameterizedTypeArrayName(List.class, String[].class, 2)).withComponentType(
                                        genericArray(parameterizedTypeArrayName(List.class, String[].class, 1)).withComponentType(
                                                parameterizedType(List.class).withTypeArguments(String[].class))),
                                genericArray(parameterizedTypeArrayName(List.class, String[][].class, 3)).withComponentType(
                                        genericArray(parameterizedTypeArrayName(List.class, String[][].class, 2)).withComponentType(
                                                genericArray(parameterizedTypeArrayName(List.class, String[][].class, 1)).withComponentType(
                                                        parameterizedType(List.class).withTypeArguments(String[][].class))))));
    }

    @DataProvider
    public static Object[][] data_imports_complex_type_with_multiple_nested_parameters_with_generic_array_bounds() {
        @SuppressWarnings("unused")
        class GenericSignatureOnConstructor {
            <
                    X extends Serializable,
                    Y extends String,
                    A extends List<X[]>,
                    B extends List<? extends X[][]>,
                    C extends Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>
                    > GenericSignatureOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class GenericSignatureOnMethod {
            <
                    X extends Serializable,
                    Y extends String,
                    A extends List<X[]>,
                    B extends List<? extends X[][]>,
                    C extends Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>
                    > void genericSignatureOnMethod() {
            }
        }
        return testCasesFromSameGenericSignatureOnConstructorAndMethod(GenericSignatureOnConstructor.class, GenericSignatureOnMethod.class,
                List.class, Serializable.class, Map.class, String.class);
    }

    @Test
    @UseDataProvider
    public void test_imports_complex_type_with_multiple_nested_parameters_with_generic_array_bounds(JavaCodeUnit codeUnit) {
        assertThatCodeUnit(codeUnit)
                .hasTypeParameter("A")
                .withBoundsMatching(
                        parameterizedType(List.class).withTypeArguments(
                                genericArray(typeVariableArrayName("X", 1)).withComponentType(typeVariable("X").withUpperBounds(Serializable.class))))
                .hasTypeParameter("B")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                genericArray(typeVariableArrayName("X", 2)).withComponentType(
                                        genericArray(typeVariableArrayName("X", 1)).withComponentType(
                                                typeVariable("X").withUpperBounds(Serializable.class)))))
                .hasTypeParameter("C")
                .withBoundsMatching(
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
    public interface InterfaceParameterWithSingleTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    static class ClassWithThreeTypeParameters<A, B, C> {
    }
}
