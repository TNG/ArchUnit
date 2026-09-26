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

import java.lang.reflect.Member;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMemberBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

/**
 * Mirrors {@link java.lang.reflect.Member}.
 */
@PublicAPI(usage = ACCESS)
public abstract class JavaMember extends JavaBaseMember implements HasModifiers {
    private final Set<JavaModifier> modifiers;

    JavaMember(JavaMemberBuilder<?, ?> builder) {
        super(builder);
        this.modifiers = checkNotNull(builder.getModifiers());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    /**
     * Resolves the respective {@link Member} from the classpath.<br>
     * NOTE: This method will throw an exception, if the owning {@link Class} or any of its dependencies
     * can't be found on the classpath.
     *
     * @return The {@link Member} equivalent to this {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    public abstract Member reflect();

    /**
     * Predefined {@link DescribedPredicate predicates} targeting {@link JavaMember}.
     * Note that due to inheritance, further predicates for {@link JavaMember} can be found in the following locations:
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
