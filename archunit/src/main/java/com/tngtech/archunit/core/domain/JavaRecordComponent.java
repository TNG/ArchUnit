/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

/**
 * Note: Does not extend {@link com.tngtech.archunit.core.domain.JavaMember}
 * because {@link java.lang.reflect.RecordComponent} does not implement {@link java.lang.reflect.Member}.
 */
@PublicAPI(usage = ACCESS)
public final class JavaRecordComponent extends JavaBaseMember implements HasType, HasAnnotations<JavaRecordComponent> {
    private final JavaType type;
    private Map<String, JavaAnnotation<JavaRecordComponent>> annotations = Collections.emptyMap();

    JavaRecordComponent(DomainBuilders.JavaRecordComponentBuilder builder) {
        super(builder);
        type = builder.getType(this);
    }

    void completeAnnotations(ImportContext context) {
        annotations = context.createAnnotations(this);
    }

    /**
     * @return The full name of this {@link JavaRecordComponent}, i.e. a string containing {@code ${declaringClass}.${name}}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getFullName() {
        return getOwner().getName() + "." + getName();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaType getType() {
        return type;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return type.toErasure();
    }

    /**
     * @return All raw types involved in this field's signature, which is equivalent to {@link #getType()}.{@link JavaType#getAllInvolvedRawTypes() getAllInvolvedRawTypes()}.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllInvolvedRawTypes() {
        return getType().getAllInvolvedRawTypes();
    }

    // TODO: Instead of delegation, would introducing an API like `getUnderlyingField()` be reasonable?

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getAccessesToSelf() {
        // We delegate to the accesses of the field underlying this record component. The names are identical.
        return getOwner().getField(getName()).getAccessesToSelf();
    }

    // FIXME: Refactor annotations API implementation after #1730

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<JavaRecordComponent>> getAnnotations() {
        return ImmutableSet.copyOf(annotations.values());
    }

    /**
     * @return The {@link Annotation} of this record component of the given {@link Annotation} type.
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaAnnotation<JavaRecordComponent> getAnnotationOfType(String typeName) {
        Optional<JavaAnnotation<JavaRecordComponent>> annotation = tryGetAnnotationOfType(typeName);
        if (!annotation.isPresent()) {
            throw new IllegalArgumentException(String.format("Member %s is not annotated with @%s", getFullName(), typeName));
        }
        return annotation.get();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).map(toAnnotationOfType(type));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Optional<JavaAnnotation<JavaRecordComponent>> tryGetAnnotationOfType(String typeName) {
        return Optional.ofNullable(annotations.get(typeName));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(Class<? extends Annotation> type) {
        return isAnnotatedWith(type.getName());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(String typeName) {
        return annotations.containsKey(typeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.values(), predicate);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> type) {
        return isMetaAnnotatedWith(type.getName());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(String typeName) {
        return isMetaAnnotatedWith(GET_RAW_TYPE.then(GET_NAME).is(equalTo(typeName)));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return CanBeAnnotated.Utils.isMetaAnnotatedWith(annotations.values(), predicate);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "RecordComponent <" + getFullName() + ">";
    }

    /**
     * Mirrors {@link java.lang.reflect.RecordComponent#getAccessor()}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaMethod getAccessor() {
        // The accessor is an automatically generated getter with the same name as the record component.
        return getOwner().getMethod(getName());
    }
}
