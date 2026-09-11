package com.tngtech.archunit.testutil.assertion;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.InstanceOfAssertFactories;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;

public class JavaAnnotationsAssertion extends AbstractIterableAssert<JavaAnnotationsAssertion, Set<JavaAnnotation<?>>, JavaAnnotation<?>, JavaAnnotationAssertion> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public JavaAnnotationsAssertion(Set<? extends JavaAnnotation<?>> javaAnnotations) {
        super((Set) javaAnnotations, JavaAnnotationsAssertion.class);
    }

    public JavaAnnotationsAssertion match(Annotation[] annotations) {
        return match(ImmutableSet.copyOf(annotations));
    }

    public JavaAnnotationsAssertion match(Collection<Annotation> annotations) {
        Map<String, JavaAnnotation<?>> actualByClassName = annotationsByClassName(actual);
        Map<String, Annotation> reflectionByClassName = reflectionByClassName(annotations);
        assertThat(actualByClassName.keySet())
                .as("annotation type names")
                .containsExactlyInAnyOrderElementsOf(reflectionByClassName.keySet());

        for (Map.Entry<String, JavaAnnotation<?>> entry : actualByClassName.entrySet()) {
            Annotation reflectionAnnotation = reflectionByClassName.get(entry.getKey());
            assertThatAnnotation(entry.getValue()).matches(reflectionAnnotation);
        }

        return myself;
    }

    private Map<String, JavaAnnotation<?>> annotationsByClassName(Iterable<JavaAnnotation<?>> annotations) {
        ImmutableMap.Builder<String, JavaAnnotation<?>> result = ImmutableMap.builder();
        for (JavaAnnotation<?> annotation : annotations) {
            result.put(annotation.getRawType().getName(), annotation);
        }
        return result.build();
    }

    private Map<String, Annotation> reflectionByClassName(Iterable<Annotation> annotations) {
        ImmutableMap.Builder<String, Annotation> result = ImmutableMap.builder();
        for (Annotation annotation : annotations) {
            result.put(annotation.annotationType().getName(), annotation);
        }
        return result.build();
    }

    @SafeVarargs
    public final JavaAnnotationsAssertion matchClasses(Class<? extends Annotation>... classes) {
        @SuppressWarnings("unchecked")
        Class<Class<? extends Annotation>> elementType = (Class<Class<? extends Annotation>>) (Class<?>) Annotation.class;
        myself.extracting(a -> a.getRawType().reflect())
                .asInstanceOf(InstanceOfAssertFactories.collection(elementType))
                .containsExactlyInAnyOrder(classes);
        return myself;
    }

    @Override
    protected JavaAnnotationAssertion toAssert(JavaAnnotation<?> value, String description) {
        return assertThatAnnotation(value).as(description);
    }

    @Override
    protected JavaAnnotationsAssertion newAbstractIterableAssert(Iterable<? extends JavaAnnotation<?>> iterable) {
        return new JavaAnnotationsAssertion(ImmutableSet.copyOf(iterable));
    }
}
