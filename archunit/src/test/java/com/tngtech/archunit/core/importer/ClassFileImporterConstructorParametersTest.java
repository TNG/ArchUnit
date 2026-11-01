package com.tngtech.archunit.core.importer;

import java.lang.reflect.Constructor;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.AnonymousClassExamples;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.ClassRetentionParameterAnnotation;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.ClassWithCapturingAnonymousClassInInstanceMethod;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.ClassWithCapturingLambdaInInstanceMethod;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.ClassWithConstructorParameter;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.LocalClassExamples;
import com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.SomeParameterAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.importer.ConstructorParametersAssertion.assertThatParametersOf;
import static com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.LocalClassExamples.LOCAL_CLASS_IN_STATIC_INITIALIZER;
import static com.tngtech.archunit.core.importer.testexamples.compileraddedparameters.LocalClassExamples.LOCAL_CLASS_IN_STATIC_INITIALIZER_CAPTURING_VARIABLE;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.TestUtils.currentJavaMajorVersion;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Documents how {@link JavaCodeUnit#getRawParameterTypes()}, {@link JavaCodeUnit#getParameterTypes()} and
 * {@link JavaCodeUnit#getParameters()} behave for constructors whose class file contains parameters that do not
 * appear in the source code, because the compiler added them: the enclosing instance of inner classes,
 * the name and ordinal of enum constants and local variables captured by local and anonymous classes (compare #1772).
 * <p>
 * The class file lists all parameters in the method descriptor, but only the declared ones in the generic
 * {@code Signature} attribute. {@code javac} writes a {@code Signature} attribute if the constructor has
 * a generic parameter type, or if it has compiler-added parameters other than the enclosing instance
 * (i.e. enum name and ordinal or captured variables). For the implicit constructor of an anonymous class it writes
 * at most an empty one that lists no parameters at all. {@code getRawParameterTypes()} is derived from the descriptor,
 * while {@code getParameterTypes()} and {@code getParameters()} are derived from the {@code Signature} attribute if present
 * and from the descriptor otherwise (also if the {@code Signature} attribute is present, but lists no parameters at all).
 * Thus, the latter two sometimes contain the compiler-added parameters and sometimes not,
 * and parameter annotations (which only count declared parameters) are shifted if they do (compare #1088).
 * <p>
 * The Reflection API, in contrast, always reports all parameters from the descriptor via {@link Constructor#getParameters()},
 * while {@link Constructor#getGenericParameterTypes()} follows the same logic as {@code getParameterTypes()}.
 * <p>
 * Records are covered by {@code ClassFileImporterRecordsTest}, static contexts inside of inner classes (Java 16+)
 * by {@code ClassFileImporterConstructorParametersNewerJavaVersionTest} and classes declared before the
 * {@code super(...)} call of a constructor (Java 25+) by {@code ClassFileImporterConstructorParametersFlexibleConstructorBodiesTest}.
 */
public class ClassFileImporterConstructorParametersTest {

    @Nested
    class StaticNestedClasses {
        @Test
        void constructor_without_generic_signature() {
            assertThatParametersOf(StaticNestedClassWithoutGenericParameter.class)
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }

        @Test
        void constructor_with_generic_signature() {
            assertThatParametersOf(StaticNestedClassWithGenericParameter.class)
                    .haveRawTypes(List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }
    }

    @Nested
    class InnerClasses {
        @Test
        void constructor_without_generic_signature_has_enclosing_instance_as_first_parameter() {
            // the descriptor is (Outer, String) and there is no Signature attribute,
            // so the enclosing instance appears in all three views
            assertThatParametersOf(Outer.InnerWithoutGenericParameter.class)
                    .haveRawTypes(Outer.class, String.class)
                    .haveTypes(concreteClass(Outer.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection()
                    // compare #1088: the annotation belongs to the String parameter, but the annotation attribute
                    // only counts declared parameters, so the importer attaches it to the enclosing instance
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflectionSinceJava(9, 1);
        }

        @Test
        void constructor_without_generic_signature_shifts_class_retention_parameter_annotations_the_same_way() {
            assertThatParametersOf(Outer.InnerWithClassRetentionParameterAnnotation.class)
                    .haveRawTypes(Outer.class, String.class)
                    .haveCount(2)
                    .haveAnnotationOnlyOnParameter(0, ClassRetentionParameterAnnotation.class);
        }

        @Test
        void constructor_with_generic_signature_omits_enclosing_instance_from_parameters() {
            // the descriptor is (Outer, List), but the Signature attribute is (List<String>)
            assertThatParametersOf(Outer.InnerWithGenericParameter.class)
                    .haveRawTypes(Outer.class, List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflectionSinceJava(9, 1);
        }

        @Test
        void constructor_without_declared_parameters_has_only_the_enclosing_instance_as_parameter() {
            assertThatParametersOf(Outer.InnerWithoutParameters.class)
                    .haveRawTypes(Outer.class)
                    .haveTypes(concreteClass(Outer.class))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_generic_inner_class_with_type_variable_parameter_omits_enclosing_instance_from_parameters() {
            // the descriptor is (Outer, Object), but the Signature attribute is (T)
            assertThatParametersOf(Outer.GenericInnerWithTypeVariableParameter.class)
                    .haveRawTypes(Outer.class, Object.class)
                    .haveTypes(typeVariable("T"))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1);
        }

        @Test
        void constructor_of_generic_inner_class_without_type_variable_parameter_has_enclosing_instance_as_first_parameter() {
            // javac only writes a Signature attribute for a method whose own type differs from its erasure,
            // so it does not matter that the class declares a type parameter
            assertThatParametersOf(Outer.GenericInnerWithoutTypeVariableParameter.class)
                    .haveRawTypes(Outer.class, String.class)
                    .haveTypes(concreteClass(Outer.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_inner_class_of_generic_class_using_type_parameter_of_enclosing_class_omits_enclosing_instance_from_parameters() {
            assertThatParametersOf(GenericOuter.InnerUsingOuterTypeParameter.class)
                    .haveRawTypes(GenericOuter.class, List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(typeVariable("T")))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1);
        }

        @Test
        void constructor_of_inner_class_of_generic_class_not_using_type_parameter_of_enclosing_class_has_raw_enclosing_instance() {
            // the Signature attribute never mentions the enclosing instance, so there is no generic type GenericOuter<T>
            // for it anywhere in the class file (compare #1773)
            assertThatParametersOf(GenericOuter.InnerNotUsingOuterTypeParameter.class)
                    .haveRawTypes(GenericOuter.class, String.class)
                    .haveTypes(concreteClass(GenericOuter.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_inner_class_of_inner_class_has_immediately_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(Outer.Middle.Inner.class)
                    .haveRawTypes(Outer.Middle.class, String.class)
                    .haveTypes(concreteClass(Outer.Middle.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class LocalClasses {
        private final LocalClassExamples examples = new LocalClassExamples();

        @Test
        void constructor_of_local_class_in_instance_method_has_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(examples.localClassInInstanceMethod())
                    .haveRawTypes(LocalClassExamples.class, String.class)
                    .haveTypes(concreteClass(LocalClassExamples.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection()
                    // compare #1088, same as for inner classes
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    // the Reflection API does not try to align the annotations of local classes with the parameters
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_instance_method_with_generic_signature_omits_enclosing_instance_from_parameters() {
            assertThatParametersOf(examples.localClassInInstanceMethodWithGenericParameter())
                    .haveRawTypes(LocalClassExamples.class, List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_instance_method_capturing_variable_omits_enclosing_instance_and_captured_variable_from_parameters() {
            // the descriptor is (LocalClassExamples, String, int); javac writes a Signature attribute (String) because of the captured variable,
            // even though there is no generic type involved
            assertThatParametersOf(examples.localClassInInstanceMethodCapturingVariable())
                    .haveRawTypes(LocalClassExamples.class, String.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_instance_method_capturing_generic_variable_only_has_erased_type_of_captured_variable() {
            // the captured List<String> only appears as List in the descriptor; the Signature attribute does not list it,
            // so its generic type is not recoverable from the class file
            assertThatParametersOf(examples.localClassInInstanceMethodCapturingGenericVariable())
                    .haveRawTypes(LocalClassExamples.class, String.class, List.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2);
        }

        @Test
        void constructor_of_local_class_in_instance_initializer_has_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(examples.localClassInInstanceInitializer)
                    .haveRawTypes(LocalClassExamples.class, String.class)
                    .haveTypes(concreteClass(LocalClassExamples.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_local_class_in_constructor_has_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(examples.localClassInConstructor)
                    .haveRawTypes(LocalClassExamples.class, String.class)
                    .haveTypes(concreteClass(LocalClassExamples.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_local_class_in_static_method_has_no_enclosing_instance() {
            assertThatParametersOf(LocalClassExamples.localClassInStaticMethod())
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }

        @Test
        void constructor_of_local_class_in_static_method_capturing_variable_omits_captured_variable_from_parameters() {
            // the descriptor is (String, int) and the Signature attribute is (String)
            assertThatParametersOf(LocalClassExamples.localClassInStaticMethodCapturingVariable())
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_static_method_capturing_generic_variable_only_has_erased_type_of_captured_variable() {
            assertThatParametersOf(LocalClassExamples.localClassInStaticMethodCapturingGenericVariable())
                    .haveRawTypes(String.class, List.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1);
        }

        @Test
        void constructor_of_local_class_in_static_initializer_has_no_enclosing_instance() {
            assertThatParametersOf(LOCAL_CLASS_IN_STATIC_INITIALIZER)
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }

        @Test
        void constructor_of_local_class_in_static_initializer_capturing_variable_omits_captured_variable_from_parameters() {
            assertThatParametersOf(LOCAL_CLASS_IN_STATIC_INITIALIZER_CAPTURING_VARIABLE)
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_lambda_in_instance_method_has_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(examples.localClassInLambdaInInstanceMethod())
                    .haveRawTypes(LocalClassExamples.class, String.class)
                    .haveTypes(concreteClass(LocalClassExamples.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_local_class_in_lambda_in_static_method_has_no_enclosing_instance() {
            assertThatParametersOf(LocalClassExamples.localClassInLambdaInStaticMethod())
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_local_class_in_anonymous_class_in_static_method_has_anonymous_class_as_enclosing_instance() {
            Class<?> localClass = LocalClassExamples.localClassInAnonymousClassInStaticMethod();
            Class<?> anonymousClass = localClass.getEnclosingClass();

            // the context is static, but the local class is declared inside a method of the anonymous class,
            // so the instance of the anonymous class is its enclosing instance
            assertThat(anonymousClass.isAnonymousClass()).as("enclosing class is anonymous").isTrue();
            assertThatParametersOf(localClass)
                    .haveRawTypes(anonymousClass, String.class)
                    .haveTypes(concreteClass(anonymousClass), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructors_of_local_class_and_its_anonymous_subclass_both_have_all_captured_variables_as_parameters() {
            List<Class<?>> classes = LocalClassExamples.localClassWithAnonymousSubclassCapturingDifferentVariablesInStaticMethod();
            Class<?> localClass = classes.get(0);
            Class<?> anonymousSubclass = classes.get(1);

            // each class captures only one of the two variables, but the anonymous subclass has to pass on the variable captured
            // by its superclass, so javac gives both constructors parameters for both variables;
            // the local class gets a Signature attribute (String) because of the captured variables, the anonymous subclass never does
            assertThatParametersOf(localClass)
                    .haveRawTypes(String.class, int.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2);
            assertThatParametersOf(anonymousSubclass)
                    .haveRawTypes(localClass, String.class, int.class, int.class)
                    .haveTypes(concreteClass(localClass), concreteClass(String.class), concreteClass(int.class), concreteClass(int.class))
                    .haveCount(4)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class AnonymousClasses {
        private final AnonymousClassExamples examples = new AnonymousClassExamples();

        @Test
        void constructor_of_anonymous_class_in_instance_method_has_enclosing_instance_as_parameter() {
            assertThatParametersOf(examples.anonymousClassInInstanceMethod())
                    .haveRawTypes(AnonymousClassExamples.class)
                    .haveTypes(concreteClass(AnonymousClassExamples.class))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_instance_method_forwards_superclass_constructor_argument_after_enclosing_instance() {
            assertThatParametersOf(examples.anonymousClassInInstanceMethodForwardingSuperclassConstructorArgument())
                    .haveRawTypes(AnonymousClassExamples.class, String.class)
                    .haveTypes(concreteClass(AnonymousClassExamples.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_instance_method_capturing_variable_has_trailing_captured_variable_as_parameter() {
            // anonymous constructors never have a Signature attribute, so the captured variable appears in all three views
            assertThatParametersOf(examples.anonymousClassInInstanceMethodCapturingVariable())
                    .haveRawTypes(AnonymousClassExamples.class, String.class, int.class)
                    .haveTypes(concreteClass(AnonymousClassExamples.class), concreteClass(String.class), concreteClass(int.class))
                    .haveCount(3)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_static_method_capturing_variable_has_no_enclosing_instance() {
            // the descriptor is (String, int), where String is forwarded to the superclass constructor and int is the captured variable;
            // neither the descriptor nor a Signature attribute distinguishes the two
            assertThatParametersOf(AnonymousClassExamples.anonymousClassInStaticMethodCapturingVariable())
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class), concreteClass(int.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_capturing_generic_variable_only_has_erased_type_of_captured_variable() {
            assertThatParametersOf(AnonymousClassExamples.anonymousClassInStaticMethodCapturingGenericVariable())
                    .haveRawTypes(String.class, List.class)
                    .haveTypes(concreteClass(String.class), concreteClass(List.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructors_of_nested_anonymous_classes_have_parameters_for_all_captured_variables_of_the_nesting() {
            ClassWithConstructorParameter outerAnonymousInstance =
                    AnonymousClassExamples.anonymousClassWithNestedAnonymousClassInStaticMethod("param");
            Class<?> outerAnonymousClass = outerAnonymousInstance.getClass();
            Class<?> nestedAnonymousClass = outerAnonymousInstance.nestedAnonymousClass();

            // the outer anonymous class forwards the String to its superclass constructor and captures the int it uses itself
            // as well as the String and the double that the nested anonymous class uses, since it has to pass them on
            assertThatParametersOf(outerAnonymousClass)
                    .haveRawTypes(String.class, int.class, String.class, double.class)
                    .haveTypes(concreteClass(String.class), concreteClass(int.class), concreteClass(String.class), concreteClass(double.class))
                    .haveCount(4)
                    .haveSameCountAsReflection();
            // the nested anonymous class has the outer anonymous class as enclosing instance and forwards the String
            // to its superclass constructor; whether it captures the double itself depends on the compiler: javac 25 adds
            // a parameter for it, while javac 11 to 23 read it from the field of the enclosing instance
            // (the tests are compiled and run with the same JDK)
            if (currentJavaMajorVersion() >= 25) {
                assertThatParametersOf(nestedAnonymousClass)
                        .haveRawTypes(outerAnonymousClass, String.class, double.class)
                        .haveTypes(concreteClass(outerAnonymousClass), concreteClass(String.class), concreteClass(double.class))
                        .haveCount(3)
                        .haveSameCountAsReflection();
            } else {
                assertThatParametersOf(nestedAnonymousClass)
                        .haveRawTypes(outerAnonymousClass, String.class)
                        .haveTypes(concreteClass(outerAnonymousClass), concreteClass(String.class))
                        .haveCount(2)
                        .haveSameCountAsReflection();
            }
        }

        @Test
        void constructor_of_anonymous_subclass_of_inner_class_in_static_method_has_enclosing_instance_of_superclass_as_parameter() {
            // the anonymous class itself has no enclosing instance, but it has to pass the explicitly qualified enclosing instance
            // ('outer.new InnerWithoutParameters() {}') on to the superclass constructor
            assertThatParametersOf(Outer.anonymousSubclassOfInnerWithoutParametersInStaticMethod())
                    .haveRawTypes(Outer.class)
                    .haveTypes(concreteClass(Outer.class))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_subclass_of_inner_class_in_instance_method_of_outer_class_has_shared_enclosing_instance_as_parameter() {
            // the enclosing instance of the anonymous class is at the same time the enclosing instance of its superclass
            assertThatParametersOf(new Outer().anonymousSubclassOfInnerWithoutGenericParameterInInstanceMethod())
                    .haveRawTypes(Outer.class, String.class)
                    .haveTypes(concreteClass(Outer.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_subclass_of_inner_class_in_other_inner_class_has_only_its_own_enclosing_instance_as_parameter() {
            // the superclass constructor has the descriptor (Outer, String), but the anonymous constructor takes the Outer instance
            // from the this$0 field of its own enclosing instance (OtherInner) instead of from a parameter
            assertThatParametersOf(new Outer().new OtherInner().anonymousSubclassOfInnerWithoutGenericParameter())
                    .haveRawTypes(Outer.OtherInner.class, String.class)
                    .haveTypes(concreteClass(Outer.OtherInner.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class Enums {
        @Test
        void constructor_without_generic_parameter_omits_name_and_ordinal_from_parameters() {
            // the descriptor is (String, int, String); javac writes a Signature attribute (String) because of the synthetic parameters,
            // even though there is no generic type involved
            assertThatParametersOf(EnumWithConstructorParameter.class)
                    .haveRawTypes(String.class, int.class, String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflectionSinceJava(17, 2);
        }

        @Test
        void constructor_with_generic_parameter_omits_name_and_ordinal_from_parameters() {
            assertThatParametersOf(EnumWithGenericConstructorParameter.class)
                    .haveRawTypes(String.class, int.class, List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflectionSinceJava(17, 2);
        }

        @Test
        void implicit_constructor_has_name_and_ordinal_in_all_views_because_its_signature_attribute_lists_no_parameters() {
            // the descriptor is (String, int) and the Signature attribute is (), i.e. it lists no parameters at all;
            // the importer cannot distinguish an empty Signature attribute from a missing one and falls back to the descriptor,
            // so in contrast to the constructors above name and ordinal appear in all three views here
            assertThatParametersOf(EnumWithoutConstructorParameters.class)
                    .haveRawTypes(String.class, int.class)
                    .haveTypesWhileReflectionReportsNoGenericParameterTypes(concreteClass(String.class), concreteClass(int.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_enum_constant_with_class_body_is_an_anonymous_constructor_with_name_and_ordinal() {
            assertThatParametersOf(EnumWithConstantWithClassBody.CONSTANT.getClass())
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class), concreteClass(int.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class Lambdas {
        @Test
        void synthetic_lambda_methods_with_captured_parameters_are_not_imported_as_code_units() {
            JavaClass javaClass = new ClassFileImporter().importClass(ClassWithCapturingLambdaInInstanceMethod.class);

            assertThat(javaClass.getCodeUnits().stream().map(JavaCodeUnit::getName).collect(toList()))
                    .containsOnly(JavaConstructor.CONSTRUCTOR_NAME, "supplier");
        }

        @Test
        void constructor_of_equivalent_anonymous_class_has_enclosing_instance_and_captured_variable_as_parameters() {
            Class<?> anonymousClass = new ClassWithCapturingAnonymousClassInInstanceMethod().supplier("captured").getClass();

            ConstructorParametersAssertion assertion = assertThatParametersOf(anonymousClass)
                    .haveRawTypes(ClassWithCapturingAnonymousClassInInstanceMethod.class, String.class);
            // javac 17+ (but not javac 11) writes a Signature attribute () that lists no parameters for the constructor
            // of an anonymous class that implements an interface and captures variables (it does not for an anonymous subclass
            // of a class). The importer treats it like a missing one (compare the implicit enum constructor),
            // while Constructor.getGenericParameterTypes() returns an empty array. The tests are compiled and run with the same JDK.
            if (currentJavaMajorVersion() >= 17) {
                assertion.haveTypesWhileReflectionReportsNoGenericParameterTypes(
                        concreteClass(ClassWithCapturingAnonymousClassInInstanceMethod.class), concreteClass(String.class));
            } else {
                assertion.haveTypes(concreteClass(ClassWithCapturingAnonymousClassInInstanceMethod.class), concreteClass(String.class));
            }
            assertion
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }
    }

    @SuppressWarnings("unused")
    static class StaticNestedClassWithoutGenericParameter {
        StaticNestedClassWithoutGenericParameter(@SomeParameterAnnotation String param) {
        }
    }

    @SuppressWarnings("unused")
    static class StaticNestedClassWithGenericParameter {
        StaticNestedClassWithGenericParameter(@SomeParameterAnnotation List<String> param) {
        }
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class Outer {
        class InnerWithoutGenericParameter {
            InnerWithoutGenericParameter(@SomeParameterAnnotation String param) {
            }
        }

        class InnerWithClassRetentionParameterAnnotation {
            InnerWithClassRetentionParameterAnnotation(@ClassRetentionParameterAnnotation String param) {
            }
        }

        class InnerWithGenericParameter {
            InnerWithGenericParameter(@SomeParameterAnnotation List<String> param) {
            }
        }

        class InnerWithoutParameters {
        }

        class GenericInnerWithTypeVariableParameter<T> {
            GenericInnerWithTypeVariableParameter(T param) {
            }
        }

        class GenericInnerWithoutTypeVariableParameter<T> {
            GenericInnerWithoutTypeVariableParameter(String param) {
            }
        }

        class Middle {
            class Inner {
                Inner(String param) {
                }
            }
        }

        class OtherInner {
            Class<?> anonymousSubclassOfInnerWithoutGenericParameter() {
                return new InnerWithoutGenericParameter("param") {
                }.getClass();
            }
        }

        Class<?> anonymousSubclassOfInnerWithoutGenericParameterInInstanceMethod() {
            return new InnerWithoutGenericParameter("param") {
            }.getClass();
        }

        static Class<?> anonymousSubclassOfInnerWithoutParametersInStaticMethod() {
            Outer outer = new Outer();
            return outer.new InnerWithoutParameters() {
            }.getClass();
        }
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class GenericOuter<T> {
        class InnerUsingOuterTypeParameter {
            InnerUsingOuterTypeParameter(List<T> param) {
            }
        }

        class InnerNotUsingOuterTypeParameter {
            InnerNotUsingOuterTypeParameter(String param) {
            }
        }
    }

    @SuppressWarnings("unused")
    enum EnumWithConstructorParameter {
        CONSTANT("param");

        EnumWithConstructorParameter(@SomeParameterAnnotation String param) {
        }
    }

    @SuppressWarnings("unused")
    enum EnumWithGenericConstructorParameter {
        CONSTANT(emptyList());

        EnumWithGenericConstructorParameter(@SomeParameterAnnotation List<String> param) {
        }
    }

    enum EnumWithoutConstructorParameters {
        CONSTANT
    }

    enum EnumWithConstantWithClassBody {
        CONSTANT {
            @Override
            public String toString() {
                return "constant with class body";
            }
        }
    }
}
