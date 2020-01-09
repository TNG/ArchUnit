/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InconsistentClassPathException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class JavaField extends JavaMember implements HasType {
    private final JavaClass type;
    private final Supplier<Field> fieldSupplier;
    private Supplier<Set<JavaFieldAccess>> accessesToSelf = Suppliers.ofInstance(Collections.<JavaFieldAccess>emptySet());

    JavaField(DomainBuilders.JavaFieldBuilder builder) {
        super(builder);
        type = builder.getType();
        fieldSupplier = Suppliers.memoize(new ReflectFieldSupplier());
    }

    /**
     * @return The full name of this {@link JavaField}, i.e. a string containing {@code ${declaringClass}.${name}}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getFullName() {
        return getOwner().getName() + "." + getName();
    }

    /**
     * @deprecated Use {@link #getRawType()} instead
     */
    @Override
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public JavaClass getType() {
        return getRawType();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return type;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getAccessesToSelf() {
        return accessesToSelf.get();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public Set<JavaAnnotation<JavaField>> getAnnotations() {
        return (Set<JavaAnnotation<JavaField>>) super.getAnnotations();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public JavaAnnotation<JavaField> getAnnotationOfType(String typeName) {
        return (JavaAnnotation<JavaField>) super.getAnnotationOfType(typeName);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this field
    public Optional<JavaAnnotation<JavaField>> tryGetAnnotationOfType(String typeName) {
        return (Optional<JavaAnnotation<JavaField>>) super.tryGetAnnotationOfType(typeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Field reflect() {
        return fieldSupplier.get();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Field <" + getFullName() + ">";
    }

    void registerAccessesToField(Supplier<Set<JavaFieldAccess>> accesses) {
        this.accessesToSelf = checkNotNull(accesses);
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    private class ReflectFieldSupplier implements Supplier<Field> {
        @Override
        public Field get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredField(getName());
            } catch (NoSuchFieldException e) {
                throw new InconsistentClassPathException(
                        String.format("Can't resolve field %s.%s", reflectedOwner.getName(), getName()), e);
            }
        }
    }
}
