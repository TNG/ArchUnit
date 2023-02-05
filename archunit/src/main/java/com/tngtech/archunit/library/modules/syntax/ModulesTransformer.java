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

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules;

import static java.util.stream.Collectors.toList;

class ModulesTransformer<D extends ArchModule.Descriptor> implements ClassesTransformer<ArchModule<D>> {
    private final Function<JavaClasses, ArchModules<D>> transformFunction;
    private final PredicateAggregator<ArchModule<D>> predicateAggregator;
    private final Optional<String> description;

    ModulesTransformer(Function<JavaClasses, ArchModules<D>> transformFunction) {
        this(transformFunction, new PredicateAggregator<>(), Optional.empty());
    }

    private ModulesTransformer(
            Function<JavaClasses, ArchModules<D>> transformFunction,
            PredicateAggregator<ArchModule<D>> predicateAggregator,
            Optional<String> description
    ) {
        this.transformFunction = transformFunction;
        this.predicateAggregator = predicateAggregator;
        this.description = description;
    }

    @Override
    public DescribedIterable<ArchModule<D>> transform(JavaClasses classes) {
        Collection<ArchModule<D>> modules = transformFunction.apply(classes).stream().filter(predicateAggregator).collect(toList());
        return DescribedIterable.From.iterable(modules, getDescription());
    }

    @Override
    public ModulesTransformer<D> that(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new ModulesTransformer<>(transformFunction, predicateAggregator.add(predicate), description);
    }

    ModulesTransformer<D> and(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new ModulesTransformer<>(transformFunction, predicateAggregator.thatANDs().add(predicate), description);
    }

    ModulesTransformer<D> or(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new ModulesTransformer<>(transformFunction, predicateAggregator.thatORs().add(predicate), description);
    }

    @Override
    public ModulesTransformer<D> as(String description) {
        return new ModulesTransformer<>(transformFunction, predicateAggregator, Optional.of(description));
    }

    @Override
    public String getDescription() {
        return description.orElse("modules") + predicateAggregator.map(it -> " that " + it.getDescription()).orElse("");
    }
}
