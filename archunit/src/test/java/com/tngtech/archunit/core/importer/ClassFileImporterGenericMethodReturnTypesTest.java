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
public class ClassFileImporterGenericMethodReturnTypesTest {

    @Test
    public void imports_non_generic_method_return_type() {
        class NonGenericReturnType {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            NonGenericReturnType method() {
                return null;
            }
        }

        JavaType returnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(returnType).as("return type").matches(NonGenericReturnType.class);
    }

    @Test
    public void imports_non_generic_method_return_type_on_generic_signature() {
        @SuppressWarnings("unused")
        class SomeClass {
            <T> Object method(T irrelevant) {
                return null;
            }

            <T> int primitive(T irrelevant) {
                return 0;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(SomeClass.class);

        JavaType returnType = javaClass.getMethod("method", Object.class).getReturnType();
        assertThatType(returnType).as("return type").matches(Object.class);

        returnType = javaClass.getMethod("primitive", Object.class).getReturnType();
        assertThatType(returnType).as("return type").matches(int.class);
    }

    @Test
    public void imports_generic_method_return_type_with_one_type_argument() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<String> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type")
                .hasErasure(GenericReturnType.class)
                .hasActualTypeArguments(String.class);
    }

    @Test
    public void imports_raw_generic_method_return_type_as_JavaClass_instead_of_JavaParameterizedType() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings({"unused", "rawtypes"})
        class SomeClass {
            GenericReturnType method() {
                return null;
            }
        }

        JavaType rawGenericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(rawGenericReturnType).as("raw generic method return type").matches(GenericReturnType.class);
    }

