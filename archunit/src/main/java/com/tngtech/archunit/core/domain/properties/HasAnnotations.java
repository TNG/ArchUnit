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
package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasAnnotations<SELF extends HasAnnotations<SELF>> extends CanBeAnnotated, HasDescription {
    @PublicAPI(usage = ACCESS)
    Set<? extends JavaAnnotation<? extends SELF>> getAnnotations();

    @PublicAPI(usage = ACCESS)
    <A extends Annotation> A getAnnotationOfType(Class<A> type);

    @PublicAPI(usage = ACCESS)
    JavaAnnotation<? extends SELF> getAnnotationOfType(String typeName);

    @PublicAPI(usage = ACCESS)
    <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type);

    @PublicAPI(usage = ACCESS)
    Optional<? extends JavaAnnotation<? extends SELF>> tryGetAnnotationOfType(String typeName);
}
