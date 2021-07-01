/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * JUnit's DisplayNameGenerator API isn't really suited for the way ArchUnit discovers tests (especially on fields),
 * so we just reuse the annotations, but provide our own implementation here (similar to JUnit's DisplayNameUtils).
 * <p>
 * For the time being, we just evaluate DisplayNameGenerator.ReplaceUnderscores and return the original name in
 * all other cases.
 */
final class DisplayNameResolver {

    static String determineDisplayName(String elementName, Class<?> testClass) {

        Optional<Class<? extends DisplayNameGenerator>> displayNameGenerator = displayNameGenerator(testClass);

        if (displayNameGenerator.isPresent()
                && (displayNameGenerator.get() == DisplayNameGenerator.ReplaceUnderscores.class)) {
            return underscoresReplacedBySpaces(elementName);
        }

        return elementName;
    }

    private static String underscoresReplacedBySpaces(String elementName) {
        return elementName.replace('_', ' ');
    }

    private static Optional<Class<? extends DisplayNameGenerator>> displayNameGenerator(Class<?> testClass) {
        return getDisplayNameGeneration(testClass)
                .map(DisplayNameGeneration::value);
    }

    /**
     * Copied from org.junit.jupiter.engine.descriptor.DisplayNameUtils to get exactly the same semantics.
     * Maybe a more general variant of this method should become part of JUnit's AnnotationUtils?
     */
    private static Optional<DisplayNameGeneration> getDisplayNameGeneration(Class<?> testClass) {
        Class<?> candidate = testClass;
        do {
            Optional<DisplayNameGeneration> generation = findAnnotation(candidate, DisplayNameGeneration.class);
            if (generation.isPresent()) {
                return generation;
            }
            candidate = candidate.getEnclosingClass();
        } while (candidate != null);
        return Optional.empty();
    }
}
