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
package com.tngtech.archunit.library.modules.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface GivenModulesByAnnotationConjunction<ANNOTATION extends Annotation> extends GivenModulesConjunction<AnnotationDescriptor<ANNOTATION>> {

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesByAnnotationConjunction<ANNOTATION> and(DescribedPredicate<? super ArchModule<AnnotationDescriptor<ANNOTATION>>> predicate);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesByAnnotationConjunction<ANNOTATION> or(DescribedPredicate<? super ArchModule<AnnotationDescriptor<ANNOTATION>>> predicate);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesByAnnotationShould<ANNOTATION> should();

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesByAnnotationConjunction<ANNOTATION> as(String description, Object... args);
}
