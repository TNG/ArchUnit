/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InconsistentClassPathException;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

public class JavaMethod extends JavaCodeUnit {
    private final Supplier<Method> methodSupplier;
    private final ThrowsClause<JavaMethod> throwsClause;
    private final Optional<Object> annotationDefaultValue;

    JavaMethod(DomainBuilders.JavaMethodBuilder builder, Function<JavaMethod, Optional<Object>> createAnnotationDefaultValue) {
        super(builder);
        throwsClause = builder.getThrowsClause(this);
        methodSupplier = Suppliers.memoize(new ReflectMethodSupplier());
        annotationDefaultValue = createAnnotationDefaultValue.apply(this);
    }

    @Override
    @SuppressWarnings("unchecked") // Cast is safe, because OWNER always refers to this object
    public List<JavaTypeVariable<JavaMethod>> getTypeParameters() {
        return (List<JavaTypeVariable<JavaMethod>>) super.getTypeParameters();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ThrowsClause<JavaMethod> getThrowsClause() {
        return throwsClause;
    }

    /**
     * Returns the default value of this annotation method, if the method is an annotation method and has a
     * declared default. It's analogue to {@link Method#getDefaultValue()}, but returns Optional.absent()
     * instead of null.
     *
     * @return Optional.of(defaultValue) if applicable, otherwise Optional.absent()
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Object> getDefaultValue() {
        return annotationDefaultValue;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getAccessesToSelf() {
        return getReverseDependencies().getCallsTo(this);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this method
    public Set<JavaAnnotation<JavaMethod>> getAnnotations() {
        return (Set<JavaAnnotation<JavaMethod>>) super.getAnnotations();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this method
    public JavaAnnotation<JavaMethod> getAnnotationOfType(String typeName) {
        return (JavaAnnotation<JavaMethod>) super.getAnnotationOfType(typeName);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this method
    public Optional<JavaAnnotation<JavaMethod>> tryGetAnnotationOfType(String typeName) {
        return (Optional<JavaAnnotation<JavaMethod>>) super.tryGetAnnotationOfType(typeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Method reflect() {
        return methodSupplier.get();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Method <" + getFullName() + ">";
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    private class ReflectMethodSupplier implements Supplier<Method> {
        @Override
        public Method get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredMethod(getName(), reflect(getRawParameterTypes()));
            } catch (NoSuchMethodException e) {
                throw new InconsistentClassPathException(
                        "Can't resolve method " + formatMethod(reflectedOwner.getName(), getName(), namesOf(getRawParameterTypes())), e);
            }
        }
    }

}