    @Test
    public void imports_generic_method_return_type_with_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<String[]> method() {
                return null;
            }
        }

        JavaType genericMethodReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericMethodReturnType).as("generic method return type")
                .hasErasure(GenericReturnType.class)
                .hasActualTypeArguments(String[].class);
    }

    @Test
    public void imports_generic_method_return_type_with_primitive_array_type_argument() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<int[]> method() {
                return null;
            }
        }

        JavaType genericMethodReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericMethodReturnType).as("generic method return type")
                .hasErasure(GenericReturnType.class)
                .hasActualTypeArguments(int[].class);
    }

    @Test
    public void imports_generic_method_return_type_with_multiple_type_arguments() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<String, Serializable, File> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type")
                .hasErasure(GenericReturnType.class)
                .hasActualTypeArguments(String.class, Serializable.class, File.class);
    }

    @Test
    public void imports_generic_method_return_type_with_single_actual_type_argument_parameterized_with_concrete_class() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<ClassParameterWithSingleTypeParameter<String>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_method_return_type_with_multiple_actual_type_arguments_parameterized_with_concrete_classes() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<
                    ClassParameterWithSingleTypeParameter<File>,
                    InterfaceParameterWithSingleTypeParameter<Serializable>,
                    InterfaceParameterWithSingleTypeParameter<String>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(File.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(Serializable.class),
                parameterizedType(InterfaceParameterWithSingleTypeParameter.class)
                        .withTypeArguments(String.class)
        );
    }

    @Test
    public void imports_generic_method_return_type_with_single_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<?> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(wildcardType());
    }

    @Test
    public void imports_generic_method_return_type_with_single_actual_type_argument_parameterized_with_unbound_wildcard() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<ClassParameterWithSingleTypeParameter<?>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameter()
        );
    }

    @Test
    public void imports_generic_method_return_type_with_actual_type_arguments_parameterized_with_bounded_wildcards() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<
                    ClassParameterWithSingleTypeParameter<? extends String>,
                    ClassParameterWithSingleTypeParameter<? super File>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(String.class),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(File.class)
        );
    }

    @Test
    public void imports_generic_method_return_type_with_actual_type_arguments_with_multiple_wildcards_with_various_bounds() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<
                    ClassParameterWithSingleTypeParameter<Map<? extends Serializable, ? super File>>,
                    ClassParameterWithSingleTypeParameter<Reference<? super String>>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
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
    public void imports_type_variable_as_generic_method_return_type() {
        @SuppressWarnings("unused")
        class SomeClass<T extends String> {
            T method() {
                return null;
            }
        }

        JavaType genericMethodReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericMethodReturnType).as("generic method return type")
                .isInstanceOf(JavaTypeVariable.class)
                .hasErasure(String.class);
    }

    @Test
    public void imports_generic_method_return_type_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS> {
            GenericReturnType<OF_CLASS> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(typeVariable("OF_CLASS"));
    }

    @Test
    public void imports_generic_method_return_type_with_actual_type_argument_parameterized_with_type_variable() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS> {
            GenericReturnType<ClassParameterWithSingleTypeParameter<OF_CLASS>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS"))
        );
    }

    @Test
    public void references_type_variable_assigned_to_actual_type_argument_of_generic_method_return_type() {
        @SuppressWarnings("unused")
        class GenericReturnType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass<OF_CLASS extends String> {
            GenericReturnType<ClassParameterWithSingleTypeParameter<OF_CLASS>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withTypeArguments(typeVariable("OF_CLASS").withUpperBounds(String.class))
        );
    }

    @Test
    public void references_outer_type_variable_assigned_to_actual_type_argument_of_generic_method_return_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                @SuppressWarnings("unused")
                class GenericReturnType<T> {
                }

                class SomeClass {
                    GenericReturnType<OUTER> method() {
                        return null;
                    }
                }
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(OuterWithTypeParameter.SomeInner.SomeClass.class)
                .getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                typeVariable("OUTER").withUpperBounds(String.class)
        );
    }

    @Test
    public void creates_new_stub_type_variables_for_type_variables_of_enclosing_classes_that_are_out_of_context_for_generic_method_return_type_of_inner_class() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER extends String> {
            class SomeInner {
                class GenericReturnType<T> {
                }

                class SomeClass {
                    GenericReturnType<OUTER> method() {
                        return null;
                    }
                }
            }
        }

        JavaType genericReturnType = resetConfigurationAround(new Callable<JavaType>() {
            @Override
            public JavaType call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                return new ClassFileImporter().importClass(OuterWithTypeParameter.SomeInner.SomeClass.class)
                        .getMethod("method").getReturnType();
            }
        });

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                typeVariable("OUTER").withoutUpperBounds()
        );
    }

    @Test
    public void considers_hierarchy_of_methods_and_classes_for_type_parameter_context() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class Level1<T1 extends String> {
            <T2 extends T1> void level2() {
                class Level3<T3 extends T2> {
                    <T4 extends T3> T4 method() {
                        return null;
                    }
                }
            }
        }

        Class<?> innermostClass = Class.forName(Level1.class.getName() + "$1Level3");
        JavaType genericReturnType = new ClassFileImporter()
                .importClass(innermostClass).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type")
                .matches(
                        typeVariable("T4").withUpperBounds(
                                typeVariable("T3").withUpperBounds(
                                        typeVariable("T2").withUpperBounds(
                                                typeVariable("T1").withUpperBounds(String.class)))));
    }

    @Test
    public void imports_wildcards_of_generic_method_return_type_bound_by_type_variables() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST extends String, SECOND extends Serializable> {
            GenericReturnType<
                    ClassParameterWithSingleTypeParameter<? extends FIRST>,
                    ClassParameterWithSingleTypeParameter<? super SECOND>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter()
                .importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("FIRST").withUpperBounds(String.class)),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("SECOND").withUpperBounds(Serializable.class))
        );
    }

    @Test
    public void imports_wildcards_of_generic_method_return_type_bound_by_type_variables_of_enclosing_classes() {
        @SuppressWarnings("unused")
        class OuterWithTypeParameter<OUTER_ONE extends String, OUTER_TWO extends Serializable> {
            class SomeInner {
                class GenericReturnType<A, B> {
                }

                class SomeClass {
                    GenericReturnType<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> method() {
                        return null;
                    }
                }
            }
        }

        JavaType genericReturnType = new ClassFileImporter()
                .importClass(OuterWithTypeParameter.SomeInner.SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
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
                class GenericReturnType<A, B> {
                }

                class SomeClass {
                    GenericReturnType<
                            ClassParameterWithSingleTypeParameter<? extends OUTER_ONE>,
                            ClassParameterWithSingleTypeParameter<? super OUTER_TWO>> method() {
                        return null;
                    }
                }
            }
        }

        JavaType genericReturnType = resetConfigurationAround(new Callable<JavaType>() {
            @Override
            public JavaType call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                return new ClassFileImporter().importClasses(OuterWithTypeParameter.SomeInner.SomeClass.class, ClassParameterWithSingleTypeParameter.class)
                        .get(OuterWithTypeParameter.SomeInner.SomeClass.class)
                        .getMethod("method").getReturnType();
            }
        });

        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithUpperBound(
                                typeVariable("OUTER_ONE").withoutUpperBounds()),
                parameterizedType(ClassParameterWithSingleTypeParameter.class)
                        .withWildcardTypeParameterWithLowerBound(
                                typeVariable("OUTER_TWO").withoutUpperBounds())
        );
    }

    @Test
    public void imports_complex_generic_method_return_type_with_multiple_nested_actual_type_arguments_with_self_referencing_type_definitions() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST extends String & Serializable, SECOND extends Serializable & Cloneable> {
            GenericReturnType<
                    // assigned to GenericReturnType<A,_,_>
                    List<? extends FIRST>,
                    // assigned to GenericReturnType<_,B,_>
                    Map<
                            Map.Entry<FIRST, Map.Entry<String, SECOND>>,
                            Map<? extends String,
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<SECOND, ?>>>>>>>>,
                    // assigned to GenericReturnType<_,_,C>
                    Comparable<SomeClass<FIRST, SECOND>>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        // @formatter:off
        assertThatType(genericReturnType).as("generic return type").hasActualTypeArguments(
            // assigned to GenericReturnType<A,_,_>
            parameterizedType(List.class)
                .withWildcardTypeParameterWithUpperBound(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class)),
            // assigned to GenericReturnType<_,B,_>
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
            // assigned to GenericReturnType<_,_,C>
            parameterizedType(Comparable.class).withTypeArguments(
                parameterizedType(SomeClass.class).withTypeArguments(
                    typeVariable("FIRST").withUpperBounds(String.class, Serializable.class),
                    typeVariable("SECOND").withUpperBounds(Serializable.class, Cloneable.class))));
        // @formatter:on
    }

    @Test
    public void imports_complex_generic_array_method_return_type() {
        @SuppressWarnings("unused")
        class GenericReturnType<A> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<Map<? super String, Map<Map<? super String, ?>, Serializable>>>[] method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).matches(
                genericArray(
                        GenericReturnType.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", "
                                + Map.class.getName() + "<" + Map.class.getName() + "<? super " + String.class.getName() + ", ?>, "
                                + Serializable.class.getName() + ">>>[]"
                ).withComponentType(
                        parameterizedType(GenericReturnType.class).withTypeArguments(
                                parameterizedType(Map.class).withTypeArguments(
                                        wildcardType().withLowerBound(String.class),
                                        parameterizedType(Map.class).withTypeArguments(
                                                parameterizedType(Map.class).withTypeArguments(
                                                        wildcardType().withLowerBound(String.class),
                                                        wildcardType()),
                                                concreteClass(Serializable.class))))));
    }

    @Test
    public void imports_complex_generic_method_return_type_with_multiple_nested_actual_type_arguments_with_concrete_array_bounds() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<
                    List<Serializable[]>,
                    List<? extends Serializable[][]>,
                    Map<? super String[], Map<Map<? super String[][][], ?>, Serializable[][]>>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).hasActualTypeArguments(
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
    public void imports_generic_method_return_type_with_parameterized_array_bounds() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C> {
        }

        @SuppressWarnings("unused")
        class SomeClass {
            GenericReturnType<List<String>[], List<String[]>[][], List<String[][]>[][][]> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).hasActualTypeArguments(
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
        abstract class SomeClass<T extends String> {
            abstract T[] method();

            abstract T[][] method2Dim();
        }

        JavaClass javaClass = new ClassFileImporter().importClass(SomeClass.class);

        assertThatType(javaClass.getMethod("method").getReturnType())
                .hasErasure(String[].class)
                .matches(genericArray(typeVariableArrayName("T", 1))
                        .withComponentType(typeVariable("T").withUpperBounds(String.class)));
        assertThatType(javaClass.getMethod("method2Dim").getReturnType())
                .hasErasure(String[][].class)
                .matches(genericArray(typeVariableArrayName("T", 2))
                        .withComponentType(genericArray(typeVariableArrayName("T", 1))
                                .withComponentType(typeVariable("T")
                                        .withUpperBounds(String.class))));
    }

    @Test
    public void imports_complex_generic_method_return_type_with_multiple_nested_actual_type_arguments_with_generic_array_bounds() {
        @SuppressWarnings("unused")
        class GenericReturnType<A, B, C, D> {
        }
        @SuppressWarnings("unused")
        class SomeClass<X extends Serializable, Y extends String> {
            GenericReturnType<
                    X[],
                    List<X[]>,
                    List<? extends X[][]>,
                    Map<? super Y[], Map<Map<? super Y[][][], ?>, X[][]>>> method() {
                return null;
            }
        }

        JavaType genericReturnType = new ClassFileImporter().importClass(SomeClass.class).getMethod("method").getReturnType();

        assertThatType(genericReturnType).hasActualTypeArguments(
                genericArray("X[]").withComponentType(
                        typeVariable("X").withUpperBounds(Serializable.class)),
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
