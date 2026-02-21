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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InconsistentClassPathException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;
import com.tngtech.archunit.base.ResolvesTypesViaReflection;
import com.tngtech.archunit.base.Suppliers;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static java.util.stream.Collectors.toList;

@PublicAPI(usage = ACCESS)
public final class JavaMethod extends JavaCodeUnit {
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

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMethod() {
        return true;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getCallsOfSelf() {
        return getReverseDependencies().getCallsTo(this);
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodReference> getReferencesToSelf() {
        return getReverseDependencies().getReferencesTo(this);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitAccess<?>> getAccessesToSelf() {
        return union(getCallsOfSelf(), getReferencesToSelf());
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

    /**
     * Returns true, if this method overrides or implements a method from a supertype.
     * The annotation {@link Override} may or may not be present on this method.
     *
     * @see Override
     */
    @PublicAPI(usage = ACCESS)
    public boolean isOverriding() {
        List<JavaClass> supertypes = Stream.concat(
                getOwner().getAllRawSuperclasses().stream(),
                getOwner().getAllRawInterfaces().stream()
        ).collect(toList());

        String name = getName();
        String[] parameterTypes = getRawParameterTypes().stream().map(JavaClass::getFullName).toArray(String[]::new);
        for (JavaClass supertype : supertypes) {
            if (supertype.tryGetMethod(name, parameterTypes).isPresent()) {
                return true;
            }
        }

        return false;
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

    /**
     * Predefined {@link DescribedPredicate predicates} targeting {@link JavaMethod}.
     * Note that due to inheritance further predicates for {@link JavaMethod} can be found in the following locations:
     * <ul>
     *     <li>{@link JavaCodeUnit.Predicates}</li>
     *     <li>{@link JavaMember.Predicates}</li>
     *     <li>{@link HasName.Predicates}</li>
     *     <li>{@link HasName.AndFullName.Predicates}</li>
     *     <li>{@link HasModifiers.Predicates}</li>
     *     <li>{@link CanBeAnnotated.Predicates}</li>
     *     <li>{@link HasOwner.Predicates}</li>
     *     <li>{@link HasParameterTypes.Predicates}</li>
     *     <li>{@link HasReturnType.Predicates}</li>
     *     <li>{@link HasThrowsClause.Predicates}</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public static final class Predicates {
        private Predicates() {
        }

        /**
         * @see JavaMethod#isOverriding()
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaMethod> overriding() {
            return describe("overriding", JavaMethod::isOverriding);
        }
    }
}
