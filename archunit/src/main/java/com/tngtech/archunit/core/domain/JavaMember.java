/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasDescriptor;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMemberBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

public abstract class JavaMember implements
        HasName.AndFullName, HasDescriptor, HasAnnotations, HasModifiers, HasOwner<JavaClass>, HasDescription {
    private final String name;
    private final String descriptor;
    private final Supplier<Map<String, JavaAnnotation>> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;

    JavaMember(JavaMemberBuilder<?, ?> builder) {
        this.name = checkNotNull(builder.getName());
        this.descriptor = checkNotNull(builder.getDescriptor());
        this.annotations = builder.getAnnotations();
        this.owner = checkNotNull(builder.getOwner());
        this.modifiers = checkNotNull(builder.getModifiers());
    }

    @Override
    public Set<JavaAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations.get().values());
    }

    /**
     * Returns the {@link Annotation} of this member of the given {@link Annotation} type.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    @Override
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    public JavaAnnotation getAnnotationOfType(String typeName) {
        return tryGetAnnotationOfType(typeName).getOrThrow(new IllegalArgumentException(String.format(
                "Member %s is not annotated with @%s",
                getFullName(), Formatters.ensureSimpleName(typeName))));
    }

    @Override
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).transform(toAnnotationOfType(type));
    }

    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName) {
        return Optional.fromNullable(annotations.get().get(typeName));
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> type) {
        return isAnnotatedWith(type.getName());
    }

    @Override
    public boolean isAnnotatedWith(String typeName) {
        return annotations.get().containsKey(typeName);
    }

    @Override
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.get().values(), predicate);
    }

    @Override
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> type) {
        return isMetaAnnotatedWith(type.getName());
    }

    @Override
    public boolean isMetaAnnotatedWith(String typeName) {
        for (JavaAnnotation annotation : annotations.get().values()) {
            if (annotation.getRawType().isAnnotatedWith(typeName) || annotation.getRawType().isMetaAnnotatedWith(typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return CanBeAnnotated.Utils.isMetaAnnotatedWith(annotations.get().values(), predicate);
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getFullName() + '}';
    }

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
                    .forSubType();
        }
    }
}
