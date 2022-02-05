package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.importer.DependencyResolutionProcessTestUtils.importClassWithOnlyGenericTypeResolution;
import static com.tngtech.archunit.core.importer.DependencyResolutionProcessTestUtils.importClassesWithOnlyGenericTypeResolution;
import static com.tngtech.archunit.testutil.ArchConfigurationRule.resetConfigurationAround;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.genericArray;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.parameterizedTypeArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteGenericArray.typeVariableArrayName;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteWildcardType.wildcardType;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterGenericFieldTypesTest {

    @Test
    public void imports_non_generic_field_type() {
        class NonGenericFieldType {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            NonGenericFieldType field;
        }

        JavaType fieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(fieldType).as("generic field type").matches(NonGenericFieldType.class);
    }

    @Test
    public void imports_generic_field_type_with_one_type_argument() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<String> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .hasErasure(GenericFieldType.class)
                .hasActualTypeArguments(String.class);
    }

    @Test
    public void imports_raw_generic_field_type_as_JavaClass_instead_of_JavaParameterizedType() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings({"unused", "rawtypes"})
        class SomeClass {
            GenericFieldType field;
        }

        JavaType rawGenericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(rawGenericFieldType).as("raw generic field type").matches(GenericFieldType.class);
    }

    @Test
    public void imports_generic_field_type_with_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<String[]> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .hasErasure(GenericFieldType.class)
                .hasActualTypeArguments(String[].class);
    }

    @Test
    public void imports_generic_field_type_with_primitive_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<int[]> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .hasErasure(GenericFieldType.class)
                .hasActualTypeArguments(int[].class);
    }

    @Test
    public void imports_generic_field_type_with_multiple_type_arguments() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<String, Serializable, File> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .hasErasure(GenericFieldType.class)
                .hasActualTypeArguments(String.class, Serializable.class, File.class);
    }

    @Test
    public void imports_generic_field_type_with_single_actual_type_argument_parameterized_with_concrete_class() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<ClassParameterWithSingleTypeParameter<String>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_field_type_with_multiple_actual_type_arguments_parameterized_with_concrete_classes() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<
                    ClassParameterWithSingleTypeParameter<File>,
                    InterfaceParameterWithSingleTypeParameter<Serializable>,
                    InterfaceParameterWithSingleTypeParameter<String>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(File.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(Serializable.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_field_type_with_single_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<?> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(wildcardType());
    }

    @Test
    public void imports_generic_field_type_with_single_actual_type_argument_parameterized_with_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<ClassParameterWithSingleTypeParameter<?>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameter()
        );
    }

    @Test
    public void imports_generic_field_type_with_actual_type_arguments_parameterized_with_bounded_wildcards() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<
                    ClassParameterWithSingleTypeParameter<? extends String>,
                    ClassParameterWithSingleTypeParameter<? super File>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(String.class),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(File.class)
        );
    }

    @Test
    public void imports_generic_field_type_with_actual_type_arguments_with_multiple_wildcards_with_various_bounds() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<
                    ClassParameterWithSingleTypeParameter<Map<? extends Serializable, ? super File>>,
                    ClassParameterWithSingleTypeParameter<Reference<? super String>>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
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
    public void imports_type_variable_as_generic_field_type() {
        @SuppressWarnings("unused")
        class SomeClass<T extends String> {
            T field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .isInstanceOf(JavaTypeVariable.class)
                .hasErasure(String.class);
    }

    @Test
    public void imports_generic_field_type_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS> {
            GenericFieldType<OF_CLASS> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(typeVariable("OF_CLASS"));
    }

    @Test
    public void imports_generic_field_type_with_actual_type_argument_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS> {
            GenericFieldType<ClassParameterWithSingleTypeParameter<OF_CLASS>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS"))
        );
    }

    @Test
    public void references_type_variable_assigned_to_actual_type_argument_of_generic_field_type() {
        @SuppressWarnings("unused")
        class GenericFieldType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS extends String> {
            GenericFieldType<ClassParameterWithSingleTypeParameter<OF_CLASS>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS").withUpperBounds(String.class))
        );
    }

    @Test
    public void references_outer_type_variable_assigned_to_actual_type_argument_of_generic_field_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                @SuppressWarnings("unused")
                class GenericFieldType<T> {
                }

                class SomeClass {
                    GenericFieldType<OUTER> field;
                }
            }
        }

        JavaType genericFieldType = importClassesWithOnlyGenericTypeResolution(
                OuterWithTypeParameter.SomeInner.SomeClass.class,
                OuterWithTypeParameter.SomeInner.class,
                OuterWithTypeParameter.class
        ).get(OuterWithTypeParameter.SomeInner.SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                typeVariable("OUTER").withUpperBounds(String.class)
        );
    }

    @Test
    public void creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context_for_generic_field_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class GenericFieldType<T> {
                }

                class SomeClass {
                    GenericFieldType<OUTER> field;
                }
            }
        }

        JavaType genericFieldType = resetConfigurationAround(new Callable<JavaType>() {
            @Override
            public JavaType call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                return importClassWithOnlyGenericTypeResolution(OuterWithTypeParameter.SomeInner.SomeClass.class)
                        .getField("field").getType();
            }
        });

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                typeVariable("OUTER").withoutUpperBounds()
        );
    }

    @Test
    public void considers_hierarchy_of_methods_and_classes_for_type_parameter_context() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class Level1<T1 extends String> {
            <T2 extends T1> void level2() {
                class Level3<T3 extends T2> {
                    T3 field;
                }
            }
        }

        Class<?> innermostClass = Class.forName(Level1.class.getName() + "$1Level3");
        JavaType genericFieldType = importClassesWithOnlyGenericTypeResolution(
                innermostClass, Level1.class
        ).get(innermostClass).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type")
                .matches(
                        typeVariable("T3").withUpperBounds(
                                typeVariable("T2").withUpperBounds(
                                        typeVariable("T1").withUpperBounds(String.class))));
    }

    @Test
    public void imports_wildcards_of_generic_field_type_bound_by_type_variables() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST extends String, SECOND extends Serializable> {
            GenericFieldType<
                    ClassParameterWithSingleTypeParameter<? extends FIRST>,
                    ClassParameterWithSingleTypeParameter<? super SECOND>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("FIRST").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("SECOND").withUpperBounds(Serializable.class))
        );
    }

    @Test
    public void imports_wildcards_of_generic_field_type_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class GenericFieldType<A, B> {
                }

                class SomeClass {
                    GenericFieldType<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> field;
                }
            }
        }

        JavaType genericFieldType = importClassesWithOnlyGenericTypeResolution(
                OuterWithTypeParameter.SomeInner.SomeClass.class,
                OuterWithTypeParameter.SomeInner.class,
                OuterWithTypeParameter.class
        ).get(OuterWithTypeParameter.SomeInner.SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
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
                @SuppressWarnings("unused")
                class GenericFieldType<A, B> {
                }

                class SomeClass {
                    GenericFieldType<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> field;
                }
            }
        }

        JavaType genericFieldType = resetConfigurationAround(new Callable<JavaType>() {
            @Override
            public JavaType call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                return importClassesWithOnlyGenericTypeResolution(OuterWithTypeParameter.SomeInner.SomeClass.class, ClassParameterWithSingleTypeParameter.class)
                        .get(OuterWithTypeParameter.SomeInner.SomeClass.class).getField("field").getType();
            }
        });

        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withoutUpperBounds()),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withoutUpperBounds())
        );
    }

    @Test
    public void imports_complex_generic_field_type_with_multiple_nested_actual_type_arguments_with_self_referencing_type_definitions() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            GenericFieldType<
                    // assigned to GenericFieldType<A,_,_>
                    List<? extends FIRST>,
                    // assigned to GenericFieldType<_,B,_>
                    Map<
                            Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                    // assigned to GenericFieldType<_,_,C>
                    Comparable<SomeClass<FIRST, SECOND>>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        // @formatter:off
        assertThatType(genericFieldType).as("generic field type").hasActualTypeArguments(
            // assigned to GenericFieldType<A,_,_>
            parameterizedType(List.class)
                .withWildcardTypeParameterWithUpperBound(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class)),
            // assigned to GenericFieldType<_,B,_>
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
            // assigned to GenericFieldType<_,_,C>
            parameterizedType(Comparable.class).withTypeArguments(
                parameterizedType(SomeClass.class).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))));
        // @formatter:on
    }

    @Test
    public void imports_complex_generic_array_field_type() {
        @SuppressWarnings("unused")
        class GenericFieldType<A> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<Map<? super String, Map<Map<? super String, ?>, Serializable>>>[] field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).matches(
                genericArray(
                        GenericFieldType.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", "
                                + Map.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", ?>, "
                                + Serializable.class.getName() + ">>>[]"
                ).withComponentType(
                        parameterizedType(GenericFieldType.class).withTypeArguments(
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withLowerBound(String.class),
                                        parameterizedType(Map.class).withTypeArguments(
                                                parameterizedType(Map.class).withTypeArguments(
                                                        wildcardType().withLowerBound(String.class),
                                                        wildcardType()),
                                                concreteClass(Serializable.class))))));
    }

    @Test
    public void imports_complex_generic_field_type_with_multiple_nested_actual_type_arguments_with_concrete_array_bounds() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<
                    List<Serializable[]>,
                    List<? extends Serializable[][]>,
                    Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).hasActualTypeArguments(
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

    @Test
    public void imports_generic_field_type_with_parameterized_array_bounds() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }

        @SuppressWarnings("unused")
        class SomeClass {
            GenericFieldType<List<String>[], List<String[]>[][], List<String[][]>[][][]> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).hasActualTypeArguments(
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

    @Test
    public void imports_generic_field_type_with_type_parameter_as_array_component_type() {
        @SuppressWarnings("unused")
        class SomeClass<T extends String> {
            T[] field;
            T[][] field2Dim;
        }

        JavaClass javaClass = importClassWithOnlyGenericTypeResolution(SomeClass.class);

        assertThatType(javaClass.getField("field").getType()).as("generic field type")
                .hasErasure(String[].class)
                .matches(genericArray(typeVariableArrayName("T", 1))
                        .withComponentType(typeVariable("T").withUpperBounds(String.class)));
        assertThatType(javaClass.getField("field2Dim").getType()).as("generic field type")
                .hasErasure(String[][].class)
                .matches(genericArray(typeVariableArrayName("T", 2))
                        .withComponentType(genericArray(typeVariableArrayName("T", 1))
                                .withComponentType(typeVariable("T")
                                        .withUpperBounds(String.class))));
    }

    @Test
    public void imports_complex_generic_field_type_with_multiple_nested_actual_type_arguments_with_generic_array_bounds() {
        @SuppressWarnings("unused")
        class GenericFieldType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass<X extends Serializable, Y extends String> {
            GenericFieldType<
                    List<X[]>,
                    List<? extends X[][]>,
                    Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> field;
        }

        JavaType genericFieldType = importClassWithOnlyGenericTypeResolution(SomeClass.class).getField("field").getType();

        assertThatType(genericFieldType).hasActualTypeArguments(
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

    @SuppressWarnings("unused")
    public static class ClassParameterWithSingleTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    public interface InterfaceParameterWithSingleTypeParameter<T> {
    }
}
