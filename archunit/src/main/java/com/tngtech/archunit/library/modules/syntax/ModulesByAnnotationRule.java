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
package com.tngtech.archunit.library.modules.syntax;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface ModulesByAnnotationRule<ANNOTATION extends Annotation> extends ModulesRule<AnnotationDescriptor<ANNOTATION>> {

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationShould<ANNOTATION> andShould();

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> as(String newDescription);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> because(String reason);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> allowEmptyShould(boolean allowEmptyShould);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Class<?> origin, Class<?> target);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Predicate<? super JavaClass> originPredicate, Predicate<? super JavaClass> targetPredicate);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationRule<ANNOTATION> ignoreDependency(Predicate<? super Dependency> dependencyPredicate);
}
