package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporterGenericInterfacesTest.Outer.SomeNestedInterface;
import com.tngtech.archunit.core.importer.ClassFileImporterGenericInterfacesTest.Outer.SomeNestedInterface.SomeDeeplyNestedInterface;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.genericArray;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.parameterizedTypeArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.typeVariableArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteWildcardType.wildcardType;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterGenericInterfacesTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    @Test
    public void imports_non_generic_interface() {
        class Child implements SomeInterface {
        }

        Set<JavaType> genericInterfaces = new ClassFileImporter().importClasses(Child.class, SomeInterface.class)
                .get(Child.class).getInterfaces();

        assertThatTypes(genericInterfaces).as("generic interfaces").matchExactly(SomeInterface.class);
    }

    @Test
    public void imports_generic_interface_with_one_type_argument() {
        class Child implements InterfaceWithOneTypeParameter<String> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(Child.class, InterfaceWithOneTypeParameter.class, String.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface")
                .hasErasure(InterfaceWithOneTypeParameter.class)
                .hasActualTypeArguments(String.class);
    }

    @Test
    public void imports_raw_generic_superclass_as_JavaClass_instead_of_JavaParameterizedType() {
        @SuppressWarnings("rawtypes")
        class Child implements InterfaceWithOneTypeParameter {
        }

        JavaType rawGenericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(Child.class, InterfaceWithOneTypeParameter.class)
                        .get(Child.class).getInterfaces());

        assertThatType(rawGenericInterface).as("raw generic interface").matches(InterfaceWithOneTypeParameter.class);
    }

    @Test
    public void imports_generic_interface_with_array_type_argument() {
        class Child implements InterfaceWithOneTypeParameter<String[]> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(Child.class, InterfaceWithOneTypeParameter.class, String.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface")
                .hasErasure(InterfaceWithOneTypeParameter.class)
                .hasActualTypeArguments(String[].class);
    }

    @Test
    public void imports_generic_interface_with_primitive_array_type_argument() {
        class Child implements InterfaceWithOneTypeParameter<int[]> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(Child.class, InterfaceWithOneTypeParameter.class, int.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface")
                .hasErasure(InterfaceWithOneTypeParameter.class)
                .hasActualTypeArguments(int[].class);
    }

    @Test
    public void imports_generic_interface_with_multiple_type_arguments() {
        @SuppressWarnings("unused")
        class Child implements InterfaceWithThreeTypeParameters<String, Serializable, File> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithThreeTypeParameters.class, String.class, Serializable.class, File.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface")
                .hasErasure(InterfaceWithThreeTypeParameters.class)
                .hasActualTypeArguments(String.class, Serializable.class, File.class);
    }

    @Test
    public void imports_generic_interface_with_single_actual_type_argument_parameterized_with_concrete_class() {
        class Child implements InterfaceWithOneTypeParameter<ClassParameterWithSingleTypeParameter<String>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithOneTypeParameter.class, ClassParameterWithSingleTypeParameter.class, String.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_interface_with_multiple_actual_type_arguments_parameterized_with_concrete_classes() {
        class Child implements InterfaceWithThreeTypeParameters<
                ClassParameterWithSingleTypeParameter<File>,
                InterfaceWithOneTypeParameter<Serializable>,
                InterfaceWithOneTypeParameter<String>> {
        }

        JavaType genericInterface = getOnlyElement(new ClassFileImporter()
                .importClasses(
                        Child.class, ClassParameterWithSingleTypeParameter.class, InterfaceWithThreeTypeParameters.class,
                        InterfaceWithOneTypeParameter.class, File.class, Serializable.class, String.class)
                .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(File.class),
                parameterizedType(InterfaceWithOneTypeParameter.class)
                        .withTypeArguments(Serializable.class),
                parameterizedType(InterfaceWithOneTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_interface_with_single_actual_type_argument_parameterized_with_unbound_wildcard() {
        class Child implements InterfaceWithOneTypeParameter<ClassParameterWithSingleTypeParameter<?>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithOneTypeParameter.class, ClassParameterWithSingleTypeParameter.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameter()
        );
    }

    @Test
    public void imports_generic_interface_with_actual_type_arguments_parameterized_with_bounded_wildcards() {
        class Child implements InterfaceWithTwoTypeParameters<
                ClassParameterWithSingleTypeParameter<? extends String>,
                ClassParameterWithSingleTypeParameter<? super File>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, File.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(String.class),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(File.class)
        );
    }

    @Test
    public void imports_generic_interface_with_actual_type_arguments_with_multiple_wildcards_with_various_bounds() {
        class Child implements InterfaceWithTwoTypeParameters<
                ClassParameterWithSingleTypeParameter<Map<? extends Serializable, ? super File>>,
                ClassParameterWithSingleTypeParameter<Reference<? super String>>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(
                                Child.class, ClassParameterWithSingleTypeParameter.class,
                                Map.class, Serializable.class, File.class, Reference.class, String.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
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

    @Test
    public void imports_generic_interface_parameterized_with_type_variable() {
        class Child<SUB> implements InterfaceWithOneTypeParameter<SUB> {
        }

        JavaType genericInterface = getOnlyElement(new ClassFileImporter().importClasses(Child.class, InterfaceWithOneTypeParameter.class)
                .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(typeVariable("SUB"));
    }

    @Test
    public void imports_generic_interface_with_actual_type_argument_parameterized_with_type_variable() {
        class Child<SUB> implements InterfaceWithOneTypeParameter<ClassParameterWithSingleTypeParameter<SUB>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithOneTypeParameter.class, ClassParameterWithSingleTypeParameter.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("SUB"))
        );
    }

    @Test
    public void references_type_variable_assigned_to_actual_type_argument_of_generic_interface() {
        class Child<SUB extends String> implements InterfaceWithOneTypeParameter<ClassParameterWithSingleTypeParameter<SUB>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithOneTypeParameter.class, ClassParameterWithSingleTypeParameter.class, String.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("SUB").withUpperBounds(String.class))
        );
    }

    @Test
    public void references_outer_type_variable_assigned_to_actual_type_argument_of_generic_interface_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class Child implements InterfaceWithOneTypeParameter<OUTER> {
                }
            }
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(
                                OuterWithTypeParameter.class,
                                OuterWithTypeParameter.SomeInner.class,
                                OuterWithTypeParameter.SomeInner.Child.class,
                                InterfaceWithOneTypeParameter.class,
                                String.class)
                        .get(OuterWithTypeParameter.SomeInner.Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                typeVariable("OUTER").withUpperBounds(String.class)
        );
    }

    @Test
    public void creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context_for_generic_interface_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class Child implements InterfaceWithOneTypeParameter<OUTER> {
                }
            }
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(OuterWithTypeParameter.SomeInner.Child.class, InterfaceWithOneTypeParameter.class, String.class)
                        .get(OuterWithTypeParameter.SomeInner.Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                typeVariable("OUTER").withoutUpperBounds()
        );
    }

    @Test
    public void imports_wildcards_of_generic_interface_bound_by_type_variables() {
        class Child<FIRST extends String, SECOND extends Serializable> implements InterfaceWithTwoTypeParameters<
                ClassParameterWithSingleTypeParameter<? extends FIRST>,
                ClassParameterWithSingleTypeParameter<? super SECOND>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, InterfaceWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("FIRST").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("SECOND").withUpperBounds(Serializable.class))
        );
    }

    @Test
    public void imports_wildcards_of_generic_interface_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class Child implements InterfaceWithTwoTypeParameters<
                        ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                        ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> {
                }
            }
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(
                                OuterWithTypeParameter.class,
                                OuterWithTypeParameter.SomeInner.class,
                                OuterWithTypeParameter.SomeInner.Child.class,
                                InterfaceWithTwoTypeParameters.class, ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class)
                        .get(OuterWithTypeParameter.SomeInner.Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withUpperBounds(Serializable.class))
        );
    }

    @Test
    public void creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class Child implements InterfaceWithTwoTypeParameters<
                        ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                        ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> {
                }
            }
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(
                                OuterWithTypeParameter.SomeInner.Child.class,
                                ClassParameterWithSingleTypeParameter.class, String.class, Serializable.class)
                        .get(OuterWithTypeParameter.SomeInner.Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withoutUpperBounds()),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withoutUpperBounds())
        );
    }

    private static class Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions {
        static class ClassChild<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> implements InterfaceWithThreeTypeParameters<
                // assigned to InterfaceWithThreeTypeParameters<A,_,_>
                List<? extends FIRST>,
                // assigned to InterfaceWithThreeTypeParameters<_,B,_>
                Map<
                        Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                        Map<? extends String,
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                // assigned to InterfaceWithThreeTypeParameters<_,_,C>
                Comparable<ClassChild<FIRST, SECOND>>> {
        }

        interface InterfaceChild<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> extends InterfaceWithThreeTypeParameters<
                // assigned to InterfaceWithThreeTypeParameters<A,_,_>
                List<? extends FIRST>,
                // assigned to InterfaceWithThreeTypeParameters<_,B,_>
                Map<
                        Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                        Map<? extends String,
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                // assigned to InterfaceWithThreeTypeParameters<_,_,C>
                Comparable<InterfaceChild<FIRST, SECOND>>> {
        }
    }

    @DataProvider
    public static Object[][] data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions() {
        return testForEach(
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions.ClassChild.class,
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions.InterfaceChild.class
        );
    }

    @Test
    @UseDataProvider("data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions")
    public void imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_self_referencing_type_definitions(Class<?> testInput) {
        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(testInput, String.class, Serializable.class, Cloneable.class,
                                List.class, Map.class, Map.Entry.class, Set.class, Iterable.class, Comparable.class)
                        .get(testInput).getInterfaces());

        // @formatter:off
        assertThatType(genericInterface).as("generic interface").hasActualTypeArguments(
            // assigned to InterfaceWithThreeTypeParameters<A,_,_>
            parameterizedType(List.class)
                .withWildcardTypeParameterWithUpperBound(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class)),
            // assigned to InterfaceWithThreeTypeParameters<_,B,_>
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
            // assigned to InterfaceWithThreeTypeParameters<_,_,C>
            parameterizedType(Comparable.class).withTypeArguments(
                parameterizedType(testInput).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))));
        // @formatter:on
    }

    private static class Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds {
        static class ClassChild implements InterfaceWithThreeTypeParameters<
                List<Serializable[]>,
                List<? extends Serializable[][]>,
                Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> {
        }

        interface InterfaceChild extends InterfaceWithThreeTypeParameters<
                List<Serializable[]>,
                List<? extends Serializable[][]>,
                Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> {
        }
    }

    @DataProvider
    public static Object[][] data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds() {
        return testForEach(
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds.ClassChild.class,
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds.InterfaceChild.class
        );
    }

    @Test
    @UseDataProvider("data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds")
    public void imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_concrete_array_bounds(Class<?> testInput) {
        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(testInput, List.class, Serializable.class, Map.class, String.class)
                        .get(testInput).getInterfaces());

        assertThatType(genericInterface).hasActualTypeArguments(
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

    private static class Data_of_imports_type_of_generic_interface_with_parameterized_array_bounds {
        static class ClassChild implements InterfaceWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]> {
        }

        interface InterfaceChild extends InterfaceWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]> {
        }
    }

    @DataProvider
    public static Object[][] data_of_imports_type_of_generic_interface_with_parameterized_array_bounds() {
        return testForEach(
                Data_of_imports_type_of_generic_interface_with_parameterized_array_bounds.ClassChild.class,
                Data_of_imports_type_of_generic_interface_with_parameterized_array_bounds.InterfaceChild.class
        );
    }

    @Test
    @UseDataProvider("data_of_imports_type_of_generic_interface_with_parameterized_array_bounds")
    public void imports_type_of_generic_interface_with_parameterized_array_bounds(Class<?> testInput) {
        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(testInput, InterfaceWithThreeTypeParameters.class, List.class, String.class)
                        .get(testInput).getInterfaces());

        assertThatType(genericInterface).hasActualTypeArguments(
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

    private static class Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds {
        static class ClassChild<X extends Serializable, Y extends String> implements InterfaceWithThreeTypeParameters<
                List<X[]>,
                List<? extends X[][]>,
                Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> {
        }

        interface InterfaceChild<X extends Serializable, Y extends String> extends InterfaceWithThreeTypeParameters<
                List<X[]>,
                List<? extends X[][]>,
                Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> {
        }
    }

    @DataProvider
    public static Object[][] data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds() {
        return testForEach(
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds.ClassChild.class,
                Data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds.InterfaceChild.class
        );
    }

    @Test
    @UseDataProvider("data_of_imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds")
    public void imports_complex_type_with_multiple_nested_actual_type_arguments_of_generic_interface_with_generic_array_bounds(Class<?> testInput) {
        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter().importClasses(testInput, List.class, Serializable.class, Map.class, String.class)
                        .get(testInput).getInterfaces());

        assertThatType(genericInterface).hasActualTypeArguments(
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

    private static class Data_of_imports_multiple_generic_interfaces {
        @SuppressWarnings("unused")
        static class ClassChild<T> implements
                InterfaceWithOneTypeParameter<Path>,
                InterfaceWithTwoTypeParameters<List<? extends T>, Map<? super T, String>>,
                InterfaceWithThreeTypeParameters<String, Serializable, File> {
        }

        interface InterfaceChild<T> extends
                InterfaceWithOneTypeParameter<Path>,
                InterfaceWithTwoTypeParameters<List<? extends T>, Map<? super T, String>>,
                InterfaceWithThreeTypeParameters<String, Serializable, File> {
        }
    }

    @DataProvider
    public static Object[][] data_of_imports_multiple_generic_interfaces() {
        return testForEach(
                Data_of_imports_multiple_generic_interfaces.ClassChild.class,
                Data_of_imports_multiple_generic_interfaces.InterfaceChild.class);
    }

    @Test
    @UseDataProvider("data_of_imports_multiple_generic_interfaces")
    public void imports_multiple_generic_interfaces(Class<?> testInput) {
        JavaClass child = new ClassFileImporter()
                .importClasses(testInput,
                        InterfaceWithOneTypeParameter.class, InterfaceWithTwoTypeParameters.class, InterfaceWithThreeTypeParameters.class,
                        Path.class, List.class, Map.class, String.class, Serializable.class, File.class)
                .get(testInput);

        assertThatType(getGenericInterface(child, InterfaceWithOneTypeParameter.class)).as("generic interface")
                .hasActualTypeArguments(Path.class);

        assertThatType(getGenericInterface(child, InterfaceWithTwoTypeParameters.class)).as("generic interface")
                .hasActualTypeArguments(
                        parameterizedType(List.class).withTypeArguments(
                                wildcardType().withUpperBound(typeVariable("T"))),
                        parameterizedType(Map.class).withTypeArguments(
                                wildcardType().withLowerBound(typeVariable("T")),
                                concreteClass(String.class)));

        assertThatType(getGenericInterface(child, InterfaceWithThreeTypeParameters.class)).as("generic interface")
                .hasActualTypeArguments(String.class, Serializable.class, File.class);
    }

    @Test
    public void imports_generic_superclass_and_multiple_generic_interfaces_in_combination() {
        @SuppressWarnings("unused")
        class BaseClass<X> {
        }
        @SuppressWarnings("unused")
        class Child<T> extends BaseClass<File>
                implements InterfaceWithOneTypeParameter<Path>, InterfaceWithTwoTypeParameters<T, String> {
        }

        JavaClass child = new ClassFileImporter()
                .importClasses(Child.class,
                        InterfaceWithOneTypeParameter.class, InterfaceWithTwoTypeParameters.class, InterfaceWithThreeTypeParameters.class,
                        Path.class, List.class, Map.class, String.class, Serializable.class, File.class)
                .get(Child.class);

        assertThatType(child.getSuperclass().get())
                .hasErasure(BaseClass.class)
                .hasActualTypeArguments(File.class);

        assertThatType(getGenericInterface(child, InterfaceWithOneTypeParameter.class)).as("generic interface")
                .hasActualTypeArguments(Path.class);

        assertThatType(getGenericInterface(child, InterfaceWithTwoTypeParameters.class)).as("generic interface")
                .hasActualTypeArguments(typeVariable("T"), concreteClass(String.class));
    }

    @Test
    public void imports_nested_generic_interfaces() {
        @SuppressWarnings("unused")
        class Child<T> implements
                SomeDeeplyNestedInterface<File, SomeNestedInterface<Path, Path>> {
        }

        JavaType genericInterface = getOnlyElement(
                new ClassFileImporter()
                        .importClasses(Child.class, SomeDeeplyNestedInterface.class, SomeNestedInterface.class, File.class, Path.class)
                        .get(Child.class).getInterfaces());

        assertThatType(genericInterface).as("generic interface")
                .hasErasure(SomeDeeplyNestedInterface.class)
                .hasActualTypeArguments(
                        concreteClass(File.class),
                        parameterizedType(SomeNestedInterface.class).withTypeArguments(Path.class, Path.class));
    }

    private JavaType getGenericInterface(JavaClass javaClass, Class<?> rawType) {
        for (JavaType anInterface : javaClass.getInterfaces()) {
            if (anInterface.toErasure().isEquivalentTo(rawType)) {
                return anInterface;
            }
        }
        throw new AssertionError(String.format("Class %s has no interface of raw type %s", javaClass.getName(), rawType.getName()));
    }

    private interface SomeInterface {
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithOneTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithTwoTypeParameters<A, B> {
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithThreeTypeParameters<A, B, C> {
    }

    @SuppressWarnings("unused")
    static class Outer {
        interface SomeNestedInterface<A, B> {
            interface SomeDeeplyNestedInterface<C, D> extends SomeNestedInterface<C, String> {
            }
        }
    }

    @SuppressWarnings("unused")
    private static class ClassParameterWithSingleTypeParameter<T> {
    }
}
