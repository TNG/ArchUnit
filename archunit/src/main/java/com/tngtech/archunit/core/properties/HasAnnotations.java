package com.tngtech.archunit.core.properties;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.JavaAnnotation;

public interface HasAnnotations extends CanBeAnnotated {
    Set<JavaAnnotation> getAnnotations();

    JavaAnnotation getAnnotationOfType(Class<? extends Annotation> type);

    JavaAnnotation getAnnotationOfType(String typeName);

    Optional<JavaAnnotation> tryGetAnnotationOfType(Class<? extends Annotation> type);

    Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName);
}
