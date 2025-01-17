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
import java.util.function.Function;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules;
import com.tngtech.archunit.library.modules.syntax.ModulesShouldInternal.ModulesByAnnotationShouldInternal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;

abstract class AbstractGivenModulesInternal<DESCRIPTOR extends ArchModule.Descriptor, SELF extends AbstractGivenModulesInternal<DESCRIPTOR, SELF>> implements GivenModules<DESCRIPTOR>, GivenModulesConjunction<DESCRIPTOR> {
    private final ModulesTransformer<DESCRIPTOR> transformer;

    private AbstractGivenModulesInternal(Function<JavaClasses, ArchModules<DESCRIPTOR>> createModules) {
        this(new ModulesTransformer<>(createModules));
    }

    private AbstractGivenModulesInternal(ModulesTransformer<DESCRIPTOR> transformer) {
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
    public SELF and(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return copy(transformer.and(predicate));
    }

    @Override
    public SELF or(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return copy(transformer.or(predicate));
    }

    @Override
    public SELF that(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
        return copy(transformer.that(predicate));
    }

    @Override
    public SELF as(String description, Object... args) {
        return copy(transformer.as(String.format(description, args)));
    }

    abstract SELF copy(ModulesTransformer<DESCRIPTOR> transformer);

    static class GivenModulesInternal<DESCRIPTOR extends ArchModule.Descriptor> extends AbstractGivenModulesInternal<DESCRIPTOR, GivenModulesInternal<DESCRIPTOR>> {
        GivenModulesInternal(Function<JavaClasses, ArchModules<DESCRIPTOR>> createModules) {
            super(createModules);
        }

        private GivenModulesInternal(ModulesTransformer<DESCRIPTOR> transformer) {
            super(transformer);
        }

        @Override
        GivenModulesInternal<DESCRIPTOR> copy(ModulesTransformer<DESCRIPTOR> transformer) {
            return new GivenModulesInternal<>(transformer);
        }
    }

    static class GivenModulesByAnnotationInternal<ANNOTATION extends Annotation>
            extends AbstractGivenModulesInternal<AnnotationDescriptor<ANNOTATION>, GivenModulesByAnnotationInternal<ANNOTATION>>
            implements GivenModulesByAnnotation<ANNOTATION>, GivenModulesByAnnotationConjunction<ANNOTATION> {

        GivenModulesByAnnotationInternal(Function<JavaClasses, ArchModules<AnnotationDescriptor<ANNOTATION>>> createModules) {
            super(createModules);
        }

        private GivenModulesByAnnotationInternal(ModulesTransformer<AnnotationDescriptor<ANNOTATION>> transformer) {
            super(transformer);
        }

        @Override
        public ModulesByAnnotationShould<ANNOTATION> should() {
            return new ModulesByAnnotationShouldInternal<>(this::should);
        }

        @Override
        GivenModulesByAnnotationInternal<ANNOTATION> copy(ModulesTransformer<AnnotationDescriptor<ANNOTATION>> transformer) {
            return new GivenModulesByAnnotationInternal<>(transformer);
        }
    }
}
