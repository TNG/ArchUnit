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
package com.tngtech.archunit.base;

import java.util.function.Function;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public abstract class DescribedFunction<F, T> implements Function<F, T>, HasDescription {
    private final String description;

    protected DescribedFunction(String description, Object... args) {
        this.description = String.format(description, args);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("description", description)
                .toString();
    }

    @PublicAPI(usage = ACCESS)
    public static <F, T> DescribedFunction<F, T> describe(String description, final Function<F, T> function) {
        return new DescribedFunction<F, T>(description) {
            @Override
            public T apply(F input) {
                return function.apply(input);
            }
        };
    }
}
