/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasDescriptor;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMemberBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

@PublicAPI(usage = ACCESS)
public abstract class JavaMember implements
        HasName.AndFullName, HasDescriptor, HasAnnotations<JavaMember>, HasModifiers, HasOwner<JavaClass>, HasSourceCodeLocation {

    private final String name;
    private final String descriptor;
    private Map<String, JavaAnnotation<JavaMember>> annotations = Collections.emptyMap();
    private final JavaClass owner;
    private final SourceCodeLocation sourceCodeLocation;
    private final Set<JavaModifier> modifiers;
    private ReverseDependencies reverseDependencies = ReverseDependencies.EMPTY;

    JavaMember(JavaMemberBuilder<?, ?> builder) {
        this.name = checkNotNull(builder.getName());
        this.descriptor = checkNotNull(builder.getDescriptor());
        this.owner = checkNotNull(builder.getOwner());
        this.sourceCodeLocation = SourceCodeLocation.of(owner, builder.getFirstLineNumber());
        this.modifiers = checkNotNull(builder.getModifiers());
    }

    /**
     * Similar to {@link JavaType#getAllInvolvedRawTypes()}, this method returns all raw types involved in this {@link JavaMember member's} signature.
     * For more concrete details refer to {@link JavaField#getAllInvolvedRawTypes()} and {@link JavaCodeUnit#getAllInvolvedRawTypes()}.
     *
     * @return All raw types involved in the signature of this member
     */
    @PublicAPI(usage = ACCESS)
    public abstract Set<JavaClass> getAllInvolvedRawTypes();

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<? extends JavaAnnotation<? extends JavaMember>> getAnnotations() {
        return ImmutableSet.copyOf(annotations.values());
    }

    /**
     * Returns the {@link Annotation} of this member of the given {@link Annotation} type.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaAnnotation<? extends JavaMember> getAnnotationOfType(String typeName) {
        Optional<? extends JavaAnnotation<? extends JavaMember>> annotation = tryGetAnnotationOfType(typeName);
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
    public Optional<? extends JavaAnnotation<? extends JavaMember>> tryGetAnnotationOfType(String typeName) {
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
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    @Override
    @Internal
    public String getDescriptor() {
        return descriptor;
    }

    @PublicAPI(usage = ACCESS)
    public abstract Set<? extends JavaAccess<?>> getAccessesToSelf();

    /**
     * Resolves the respective {@link Member} from the classpath.<br>
     * NOTE: This method will throw an exception, if the owning {@link Class} or any of its dependencies
     * can't be found on the classpath.
     *
     * @return The {@link Member} equivalent to this {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    public abstract Member reflect();

    void completeAnnotations(ImportContext context) {
        annotations = context.createAnnotations(this);
    }

    protected ReverseDependencies getReverseDependencies() {
        return reverseDependencies;
    }

    void setReverseDependencies(ReverseDependencies reverseDependencies) {
        this.reverseDependencies = reverseDependencies;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getFullName() + '}';
    }

    /**
     * Predefined {@link DescribedPredicate predicates} targeting {@link JavaMember}.
     * Note that due to inheritance further predicates for {@link JavaMember} can be found in the following locations:
     * <ul>
     *     <li>{@link HasName.Predicates}</li>
     *     <li>{@link HasName.AndFullName.Predicates}</li>
     *     <li>{@link HasModifiers.Predicates}</li>
     *     <li>{@link CanBeAnnotated.Predicates}</li>
     *     <li>{@link HasOwner.Predicates}</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaMember> declaredIn(Class<?> clazz) {
            return declaredIn(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaMember> declaredIn(String className) {
            return declaredIn(GET_NAME.is(equalTo(className)).as(className));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaMember> declaredIn(DescribedPredicate<? super JavaClass> predicate) {
            return Get.<JavaClass>owner().is(predicate)
                    .as("declared in %s", predicate.getDescription())
                    .forSubtype();
        }
    }
}
