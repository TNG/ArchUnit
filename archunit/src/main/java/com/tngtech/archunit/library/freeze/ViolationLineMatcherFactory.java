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
package com.tngtech.archunit.library.freeze;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;

import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;
import static java.lang.Character.isDigit;

class ViolationLineMatcherFactory {
    private static final String FREEZE_LINE_MATCHER_PROPERTY = "freeze.lineMatcher";
    private static final ViolationLineMatcher DEFAULT_MATCHER = new FuzzyViolationLineMatcher();

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

    /**
     * ignores numbers that are potentially line numbers (digits following a ':' and preceding a ')')
     * or compiler-generated numbers of anonymous classes or lambda expressions (digits following a '$').
     */
    private static class FuzzyViolationLineMatcher implements ViolationLineMatcher {
        @Override
        public boolean matches(String str1, String str2) {
            // Compare relevant substrings, in a more performant way than a regex solution like this:
            //
            // normalize = str -> str.replaceAll(":\\d+\\)", ":0)").replaceAll("\\$\\d+", "\\$0");
            // return normalize.apply(str1).equals(normalize.apply(str2));

            RelevantPartIterator relevantPart1 = new RelevantPartIterator(str1);
            RelevantPartIterator relevantPart2 = new RelevantPartIterator(str2);
            while (relevantPart1.hasNext() && relevantPart2.hasNext()) {
                if (!relevantPart1.next().equals(relevantPart2.next())) {
                    return false;
                }
            }
            return !relevantPart1.hasNext() && !relevantPart2.hasNext();
        }

        static class RelevantPartIterator {
            private final String str;
            private final int length;
            // (start, end) is the (first, last) index of current relevant part, moved through string during iteration:
            private int start = 0;
            private int end = -1;

            RelevantPartIterator(String str) {
                this.str = str;
                this.length = str.length();
            }

            boolean hasNext() {
                if (start >= length) {
                    return false;
                }
                if (end >= 0) {
                    start = findStartIndexOfNextRelevantPart();
                }
                return start < length;
            }

            public String next() {
                end = Math.min(nextIndexOfCharacterOrEndOfString(':'), nextIndexOfCharacterOrEndOfString('$'));
                return str.substring(start, end + 1);
            }

            private int nextIndexOfCharacterOrEndOfString(char ch) {
                int i = str.indexOf(ch, start);
                return i >= 0 ? i : length - 1;
            }

            private int findStartIndexOfNextRelevantPart() {
                final int startOfIgnoredPart = end + 1;
                int indexOfNonDigit = findIndexOfNextNonDigitChar(startOfIgnoredPart);
                if (str.charAt(end) == ':') {
                    boolean foundNumber = indexOfNonDigit > startOfIgnoredPart;
                    boolean foundClosingParenthesisAfterNumber = foundNumber && indexOfNonDigit < length && str.charAt(indexOfNonDigit) == ')';
                    return foundClosingParenthesisAfterNumber ? indexOfNonDigit + 1 : startOfIgnoredPart;
                } else {  // str.charAt(end) == '$'
                    return indexOfNonDigit;
                }
            }

            private int findIndexOfNextNonDigitChar(int index) {
                while (index < length && isDigit(str.charAt(index))) {
                    index++;
                }
                return index;
            }
        }
    }
}
