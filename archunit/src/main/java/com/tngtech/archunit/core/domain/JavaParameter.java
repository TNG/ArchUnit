package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

/**
 * A parameter of a {@link JavaCodeUnit}, i.e. encapsulates the raw parameter type, the (possibly) generic
 * parameter type and any annotations this parameter has.
 */
@PublicAPI(usage = ACCESS)
public final class JavaParameter implements HasType, HasOwner<JavaCodeUnit>, HasAnnotations<JavaParameter> {
    private static final ChainableFunction<HasType, String> GET_ANNOTATION_TYPE_NAME = GET_RAW_TYPE.then(GET_NAME);
    private static final Function<HasType, String> GUAVA_GET_ANNOTATION_TYPE_NAME = toGuava(GET_ANNOTATION_TYPE_NAME);

    private final JavaCodeUnit owner;
    private final int index;
    private final JavaType type;
    private final JavaClass rawType;
    private final Map<String, JavaAnnotation<JavaParameter>> annotations;

    JavaParameter(JavaCodeUnit owner, DomainBuilders.JavaCodeUnitBuilder.ParameterAnnotationsBuilder builder, int index, JavaType type) {
        this.owner = owner;
        this.index = index;
        this.type = type;
        this.rawType = type.toErasure();
        this.annotations = buildIndexedByTypeName(builder);
    }

    private Map<String, JavaAnnotation<JavaParameter>> buildIndexedByTypeName(DomainBuilders.JavaCodeUnitBuilder.ParameterAnnotationsBuilder builder) {
        Set<JavaAnnotation<JavaParameter>> annotations = builder.build(this);
        return Maps.uniqueIndex(annotations, GUAVA_GET_ANNOTATION_TYPE_NAME);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getOwner() {
        return owner;
    }

    @PublicAPI(usage = ACCESS)
    public int getIndex() {
        return index;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaType getType() {
        return type;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return rawType;
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationType) {
        return annotations.containsKey(annotationType.getName());
    }

    @Override
    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.containsKey(annotationTypeName);
    }

    @Override
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return anyElementThat(predicate).apply(annotations.values());
    }

    @Override
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(annotationType)));
    }

    @Override
    public boolean isMetaAnnotatedWith(String annotationTypeName) {
        return isMetaAnnotatedWith(GET_ANNOTATION_TYPE_NAME.is(equalTo(annotationTypeName)));
    }

    @Override
    public boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return Utils.isMetaAnnotatedWith(annotations.values(), predicate);
    }

    @Override
    public Set<JavaAnnotation<JavaParameter>> getAnnotations() {
        return ImmutableSet.copyOf(annotations.values());
    }

    @Override
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    public JavaAnnotation<JavaParameter> getAnnotationOfType(String typeName) {
        Optional<JavaAnnotation<JavaParameter>> annotation = tryGetAnnotationOfType(typeName);
        if (!annotation.isPresent()) {
            throw new IllegalArgumentException(String.format("%s is not annotated with @%s", getDescription(), typeName));
        }
        return annotation.get();
    }

    @Override
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).map(toAnnotationOfType(type));
    }

    @Override
    public Optional<JavaAnnotation<JavaParameter>> tryGetAnnotationOfType(String typeName) {
        return Optional.ofNullable(annotations.get(typeName));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Parameter <" + type.getName() + "> of " + startWithLowercase(owner.getDescription());
    }

    @Override
    public String toString() {
        return "JavaParameter{owner='" + owner.getFullName() + "', index='" + index + "', type='" + type.getName() + "'}";
    }

    static String startWithLowercase(String string) {
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }
}
