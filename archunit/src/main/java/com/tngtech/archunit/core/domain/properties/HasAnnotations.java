/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasAnnotations<SELF extends HasAnnotations<SELF>> extends CanBeAnnotated, HasDescription {
    @PublicAPI(usage = ACCESS)
    Set<? extends JavaAnnotation<? extends SELF>> getAnnotations();

    /**
     * @param type The {@link Class} of the {@link Annotation} to retrieve.
     * @return The {@link Annotation} of the given type.
     *         Will throw an {@link IllegalArgumentException} if no matching {@link Annotation} is present.
     * @param <A> The type of the {@link Annotation} to retrieve
     * @see #tryGetAnnotationOfType(Class)
     * @see #getAnnotationOfType(String)
     */
    @PublicAPI(usage = ACCESS)
    <A extends Annotation> A getAnnotationOfType(Class<A> type);

    /**
     * @param typeName The fully qualified class name of the {@link Annotation} type to retrieve.
     * @return The {@link JavaAnnotation} matching the given type.
     *         Will throw an {@link IllegalArgumentException} if no matching {@link Annotation} is present.
     * @see #tryGetAnnotationOfType(String)
     * @see #getAnnotationOfType(Class)
     */
    @PublicAPI(usage = ACCESS)
    JavaAnnotation<? extends SELF> getAnnotationOfType(String typeName);

    /**
     * @param type The {@link Class} of the {@link Annotation} to retrieve.
     * @return The {@link Annotation} of the given type or {@link Optional#empty()}
     *         if there is no {@link Annotation} with the respective annotation type.
     * @param <A> The type of the {@link Annotation} to retrieve
     * @see #getAnnotationOfType(Class)
     * @see #tryGetAnnotationOfType(String)
     */
    @PublicAPI(usage = ACCESS)
    <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type);

    /**
     * @param typeName The fully qualified class name of the {@link Annotation} type to retrieve.
     * @return The {@link JavaAnnotation} matching the given type or {@link Optional#empty()}
     *         if there is no {@link Annotation} with the respective annotation type.
     * @see #getAnnotationOfType(String)
     * @see #tryGetAnnotationOfType(Class)
     */
    @PublicAPI(usage = ACCESS)
    Optional<? extends JavaAnnotation<? extends SELF>> tryGetAnnotationOfType(String typeName);
}
