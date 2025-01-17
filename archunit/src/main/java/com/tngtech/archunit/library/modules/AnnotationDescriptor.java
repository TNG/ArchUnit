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
package com.tngtech.archunit.library.modules;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * An {@link ArchModule.Descriptor} that carries along a specific {@link Annotation}.
 * @param <A> The type of {@link Annotation} this {@link ArchModule.Descriptor} contains
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class AnnotationDescriptor<A extends Annotation> implements ArchModule.Descriptor {
    private final String name;
    private final A annotation;

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public AnnotationDescriptor(String moduleName, A annotation) {
        this.name = checkNotNull(moduleName);
        this.annotation = checkNotNull(annotation);
    }

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public String getName() {
        return name;
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public A getAnnotation() {
        return annotation;
    }
}
