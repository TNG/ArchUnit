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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders;

import java.lang.reflect.RecordComponent;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Note: Does not extend {@link com.tngtech.archunit.core.domain.JavaMember}
 * because {@link java.lang.reflect.RecordComponent} does not implement {@link java.lang.reflect.Member}.
 */
@PublicAPI(usage = ACCESS)
public final class JavaRecordComponent extends JavaBaseMember implements HasType {
    private final JavaType type;

    JavaRecordComponent(DomainBuilders.JavaRecordComponentBuilder builder) {
        super(builder);
        type = builder.getType(this);
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

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getAccessesToSelf() {
        // We delegate to the accesses of the field underlying this record component. The names are identical.
        return getOwner().getField(getName()).getAccessesToSelf();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public Set<JavaAnnotation<JavaRecordComponent>> getAnnotations() {
        return (Set<JavaAnnotation<JavaRecordComponent>>) super.getAnnotations();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public JavaAnnotation<JavaRecordComponent> getAnnotationOfType(String typeName) {
        return (JavaAnnotation<JavaRecordComponent>) super.getAnnotationOfType(typeName);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public Optional<JavaAnnotation<JavaRecordComponent>> tryGetAnnotationOfType(String typeName) {
        return (Optional<JavaAnnotation<JavaRecordComponent>>) super.tryGetAnnotationOfType(typeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "RecordComponent <" + getFullName() + ">";
    }

    /**
     * Mirrors {@link RecordComponent#getAccessor()}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaMethod getAccessor() {
        // The accessor is an automatically generated getter with the same name as the record component.
        return getOwner().getMethod(getName());
    }
}
