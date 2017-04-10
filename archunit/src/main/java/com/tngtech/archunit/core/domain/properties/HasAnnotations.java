package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;

public interface HasAnnotations extends CanBeAnnotated {
    Set<JavaAnnotation> getAnnotations();

    <A extends Annotation> A getAnnotationOfType(Class<A> type);

    JavaAnnotation getAnnotationOfType(String typeName);

    <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type);

    Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName);
}
