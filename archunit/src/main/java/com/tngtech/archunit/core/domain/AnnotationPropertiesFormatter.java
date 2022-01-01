/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.base.Function.Functions.identity;

class AnnotationPropertiesFormatter {
    private final Function<List<String>, String> arrayFormatter;
    private final Function<Class<?>, String> typeFormatter;
    private final Function<String, String> stringFormatter;
    private final boolean omitOptionalIdentifierForSingleElementAnnotations;

    private AnnotationPropertiesFormatter(Builder builder) {
        this.arrayFormatter = checkNotNull(builder.arrayFormatter);
        this.typeFormatter = checkNotNull(builder.typeFormatter);
        this.stringFormatter = checkNotNull(builder.stringFormatter);
        this.omitOptionalIdentifierForSingleElementAnnotations = builder.omitOptionalIdentifierForSingleElementAnnotations;
    }

    String formatProperties(Map<String, Object> properties) {
        // see Builder#omitOptionalIdentifierForSingleElementAnnotations() for documentation
        if (properties.size() == 1 && properties.containsKey("value") && omitOptionalIdentifierForSingleElementAnnotations) {
            return formatValue(properties.get("value"));
        }

        Set<String> formattedProperties = new HashSet<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            formattedProperties.add(entry.getKey() + "=" + formatValue(entry.getValue()));
        }
        return Joiner.on(", ").join(formattedProperties);
    }

    String formatValue(Object input) {
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
            elemToString.add(formatValue(Array.get(input, i)));
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
        private boolean omitOptionalIdentifierForSingleElementAnnotations = false;

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

        /**
         * Configures that the identifier is omitted if the annotation is a
         * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-9.html#jls-9.7.3">single-element annotation</a>
         * and the identifier of the only element is "value".
         *
         * <ul><li>Example with this configuration: {@code @Copyright("2020 Acme Corporation")}</li>
         * <li>Example without this configuration: {@code @Copyright(value="2020 Acme Corporation")}</li></ul>
         */
        Builder omitOptionalIdentifierForSingleElementAnnotations() {
            omitOptionalIdentifierForSingleElementAnnotations = true;
            return this;
        }

        AnnotationPropertiesFormatter build() {
            return new AnnotationPropertiesFormatter(this);
        }
    }
}
