package com.tngtech.archunit.core.importer;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;

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
import static com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.ExpectedConcreteWildcardType.wildcardType;
import static com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.parameterizedType;
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
    public static Object[][] single_type_bound_with_upper_bound_wildcard() {
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
    @UseDataProvider("single_type_bound_with_upper_bound_wildcard")
    public void imports_single_type_bound_with_upper_bound_wildcard(Class<?> classWithWildcard, Class<?> expectedUpperBound) {
        JavaClasses classes = new ClassFileImporter().importClasses(classWithWildcard, List.class, expectedUpperBound);

        JavaClass javaClass = classes.get(classWithWildcard);

        assertThatType(javaClass)
                .hasTypeParameter("T").withBoundsMatching(parameterizedType(List.class).withWildcardTypeParameterWithUpperBound(expectedUpperBound));
    }

    @DataProvider
    public static Object[][] single_type_bound_with_lower_bound_wildcard() {
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
    @UseDataProvider("single_type_bound_with_lower_bound_wildcard")
    public void imports_single_type_bound_with_lower_bound_wildcard(Class<?> classWithWildcard, Class<?> expectedLowerBound) {
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

    @SuppressWarnings("unused")
    public static class ClassParameterWithSingleTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    public interface InterfaceParameterWithSingleTypeParameter<T> {
    }
}
