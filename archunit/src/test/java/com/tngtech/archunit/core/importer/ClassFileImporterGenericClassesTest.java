package com.tngtech.archunit.core.importer;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.genericArray;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.parameterizedTypeArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.typeVariableArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteWildcardType.wildcardType;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterGenericClassesTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    @Test
    public void imports_empty_list_of_type_parameters_for_non_generic_class() {
        JavaClass javaClass = new ClassFileImporter().importClass(getClass());

        assertThat(javaClass.getTypeParameters()).as("type parameters of non generic class").isEmpty();
    }

    @Test
    public void imports_single_generic_type_parameter_of_class() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithoutBound<T> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithoutBound.class, Object.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithoutBound.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T").withBoundsMatching(Object.class);
    }

    @Test
    public void imports_multiple_generic_type_parameters_of_class() {
        @SuppressWarnings("unused")
        class ClassWithThreeTypeParametersWithoutBounds<A, B, C> {
        }

        JavaClass javaClass = new ClassFileImporter().importClass(ClassWithThreeTypeParametersWithoutBounds.class);

        assertThatType(javaClass).hasTypeParameters("A", "B", "C");
    }

    @Test
    public void imports_simple_class_bound_of_type_variable() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithSimpleClassBound<T extends String> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithSimpleClassBound.class, String.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithSimpleClassBound.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T").withBoundsMatching(String.class);
    }

    @Test
    public void imports_single_simple_class_bounds_of_multiple_type_variables() {
        @SuppressWarnings("unused")
        class ClassWithThreeTypeParametersWithSimpleClassBounds<A extends String, B extends System, C extends File> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithThreeTypeParametersWithSimpleClassBounds.class,
                String.class, System.class, File.class);

        JavaClass javaClass = classes.get(ClassWithThreeTypeParametersWithSimpleClassBounds.class);

        assertThatType(javaClass)
                .hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(String.class)
                .hasTypeParameter("B").withBoundsMatching(System.class)
                .hasTypeParameter("C").withBoundsMatching(File.class);
    }

    @Test
    public void imports_simple_interface_bound_of_single_type_variable() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithSimpleInterfaceBound<T extends Serializable> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithSimpleInterfaceBound.class, Serializable.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithSimpleInterfaceBound.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T").withBoundsMatching(Serializable.class);
    }

    @Test
    public void imports_multiple_simple_bounds_of_single_type_variable() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithMultipleSimpleClassAndInterfaceBounds<T extends String & Serializable & Runnable> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithMultipleSimpleClassAndInterfaceBounds.class,
                String.class, Serializable.class, Runnable.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithMultipleSimpleClassAndInterfaceBounds.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T").withBoundsMatching(String.class, Serializable.class, Runnable.class);
    }

    @Test
    public void imports_multiple_simple_bounds_of_multiple_type_variables() {
        @SuppressWarnings("unused")
        class ClassWithThreeTypeParametersWithMultipleSimpleClassAndInterfaceBounds<
                A extends String & Serializable, B extends System & Runnable, C extends File & Serializable & Closeable> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithThreeTypeParametersWithMultipleSimpleClassAndInterfaceBounds.class,
                String.class, Serializable.class, System.class, Runnable.class, File.class, Serializable.class, Closeable.class);

        JavaClass javaClass = classes.get(ClassWithThreeTypeParametersWithMultipleSimpleClassAndInterfaceBounds.class);

        assertThatType(javaClass)
                .hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(String.class, Serializable.class)
                .hasTypeParameter("B").withBoundsMatching(System.class, Runnable.class)
                .hasTypeParameter("C").withBoundsMatching(File.class, Serializable.class, Closeable.class);
    }

    @Test
    public void imports_single_class_bound_with_single_type_parameter_assigned_to_concrete_class() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithGenericClassBoundAssignedToConcreteClass<T extends ClassParameterWithSingleTypeParameter<String>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToConcreteClass.class,
                ClassParameterWithSingleTypeParameter.class, String.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToConcreteClass.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String.class));
    }

    @Test
    public void imports_single_class_bound_with_single_type_parameter_assigned_to_array_type_argument() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType<T extends ClassParameterWithSingleTypeParameter<String[]>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType.class,
                ClassParameterWithSingleTypeParameter.class, String.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(String[].class));
    }

    @Test
    public void imports_single_class_bound_with_single_type_parameter_assigned_to_primitive_array_type_argument() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType<T extends ClassParameterWithSingleTypeParameter<int[]>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType.class,
                ClassParameterWithSingleTypeParameter.class, String.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterWithGenericClassBoundAssignedToArrayType.class);

        assertThatType(javaClass).hasOnlyTypeParameter("T")
                .withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(int[].class));
    }

    @Test
    public void imports_multiple_class_bounds_with_single_type_parameters_assigned_to_concrete_types() {
        @SuppressWarnings("unused")
        class ClassWithMultipleTypeParametersWithGenericClassOrInterfaceBoundsAssignedToConcreteTypes<
                A extends ClassParameterWithSingleTypeParameter<File>,
                B extends InterfaceParameterWithSingleTypeParameter<Serializable>,
                C extends InterfaceParameterWithSingleTypeParameter<String>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithMultipleTypeParametersWithGenericClassOrInterfaceBoundsAssignedToConcreteTypes.class,
                ClassParameterWithSingleTypeParameter.class, File.class, InterfaceParameterWithSingleTypeParameter.class, Serializable.class, String.class);

        JavaClass javaClass = classes.get(ClassWithMultipleTypeParametersWithGenericClassOrInterfaceBoundsAssignedToConcreteTypes.class);

        assertThatType(javaClass).hasTypeParameters("A", "B", "C")
                .hasTypeParameter("A").withBoundsMatching(parameterizedType(ClassParameterWithSingleTypeParameter.class).withTypeArguments(File.class))
                .hasTypeParameter("B").withBoundsMatching(parameterizedType(InterfaceParameterWithSingleTypeParameter.class).withTypeArguments(Serializable.class))
                .hasTypeParameter("C").withBoundsMatching(parameterizedType(InterfaceParameterWithSingleTypeParameter.class).withTypeArguments(String.class));
    }

    @Test
    public void imports_multiple_class_bounds_with_multiple_type_parameters_assigned_to_concrete_types() {
        @SuppressWarnings("unused")
        class ClassWithTwoTypeParametersWithMultipleGenericClassAndInterfaceBoundsAssignedToConcreteTypes<
                A extends ClassParameterWithSingleTypeParameter<String> & InterfaceParameterWithSingleTypeParameter<Serializable>,
                B extends Map<String, Serializable> & Iterable<File> & Function<Integer, Long>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithTwoTypeParametersWithMultipleGenericClassAndInterfaceBoundsAssignedToConcreteTypes.class,
                ClassParameterWithSingleTypeParameter.class, InterfaceParameterWithSingleTypeParameter.class,
                Map.class, Iterable.class, Function.class, String.class, Serializable.class, File.class, Integer.class, Long.class);

        JavaClass javaClass = classes.get(ClassWithTwoTypeParametersWithMultipleGenericClassAndInterfaceBoundsAssignedToConcreteTypes.class);

        assertThatType(javaClass).hasTypeParameters("A", "B")
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

    @Test
    public void imports_single_type_bound_with_unbound_wildcard() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterBoundByTypeWithUnboundWildcard<T extends List<?>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithSingleTypeParameterBoundByTypeWithUnboundWildcard.class, List.class, String.class);

        JavaClass javaClass = classes.get(ClassWithSingleTypeParameterBoundByTypeWithUnboundWildcard.class);

        assertThatType(javaClass)
                .hasTypeParameter("T").withBoundsMatching(parameterizedType(List.class).withWildcardTypeParameter());
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_upper_bound_wildcard() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterBoundByTypeWithWildcardWithUpperClassBound<T extends List<? extends String>> {
        }
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterBoundByTypeWithWildcardWithUpperInterfaceBound<T extends List<? extends Serializable>> {
        }

        return $$(
                $(ClassWithSingleTypeParameterBoundByTypeWithWildcardWithUpperClassBound.class, String.class),
                $(ClassWithSingleTypeParameterBoundByTypeWithWildcardWithUpperInterfaceBound.class, Serializable.class)
        );
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_upper_bound_wildcard(Class<?> classWithWildcard, Class<?> expectedUpperBound) {
        JavaClasses classes = new ClassFileImporter().importClasses(classWithWildcard, List.class, expectedUpperBound);

        JavaClass javaClass = classes.get(classWithWildcard);

        assertThatType(javaClass)
                .hasTypeParameter("T").withBoundsMatching(parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(expectedUpperBound));
    }

    @DataProvider
    public static Object[][] data_imports_single_type_bound_with_lower_bound_wildcard() {
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterBoundByTypeWithWildcardWithLowerClassBound<T extends List<? super String>> {
        }
        @SuppressWarnings("unused")
        class ClassWithSingleTypeParameterBoundByTypeWithWildcardWithLowerInterfaceBound<T extends List<? super Serializable>> {
        }

        return $$(
                $(ClassWithSingleTypeParameterBoundByTypeWithWildcardWithLowerClassBound.class, String.class),
                $(ClassWithSingleTypeParameterBoundByTypeWithWildcardWithLowerInterfaceBound.class, Serializable.class)
        );
    }

    @Test
    @UseDataProvider
    public void test_imports_single_type_bound_with_lower_bound_wildcard(Class<?> classWithWildcard, Class<?> expectedLowerBound) {
        JavaClasses classes = new ClassFileImporter().importClasses(classWithWildcard, List.class, expectedLowerBound);

        JavaClass javaClass = classes.get(classWithWildcard);

        assertThatType(javaClass)
                .hasTypeParameter("T").withBoundsMatching(parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(expectedLowerBound));
    }

    @Test
    public void imports_multiple_type_bounds_with_multiple_wildcards_with_various_bounds() {
        @SuppressWarnings("unused")
        class ClassWithMultipleTypeParametersBoundByTypesWithDifferentBounds<A extends Map<? extends Serializable, ? super File>, B extends Reference<? super String> & Map<?, ?>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithMultipleTypeParametersBoundByTypesWithDifferentBounds.class,
                Map.class, Serializable.class, File.class, Reference.class, String.class);

        JavaClass javaClass = classes.get(ClassWithMultipleTypeParametersBoundByTypesWithDifferentBounds.class);

        assertThatType(javaClass).hasTypeParameters("A", "B")
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

    @Test
    public void references_type_variable_bound() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameterWithTypeVariableBound<U extends T, T extends String, V extends T> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithTypeParameterWithTypeVariableBound.class, String.class);

        JavaClass javaClass = classes.get(ClassWithTypeParameterWithTypeVariableBound.class);

        assertThatType(javaClass).hasTypeParameters("U", "T", "V")
                .hasTypeParameter("U").withBoundsMatching(typeVariable("T").withUpperBounds(String.class))
                .hasTypeParameter("V").withBoundsMatching(typeVariable("T").withUpperBounds(String.class));
    }

    @Test
    public void references_type_variable_bound_for_inner_classes() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameterWithInnerClassesWithTypeVariableBound<U extends T, T extends String> {
            @SuppressWarnings("InnerClassMayBeStatic")
            class SomeInner {
                class EvenMoreInnerDeclaringOwn<V extends U, MORE_INNER1, MORE_INNER2 extends U> {
                    class AndEvenMoreInner<MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> {
                    }
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.class,
                ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.class,
                ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.EvenMoreInnerDeclaringOwn.class,
                ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.EvenMoreInnerDeclaringOwn.AndEvenMoreInner.class,
                String.class);

        JavaClass javaClass = classes.get(ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.EvenMoreInnerDeclaringOwn.AndEvenMoreInner.class);

        assertThatType(javaClass).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(
                        typeVariable("T").withUpperBounds(String.class))
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(
                        typeVariable("MORE_INNER2").withUpperBounds(
                                typeVariable("U").withUpperBounds(
                                        typeVariable("T").withUpperBounds(String.class))));
    }

    @Test
    public void imports_inner_class_as_type_variable_bound() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameterBoundByInnerClass<T extends ClassWithTypeParameterBoundByInnerClass<T, U>.SomeInner<String>, U extends ClassWithTypeParameterBoundByInnerClass<T, U>.SomeInner<String>.EvenMoreInner> {
            @SuppressWarnings("InnerClassMayBeStatic")
            class SomeInner<Z> {
                class EvenMoreInner {
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithTypeParameterBoundByInnerClass.class,
                ClassWithTypeParameterBoundByInnerClass.SomeInner.class,
                ClassWithTypeParameterBoundByInnerClass.SomeInner.EvenMoreInner.class,
                String.class);

        JavaClass javaClass = classes.get(ClassWithTypeParameterBoundByInnerClass.class);

        assertThatType(javaClass).hasTypeParameters("T", "U")
                .hasTypeParameter("T")
                .withBoundsMatching(
                        parameterizedType(ClassWithTypeParameterBoundByInnerClass.SomeInner.class).withTypeArguments(String.class))
                .hasTypeParameter("U")
                .withBoundsMatching(ClassWithTypeParameterBoundByInnerClass.SomeInner.EvenMoreInner.class);
    }

    @Test
    public void creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameterWithInnerClassesWithTypeVariableBound<U extends T, T extends String> {
            @SuppressWarnings("InnerClassMayBeStatic")
            class SomeInner {
                class EvenMoreInnerDeclaringOwn<V extends U, MORE_INNER1, MORE_INNER2 extends U> {
                    class AndEvenMoreInner<MOST_INNER1 extends T, MOST_INNER2 extends MORE_INNER2> {
                    }
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.EvenMoreInnerDeclaringOwn.AndEvenMoreInner.class);

        JavaClass javaClass = classes.get(ClassWithTypeParameterWithInnerClassesWithTypeVariableBound.SomeInner.EvenMoreInnerDeclaringOwn.AndEvenMoreInner.class);

        assertThatType(javaClass).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(typeVariable("T").withoutUpperBounds())
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(typeVariable("MORE_INNER2").withoutUpperBounds());
    }

    @Test
    public void considers_hierarchy_of_methods_and_classes_for_type_parameter_context() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class Level1<T1 extends String> {
            <T2 extends T1> void level2() {
                class Level3<T3 extends T2> {
                    <T4 extends T3> void level4() {
                        class Level5<T51 extends T4, T52 extends T1> {
                        }
                    }
                }
            }
        }

        Class<?> innermostClass = Class.forName(Level1.class.getName() + "$1Level3$1Level5");
        JavaClasses classes = new ClassFileImporter().importClasses(
                Class.forName(Level1.class.getName() + "$1Level3"),
                innermostClass,
                Level1.class, String.class);

        JavaClass javaClass = classes.get(innermostClass);

        assertThatType(javaClass).hasTypeParameters("T51", "T52")
                .hasTypeParameter("T51")
                .withBoundsMatching(
                        typeVariable("T4").withUpperBounds(
                                typeVariable("T3").withUpperBounds(
                                        typeVariable("T2").withUpperBounds(
                                                typeVariable("T1").withUpperBounds(String.class)))))
                .hasTypeParameter("T52")
                .withBoundsMatching(typeVariable("T1").withUpperBounds(String.class));
    }

    @Test
    public void imports_wild_cards_bound_by_type_variables() {
        @SuppressWarnings("unused")
        class ClassWithWildcardWithTypeVariableBounds<T extends String, U extends List<? extends T>, V extends List<? super T>> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithWildcardWithTypeVariableBounds.class, List.class, String.class);

        JavaClass javaClass = classes.get(ClassWithWildcardWithTypeVariableBounds.class);

        assertThatType(javaClass).hasTypeParameters("T", "U", "V")
                .hasTypeParameter("U")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                typeVariable("T").withUpperBounds(String.class)))
                .hasTypeParameter("V")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                typeVariable("T").withUpperBounds(String.class)));
    }

    @Test
    public void imports_wild_cards_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class ClassWithWildcardWithTypeVariableBounds<T extends String, U extends List<? extends T>, V extends List<? super T>> {
            class Inner<MORE_INNER extends List<? extends U>> {
                class MoreInner<MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> {
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassWithWildcardWithTypeVariableBounds.class,
                ClassWithWildcardWithTypeVariableBounds.Inner.class,
                ClassWithWildcardWithTypeVariableBounds.Inner.MoreInner.class,
                List.class, String.class);

        JavaClass javaClass = classes.get(ClassWithWildcardWithTypeVariableBounds.Inner.MoreInner.class);

        assertThatType(javaClass).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
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

    @Test
    public void creates_new_stub_type_variables_for_wildcards_bound_by_type_variables_of_enclosing_classes_that_are_out_of_context() {
        @SuppressWarnings("unused")
        class ClassWithWildcardWithTypeVariableBounds<T extends String, U extends List<? extends T>, V extends List<? super T>> {
            class Inner<MORE_INNER extends List<? extends U>> {
                class MoreInner<MOST_INNER1 extends List<? extends T>, MOST_INNER2 extends List<? super V>> {
                }
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithWildcardWithTypeVariableBounds.Inner.MoreInner.class,
                List.class, String.class);

        JavaClass javaClass = classes.get(ClassWithWildcardWithTypeVariableBounds.Inner.MoreInner.class);

        assertThatType(javaClass).hasTypeParameters("MOST_INNER1", "MOST_INNER2")
                .hasTypeParameter("MOST_INNER1")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(
                                typeVariable("T").withoutUpperBounds()))
                .hasTypeParameter("MOST_INNER2")
                .withBoundsMatching(
                        parameterizedType(List.class).withWildcardTypeParameterWithLowerBound(
                                typeVariable("V").withoutUpperBounds()));
    }

    @Test
    public void imports_complex_type_with_multiple_nested_parameters_with_various_bounds_and_recursive_type_definitions() {
        @SuppressWarnings({"unused", "rawtypes"})
        class ClassWithComplexTypeParameters<
                A extends List<?> & Serializable & Comparable<A>,
                B extends A,
                C extends Map<
                        Map.Entry<A, Map.Entry<String, B>>,
                        Map<? extends String,
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<B, ?>>>>>>>>,
                SELF extends ClassWithComplexTypeParameters<A, B, C, SELF, D, RAW>,
                D,
                RAW extends List> {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithComplexTypeParameters.class,
                List.class, Serializable.class, Comparable.class, Map.class, Map.Entry.class, String.class, Set.class, Iterable.class, Object.class);

        JavaClass javaClass = classes.get(ClassWithComplexTypeParameters.class);

        assertThatType(javaClass)
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
                .withBoundsMatching(
                        parameterizedType(ClassWithComplexTypeParameters.class).withTypeArguments(
                                typeVariable("A"),
                                typeVariable("B"),
                                typeVariable("C"),
                                typeVariable("SELF"),
                                typeVariable("D"),
                                typeVariable("RAW")
                        ))
                .hasTypeParameter("D").withBoundsMatching(Object.class)
                .hasTypeParameter("RAW").withBoundsMatching(List.class);
    }

    @Test
    public void imports_complex_type_with_multiple_nested_parameters_with_concrete_array_bounds() {
        @SuppressWarnings("unused")
        class ClassWithComplexTypeParametersWithConcreteArrayBounds<
                A extends List<Serializable[]>,
                B extends List<? extends Serializable[][]>,
                C extends Map<? super String[], Map<Map<? super String[][][], ?>, int[][]>>
                > {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithComplexTypeParametersWithConcreteArrayBounds.class,
                List.class, Serializable.class, Map.class, String.class);

        JavaClass javaClass = classes.get(ClassWithComplexTypeParametersWithConcreteArrayBounds.class);

        assertThatType(javaClass)
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

    @Test
    public void imports_type_with_parameterized_array_bounds() {
        @SuppressWarnings("unused")
        class ClassWithThreeTypeParameters<A, B, C> {
        }
        @SuppressWarnings("unused")
        class ClassWithTypeParameterWithParameterizedArrayBounds<
                T extends ClassWithThreeTypeParameters<List<String>[], List<String[]>[][], List<String[][]>[][][]>
                > {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithTypeParameterWithParameterizedArrayBounds.class,
                ClassWithThreeTypeParameters.class, List.class, String.class);

        JavaClass javaClass = classes.get(ClassWithTypeParameterWithParameterizedArrayBounds.class);

        assertThatType(javaClass)
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

    @Test
    public void imports_complex_type_with_multiple_nested_parameters_with_generic_array_bounds() {
        @SuppressWarnings("unused")
        class ClassWithComplexTypeParametersWithGenericArrayBounds<
                X extends Serializable,
                Y extends String,
                A extends List<X[]>,
                B extends List<? extends X[][]>,
                C extends Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>
                > {
        }

        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithComplexTypeParametersWithGenericArrayBounds.class,
                List.class, Serializable.class, Map.class, String.class);

        JavaClass javaClass = classes.get(ClassWithComplexTypeParametersWithGenericArrayBounds.class);

        assertThatType(javaClass)
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

    @SuppressWarnings("unused")
    public static class ClassParameterWithSingleTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    public interface InterfaceParameterWithSingleTypeParameter<T> {
    }
}
