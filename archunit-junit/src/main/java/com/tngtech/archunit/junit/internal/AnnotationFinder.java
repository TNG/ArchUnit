package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

class AnnotationFinder<T extends Annotation> {

    private final Class<T> annotationClass;

    public AnnotationFinder(final Class<T> annotationClass) {
        this.annotationClass = annotationClass;
    }

    /**
     * Recursively retrieve all {@link T} annotations from a given element.
     *
     * @param clazz The clazz from which to retrieve the annotation.
     * @return List of all found annotation instance or empty list.
     */
    public List<T> findAnnotationsOn(final Class<?> clazz) {
        return findAnnotations(clazz.getAnnotations(), new HashSet<>());
    }

    private List<T> findAnnotations(final Annotation[] annotations, final HashSet<Annotation> visited) {
        final List<T> result = new LinkedList<>();
        for (Annotation annotation : annotations) {
            if (visited.contains(annotation)) {
                continue;
            } else {
                visited.add(annotation);
            }
            if (annotationClass.isInstance(annotation)) {
                result.add(annotationClass.cast(annotation));
            } else {
                result.addAll(findAnnotations(annotation.annotationType().getAnnotations(), visited));
            }
        }
        return result;
    }
}
