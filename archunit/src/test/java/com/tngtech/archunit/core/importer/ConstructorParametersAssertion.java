package com.tngtech.archunit.core.importer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.TestUtils.currentJavaMajorVersion;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Assertions on the parameters of the only constructor of a class, used by the tests documenting constructor
 * parameters that were added by the compiler (compare #1772). Each assertion on the ArchUnit view of the parameters
 * is accompanied by the corresponding view of the Reflection API, so that the tests show where both agree and where they differ.
 */
class ConstructorParametersAssertion {
    private static final int JAVA_VERSION = currentJavaMajorVersion();

    private final JavaConstructor constructor;
    private final Constructor<?> reflected;

    private ConstructorParametersAssertion(JavaConstructor constructor) {
        this.constructor = constructor;
        this.reflected = constructor.reflect();
    }

    static ConstructorParametersAssertion assertThatParametersOf(Class<?> owner) {
        JavaClass javaClass = new ClassFileImporter().importClass(owner);
        return new ConstructorParametersAssertion(getOnlyElement(javaClass.getConstructors()));
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

    /**
     * {@link Parameter#isImplicit()} reports the {@code mandated} flag of the {@code MethodParameters} attribute,
     * i.e. it is {@code false} for all parameters if the compiler did not write that attribute.
     */
    ConstructorParametersAssertion reflectionReportsParametersAsImplicit(Boolean... implicit) {
        List<Boolean> actual = stream(reflected.getParameters()).map(Parameter::isImplicit).collect(toList());
        assertThat(actual).as("Parameter.isImplicit() from Reflection").containsExactly(implicit);
        return this;
    }

    private static List<String> namesOf(List<JavaType> types) {
        return types.stream().map(JavaType::getName).collect(toList());
    }

    private static List<String> typeNamesOf(Type[] types) {
        return stream(types).map(Type::getTypeName).collect(toList());
    }
}
