package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasAnnotations extends CanBeAnnotated {
    @PublicAPI(usage = ACCESS)
    Set<JavaAnnotation> getAnnotations();

    @PublicAPI(usage = ACCESS)
    <A extends Annotation> A getAnnotationOfType(Class<A> type);

    @PublicAPI(usage = ACCESS)
    JavaAnnotation getAnnotationOfType(String typeName);

    @PublicAPI(usage = ACCESS)
    <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type);

    @PublicAPI(usage = ACCESS)
    Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName);
}
