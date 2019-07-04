/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.freeze;

import java.util.Iterator;

import com.google.common.base.Splitter;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;

import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;
import static java.lang.Character.isDigit;

class ViolationLineMatcherFactory {
    private static final String FREEZE_LINE_MATCHER_PROPERTY = "freeze.lineMatcher";
    private static final FuzzyLineNumberMatcher DEFAULT_MATCHER = new FuzzyLineNumberMatcher();

    static ViolationLineMatcher create() {
        return ArchConfiguration.get().containsProperty(FREEZE_LINE_MATCHER_PROPERTY)
                ? createInstance(ArchConfiguration.get().getProperty(FREEZE_LINE_MATCHER_PROPERTY))
                : DEFAULT_MATCHER;
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static ViolationLineMatcher createInstance(String lineMatcherClassName) {
        try {
            return (ViolationLineMatcher) newInstanceOf(Class.forName(lineMatcherClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    ViolationLineMatcher.class.getSimpleName(), FREEZE_LINE_MATCHER_PROPERTY, lineMatcherClassName);
            throw new ViolationLineMatcherInitializationFailedException(message, e);
        }
    }

    private static class FuzzyLineNumberMatcher implements ViolationLineMatcher {
        @Override
        public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
            return ignorePotentialLineNumbers(lineFromFirstViolation).equals(ignorePotentialLineNumbers(lineFromSecondViolation));
        }

        // Would be nicer to use a regex here, but unfortunately that would have a massive performance impact.
        // So we do some low level character processing instead.
        private String ignorePotentialLineNumbers(String violation) {
            Iterator<String> parts = Splitter.on(":").split(violation).iterator();
            StringBuilder removedLineNumbers = new StringBuilder(parts.next());
            while (parts.hasNext()) {
                removedLineNumbers.append(removeNumberIfPresent(parts.next()));
            }
            return removedLineNumbers.toString();
        }

        private String removeNumberIfPresent(String part) {
            int i = 0;
            while (part.length() > i && isDigit(part.charAt(i))) {
                i++;
            }
            boolean foundClosingBracketFollowingDigits = i > 0 && part.length() > i && part.charAt(i) == ')';
            return foundClosingBracketFollowingDigits
                    ? part.substring(i)
                    : part;
        }
    }
}
