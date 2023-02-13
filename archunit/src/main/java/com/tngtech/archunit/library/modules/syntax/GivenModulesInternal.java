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
package com.tngtech.archunit.library.modules.syntax;

import java.util.function.Function;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;

class GivenModulesInternal<DESCRIPTOR extends ArchModule.Descriptor> implements GivenModules<DESCRIPTOR>, GivenModulesConjunction<DESCRIPTOR> {
    private final ModulesTransformer<DESCRIPTOR> transformer;

    GivenModulesInternal(Function<JavaClasses, ArchModules<DESCRIPTOR>> createModules) {
        this(new ModulesTransformer<>(createModules));
    }

    private GivenModulesInternal(ModulesTransformer<DESCRIPTOR> transformer) {
        this.transformer = checkNotNull(transformer);
    }

    @Override
    public ArchRule should(ArchCondition<? super ArchModule<DESCRIPTOR>> condition) {
        return all(transformer).should(condition);
    }

    @Override
    public ModulesShould<DESCRIPTOR> should() {
        return new ModulesShouldInternal<>(this::should);
    }

    @Override
    public GivenModulesInternal<DESCRIPTOR> and(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return new GivenModulesInternal<>(transformer.and(predicate));
    }

    @Override
    public GivenModulesInternal<DESCRIPTOR> or(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return new GivenModulesInternal<>(transformer.or(predicate));
    }

    @Override
    public GivenModulesInternal<DESCRIPTOR> that(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return new GivenModulesInternal<>(transformer.that(predicate));
    }

    @Override
    public GivenModules<DESCRIPTOR> as(String description, Object... args) {
        return new GivenModulesInternal<>(transformer.as(String.format(description, args)));
    }
}
