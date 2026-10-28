package com.tngtech.archunit.core.importer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
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
 * (i.e. enum name and ordinal or captured variables). It never writes one for the implicit constructor
 * of an anonymous class. {@code getRawParameterTypes()} is derived from the descriptor,
 * while {@code getParameterTypes()} and {@code getParameters()} are derived from the {@code Signature} attribute if present
 * and from the descriptor otherwise (also if the {@code Signature} attribute is present, but lists no parameters at all).
 * Thus, the latter two sometimes contain the compiler-added parameters and sometimes not,
 * and parameter annotations (which only count declared parameters) are shifted if they do (compare #1088).
 * <p>
 * The Reflection API, in contrast, always reports all parameters from the descriptor via {@link Constructor#getParameters()},
 * while {@link Constructor#getGenericParameterTypes()} follows the same logic as {@code getParameterTypes()}.
 */
public class ClassFileImporterConstructorParametersTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface SomeParameterAnnotation {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.PARAMETER)
    public @interface ClassRetentionParameterAnnotation {
    }

    @Nested
    class StaticNestedClasses {
        @Test
        void constructor_without_generic_signature() {
            assertThatParametersOf(StaticNestedClassWithoutGenerics.class)
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }

        @Test
        void constructor_with_generic_signature() {
            assertThatParametersOf(StaticNestedClassWithGenerics.class)
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
            assertThatParametersOf(Outer.InnerWithoutGenerics.class)
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
            assertThatParametersOf(Outer.InnerWithGenerics.class)
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
        void constructor_of_inner_class_of_generic_class_with_generic_signature() {
            assertThatParametersOf(GenericOuter.InnerWithGenericParameter.class)
                    .haveRawTypes(GenericOuter.class, List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(typeVariable("T")))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1);
        }

        @Test
        void constructor_of_inner_class_of_generic_class_without_generic_signature_has_raw_enclosing_instance() {
            // the Signature attribute never mentions the enclosing instance, so there is no generic type GenericOuter<T>
            // for it anywhere in the class file (compare #1773)
            assertThatParametersOf(GenericOuter.InnerWithoutGenericParameter.class)
                    .haveRawTypes(GenericOuter.class, String.class)
                    .haveTypes(concreteClass(GenericOuter.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_inner_class_of_inner_class_has_immediately_enclosing_instance_as_first_parameter() {
            assertThatParametersOf(Outer.Inner.Innermost.class)
                    .haveRawTypes(Outer.Inner.class, String.class)
                    .haveTypes(concreteClass(Outer.Inner.class), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class LocalClasses {
        @Test
        void constructor_of_local_class_in_instance_method_without_generic_signature() {
            @SuppressWarnings("unused")
            class LocalClass {
                LocalClass(@SomeParameterAnnotation String param) {
                }
            }

            assertThatParametersOf(LocalClass.class)
                    .haveRawTypes(getClass(), String.class)
                    .haveTypes(concreteClass(getClass()), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection()
                    // compare #1088, same as for inner classes
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    // the Reflection API does not try to align the annotations of local classes with the parameters
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_instance_method_with_generic_signature() {
            @SuppressWarnings("unused")
            class LocalClass {
                LocalClass(@SomeParameterAnnotation List<String> param) {
                }
            }

            assertThatParametersOf(LocalClass.class)
                    .haveRawTypes(getClass(), List.class)
                    .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_instance_method_capturing_variable_has_trailing_captured_parameter() {
            int captured = 42;
            @SuppressWarnings("unused")
            class LocalClass {
                LocalClass(@SomeParameterAnnotation String param) {
                }

                int captured() {
                    return captured;
                }
            }

            // the descriptor is (Outer, String, int); javac writes a Signature attribute (String) because of the captured variable,
            // even though there is no generic type involved
            assertThatParametersOf(LocalClass.class)
                    .haveRawTypes(getClass(), String.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(2)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_in_static_method_has_no_enclosing_instance() {
            assertThatParametersOf(localClassInStaticMethod())
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection()
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .haveAnnotationOnlyOnParameterLikeReflection(0);
        }

        @Test
        void constructor_of_local_class_in_static_method_capturing_variable_has_trailing_captured_parameter() {
            // the descriptor is (String, int) and the Signature attribute is (String)
            assertThatParametersOf(localClassInStaticMethodCapturingVariable())
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1)
                    .haveAnnotationOnlyOnParameter(0, SomeParameterAnnotation.class)
                    .reflectionReportsAnnotationsForDeclaredParametersOnly(1);
        }

        @Test
        void constructor_of_local_class_capturing_generic_variable_only_has_erased_type_of_captured_variable() {
            // the captured List<String> only appears as List in the descriptor; the Signature attribute does not list it,
            // so its generic type is not recoverable from the class file
            assertThatParametersOf(localClassInStaticMethodCapturingGenericVariable())
                    .haveRawTypes(String.class, List.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveFewerParametersThanReflection(1);
        }

        @Test
        void constructor_of_local_class_in_lambda_in_instance_method_has_enclosing_instance() {
            Supplier<Class<?>> localClass = () -> {
                @SuppressWarnings("unused")
                class LocalClassInLambda {
                    LocalClassInLambda(String param) {
                    }
                }
                return LocalClassInLambda.class;
            };

            assertThatParametersOf(localClass.get())
                    .haveRawTypes(getClass(), String.class)
                    .haveTypes(concreteClass(getClass()), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_local_class_in_lambda_in_static_method_has_no_enclosing_instance() {
            assertThatParametersOf(localClassInLambdaInStaticMethod())
                    .haveRawTypes(String.class)
                    .haveTypes(concreteClass(String.class))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }
    }

    @Nested
    class AnonymousClasses {
        @Test
        void constructor_of_anonymous_class_in_instance_method_has_enclosing_instance() {
            Class<?> anonymousClass = new Object() {
            }.getClass();

            assertThatParametersOf(anonymousClass)
                    .haveRawTypes(getClass())
                    .haveTypes(concreteClass(getClass()))
                    .haveCount(1)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_instance_method_forwards_superclass_constructor_arguments() {
            Class<?> anonymousClass = new BaseClassWithConstructorParameter("param") {
            }.getClass();

            assertThatParametersOf(anonymousClass)
                    .haveRawTypes(getClass(), String.class)
                    .haveTypes(concreteClass(getClass()), concreteClass(String.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_instance_method_capturing_variable_has_trailing_captured_parameter() {
            int captured = 42;
            Class<?> anonymousClass = new BaseClassWithConstructorParameter("param") {
                @Override
                int captured() {
                    return captured;
                }
            }.getClass();

            // anonymous constructors never have a Signature attribute, so the captured variable appears in all three views
            assertThatParametersOf(anonymousClass)
                    .haveRawTypes(getClass(), String.class, int.class)
                    .haveTypes(concreteClass(getClass()), concreteClass(String.class), concreteClass(int.class))
                    .haveCount(3)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_class_in_static_method_capturing_variable_has_no_enclosing_instance() {
            // the descriptor is (String, int), where String is forwarded to the superclass constructor and int is the captured variable;
            // nothing in the class file distinguishes the two
            assertThatParametersOf(anonymousClassInStaticMethodCapturingVariable())
                    .haveRawTypes(String.class, int.class)
                    .haveTypes(concreteClass(String.class), concreteClass(int.class))
                    .haveCount(2)
                    .haveSameCountAsReflection();
        }

        @Test
        void constructor_of_anonymous_subclass_of_inner_class_in_static_method_forwards_enclosing_instance_of_superclass() {
            assertThatParametersOf(anonymousSubclassOfInnerClassInStaticMethod())
                    .haveRawTypes(Outer.class)
                    .haveTypes(concreteClass(Outer.class))
                    .haveCount(1)
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
            JavaClass javaClass = new ClassFileImporter().importClass(ClassWithCapturingLambda.class);

            assertThat(javaClass.getCodeUnits().stream().map(JavaCodeUnit::getName).collect(toList()))
                    .containsOnly(JavaConstructor.CONSTRUCTOR_NAME, "supplier");
        }
    }

    private static ConstructorParametersAssertion assertThatParametersOf(Class<?> owner) {
        JavaClass javaClass = new ClassFileImporter().importClass(owner);
        return new ConstructorParametersAssertion(getOnlyElement(javaClass.getConstructors()));
    }

    private static class ConstructorParametersAssertion {
        private static final int JAVA_VERSION = javaSpecificationVersion();

        private final JavaConstructor constructor;
        private final Constructor<?> reflected;

        ConstructorParametersAssertion(JavaConstructor constructor) {
            this.constructor = constructor;
            this.reflected = constructor.reflect();
        }

        ConstructorParametersAssertion haveRawTypes(Class<?>... types) {
            assertThatTypes(constructor.getRawParameterTypes()).as("raw parameter types").matchExactly(types);
            // the raw parameter types always reflect the descriptor, like the Reflection API
            assertThatTypes(constructor.getRawParameterTypes()).as("raw parameter types").matchExactly(reflected.getParameterTypes());
            return this;
        }

        ConstructorParametersAssertion haveTypes(ExpectedConcreteType... types) {
            assertThatTypes(constructor.getParameterTypes()).as("parameter types").matchExactly(types);
            // at the moment the parameter types mirror Constructor.getGenericParameterTypes(),
            // which omits compiler-added parameters whenever a Signature attribute is present
            assertThat(namesOf(constructor.getParameterTypes())).as("parameter types")
                    .containsExactlyElementsOf(typeNamesOf(reflected.getGenericParameterTypes()));
            return this;
        }

        ConstructorParametersAssertion haveTypesWhileReflectionReportsNoGenericParameterTypes(ExpectedConcreteType... types) {
            assertThatTypes(constructor.getParameterTypes()).as("parameter types").matchExactly(types);
            assertThat(reflected.getGenericParameterTypes()).as("generic parameter types from Reflection").isEmpty();
            return this;
        }

        ConstructorParametersAssertion haveCount(int count) {
            List<JavaParameter> parameters = constructor.getParameters();
            assertThat(parameters).as("parameters").hasSize(count);
            // the parameters always follow getParameterTypes()
            assertThat(parameters).as("parameters").hasSameSizeAs(constructor.getParameterTypes());
            for (int i = 0; i < parameters.size(); i++) {
                JavaParameter parameter = parameters.get(i);
                assertThat(parameter.getIndex()).as("index of parameter " + i).isEqualTo(i);
                assertThat(parameter.getOwner()).as("owner of parameter " + i).isEqualTo(constructor);
                assertThat(parameter.getType().getName()).as("type of parameter " + i)
                        .isEqualTo(constructor.getParameterTypes().get(i).getName());
                assertThatType(parameter.getRawType()).as("raw type of parameter " + i)
                        .matches(constructor.getParameterTypes().get(i).toErasure().reflect());
            }
            return this;
        }

        ConstructorParametersAssertion haveSameCountAsReflection() {
            assertThat(constructor.getParameters()).as("parameters").hasSize(reflected.getParameterCount());
            return this;
        }

        ConstructorParametersAssertion haveFewerParametersThanReflection(int difference) {
            assertThat(constructor.getParameters()).as("parameters").hasSize(reflected.getParameterCount() - difference);
            return this;
        }

        ConstructorParametersAssertion haveAnnotationOnlyOnParameter(int index, Class<? extends Annotation> annotationType) {
            List<JavaParameter> parameters = constructor.getParameters();
            List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = constructor.getParameterAnnotations();
            assertThat(parameterAnnotations).as("parameter annotations").hasSameSizeAs(parameters);
            for (int i = 0; i < parameters.size(); i++) {
                assertThat(parameters.get(i).getAnnotations()).as("annotations of parameter " + i).isEqualTo(parameterAnnotations.get(i));
                if (i == index) {
                    JavaAnnotation<JavaParameter> annotation = getOnlyElement(parameterAnnotations.get(i));
                    assertThatType(annotation.getRawType()).as("annotation of parameter " + i).matches(annotationType);
                    assertThat(annotation.getOwner()).as("owner of annotation of parameter " + i).isEqualTo(parameters.get(i));
                } else {
                    assertThat(parameterAnnotations.get(i)).as("annotations of parameter " + i).isEmpty();
                }
            }
            return this;
        }

        ConstructorParametersAssertion haveAnnotationOnlyOnParameterLikeReflection(int index) {
            Annotation[][] reflectedAnnotations = reflected.getParameterAnnotations();
            assertThat(reflectedAnnotations).as("parameter annotations from Reflection").hasNumberOfRows(reflected.getParameterCount());
            for (int i = 0; i < reflectedAnnotations.length; i++) {
                assertThat(reflectedAnnotations[i]).as("annotations from Reflection of parameter " + i).hasSize(i == index ? 1 : 0);
            }
            return this;
        }

        /**
         * The Reflection API only accounts for compiler-added parameters when aligning parameter annotations
         * since JDK 9 for the enclosing instance of inner classes (compare JDK-8074977)
         * and since JDK 17 for name and ordinal of enums (compare JDK-8263763).
         * On older JDKs {@link Constructor#getParameterAnnotations()} just has one entry per declared parameter.
         */
        ConstructorParametersAssertion haveAnnotationOnlyOnParameterLikeReflectionSinceJava(int javaVersion, int index) {
            if (JAVA_VERSION < javaVersion) {
                assertThat(reflected.getParameterAnnotations().length).as("number of parameter annotations from Reflection")
                        .isLessThan(reflected.getParameterCount());
                return this;
            }
            return haveAnnotationOnlyOnParameterLikeReflection(index);
        }

        /**
         * For local classes the Reflection API gives up aligning parameter annotations with parameters
         * ("Can't do reliable parameter counting" in {@code Constructor.handleParameterNumberMismatch}, compare JDK-8303112),
         * so {@link Constructor#getParameterAnnotations()} only has one entry per declared parameter.
         */
        ConstructorParametersAssertion reflectionReportsAnnotationsForDeclaredParametersOnly(int numberOfDeclaredParameters) {
            assertThat(reflected.getParameterAnnotations()).as("parameter annotations from Reflection").hasNumberOfRows(numberOfDeclaredParameters);
            return this;
        }

        private static int javaSpecificationVersion() {
            String version = System.getProperty("java.specification.version");
            return Integer.parseInt(version.startsWith("1.") ? version.substring(2) : version);
        }

        private static List<String> namesOf(List<JavaType> types) {
            return types.stream().map(JavaType::getName).collect(toList());
        }

        private static List<String> typeNamesOf(Type[] types) {
            return java.util.Arrays.stream(types).map(Type::getTypeName).collect(toList());
        }
    }

    @SuppressWarnings("unused")
    static class StaticNestedClassWithoutGenerics {
        StaticNestedClassWithoutGenerics(@SomeParameterAnnotation String param) {
        }
    }

    @SuppressWarnings("unused")
    static class StaticNestedClassWithGenerics {
        StaticNestedClassWithGenerics(@SomeParameterAnnotation List<String> param) {
        }
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class Outer {
        class InnerWithoutGenerics {
            InnerWithoutGenerics(@SomeParameterAnnotation String param) {
            }
        }

        class InnerWithClassRetentionParameterAnnotation {
            InnerWithClassRetentionParameterAnnotation(@ClassRetentionParameterAnnotation String param) {
            }
        }

        class InnerWithGenerics {
            InnerWithGenerics(@SomeParameterAnnotation List<String> param) {
            }
        }

        class InnerWithoutParameters {
        }

        class Inner {
            class Innermost {
                Innermost(String param) {
                }
            }
        }
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class GenericOuter<T> {
        class InnerWithGenericParameter {
            InnerWithGenericParameter(List<T> param) {
            }
        }

        class InnerWithoutGenericParameter {
            InnerWithoutGenericParameter(String param) {
            }
        }
    }

    @SuppressWarnings("unused")
    static class BaseClassWithConstructorParameter {
        BaseClassWithConstructorParameter(String param) {
        }

        int captured() {
            return 0;
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

    @SuppressWarnings("unused")
    static class ClassWithCapturingLambda {
        Supplier<String> supplier(String captured) {
            return () -> captured;
        }
    }

    private static Class<?> localClassInStaticMethod() {
        @SuppressWarnings("unused")
        class LocalClassInStaticMethod {
            LocalClassInStaticMethod(@SomeParameterAnnotation String param) {
            }
        }
        return LocalClassInStaticMethod.class;
    }

    private static Class<?> localClassInStaticMethodCapturingVariable() {
        int captured = 42;
        @SuppressWarnings("unused")
        class LocalClassInStaticMethodCapturingVariable {
            LocalClassInStaticMethodCapturingVariable(@SomeParameterAnnotation String param) {
            }

            int captured() {
                return captured;
            }
        }
        return LocalClassInStaticMethodCapturingVariable.class;
    }

    private static Class<?> localClassInStaticMethodCapturingGenericVariable() {
        List<String> captured = emptyList();
        @SuppressWarnings("unused")
        class LocalClassInStaticMethodCapturingGenericVariable {
            LocalClassInStaticMethodCapturingGenericVariable(String param) {
            }

            List<String> captured() {
                return captured;
            }
        }
        return LocalClassInStaticMethodCapturingGenericVariable.class;
    }

    private static Class<?> localClassInLambdaInStaticMethod() {
        Supplier<Class<?>> localClass = () -> {
            @SuppressWarnings("unused")
            class LocalClassInLambdaInStaticMethod {
                LocalClassInLambdaInStaticMethod(String param) {
                }
            }
            return LocalClassInLambdaInStaticMethod.class;
        };
        return localClass.get();
    }

    private static Class<?> anonymousClassInStaticMethodCapturingVariable() {
        int captured = 42;
        return new BaseClassWithConstructorParameter("param") {
            @Override
            int captured() {
                return captured;
            }
        }.getClass();
    }

    private static Class<?> anonymousSubclassOfInnerClassInStaticMethod() {
        Outer outer = new Outer();
        return outer.new InnerWithoutParameters() {
        }.getClass();
    }
}
