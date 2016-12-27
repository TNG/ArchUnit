package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface HasAnnotations {
    Set<JavaAnnotation> getAnnotations();

    boolean isAnnotatedWith(Class<? extends Annotation> annotation);

    JavaAnnotation getAnnotationOfType(Class<? extends Annotation> type);

    Optional<JavaAnnotation> tryGetAnnotationOfType(Class<? extends Annotation> type);
}
