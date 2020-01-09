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

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InconsistentClassPathException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;

public final class JavaConstructor extends JavaCodeUnit {
    private final Supplier<Constructor<?>> constructorSupplier;
    private final ThrowsClause<JavaConstructor> throwsClause;
    private Set<JavaConstructorCall> callsToSelf = Collections.emptySet();

    @PublicAPI(usage = ACCESS)
    public static final String CONSTRUCTOR_NAME = "<init>";

    JavaConstructor(JavaConstructorBuilder builder) {
        super(builder);
        throwsClause = builder.getThrowsClause(this);
        constructorSupplier = Suppliers.memoize(new ReflectConstructorSupplier());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ThrowsClause<JavaConstructor> getThrowsClause() {
        return throwsClause;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isConstructor() {
        return true;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getAccessesToSelf() {
        return callsToSelf;
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this constructor
    public Set<JavaAnnotation<JavaConstructor>> getAnnotations() {
        return (Set<JavaAnnotation<JavaConstructor>>) super.getAnnotations();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this constructor
    public JavaAnnotation<JavaConstructor> getAnnotationOfType(String typeName) {
        return (JavaAnnotation<JavaConstructor>) super.getAnnotationOfType(typeName);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this constructor
    public Optional<JavaAnnotation<JavaConstructor>> tryGetAnnotationOfType(String typeName) {
        return (Optional<JavaAnnotation<JavaConstructor>>) super.tryGetAnnotationOfType(typeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Constructor<?> reflect() {
        return constructorSupplier.get();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Constructor <" + getFullName() + ">";
    }

    void registerCallsToConstructor(Collection<JavaConstructorCall> calls) {
        this.callsToSelf = ImmutableSet.copyOf(calls);
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    private class ReflectConstructorSupplier implements Supplier<Constructor<?>> {
        @Override
        public Constructor<?> get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredConstructor(reflect(getRawParameterTypes()));
            } catch (NoSuchMethodException e) {
                throw new InconsistentClassPathException(
                        "Can't resolve constructor " + formatMethod(reflectedOwner.getName(), getName(), getRawParameterTypes()), e);
            }
        }
    }
}
