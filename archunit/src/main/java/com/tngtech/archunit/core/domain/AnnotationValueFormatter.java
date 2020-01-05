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
package com.tngtech.archunit.core.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.base.Function.Functions.identity;

class AnnotationValueFormatter implements Function<Object, String> {
    private final Function<List<String>, String> arrayFormatter;
    private final Function<Class<?>, String> typeFormatter;
    private final Function<String, String> stringFormatter;

    private AnnotationValueFormatter(Builder builder) {
        this.arrayFormatter = checkNotNull(builder.arrayFormatter);
        this.typeFormatter = checkNotNull(builder.typeFormatter);
        this.stringFormatter = checkNotNull(builder.stringFormatter);
    }

    @Override
    public String apply(Object input) {
        if (input instanceof Class<?>) {
            return typeFormatter.apply((Class<?>) input);
        }
        if (input instanceof String) {
            return stringFormatter.apply((String) input);
        }
        if (!input.getClass().isArray()) {
            return String.valueOf(input);
        }

        List<String> elemToString = new ArrayList<>();
        for (int i = 0; i < Array.getLength(input); i++) {
            elemToString.add("" + apply(Array.get(input, i)));
        }
        return arrayFormatter.apply(elemToString);
    }

    static Builder configure() {
        return new Builder();
    }

    static class Builder {
        private Function<List<String>, String> arrayFormatter;
        private Function<Class<?>, String> typeFormatter;
        private Function<String, String> stringFormatter = identity();

        Builder formattingArraysWithSquareBrackets() {
            arrayFormatter = new Function<List<String>, String>() {
                @Override
                public String apply(List<String> input) {
                    return "[" + Joiner.on(", ").join(input) + "]";
                }
            };
            return this;
        }

        Builder formattingArraysWithCurlyBrackets() {
            arrayFormatter = new Function<List<String>, String>() {
                @Override
                public String apply(List<String> input) {
                    return "{" + Joiner.on(", ").join(input) + "}";
                }
            };
            return this;
        }

        Builder formattingTypesToString() {
            typeFormatter = new Function<Class<?>, String>() {
                @Override
                public String apply(Class<?> input) {
                    return String.valueOf(input);
                }
            };
            return this;
        }

        Builder formattingTypesAsClassNames() {
            typeFormatter = new Function<Class<?>, String>() {
                @Override
                public String apply(Class<?> input) {
                    return input.getName() + ".class";
                }
            };
            return this;
        }

        Builder quotingStrings() {
            stringFormatter = new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return "\"" + input + "\"";
                }
            };
            return this;
        }

        AnnotationValueFormatter build() {
            return new AnnotationValueFormatter(this);
        }
    }
}
