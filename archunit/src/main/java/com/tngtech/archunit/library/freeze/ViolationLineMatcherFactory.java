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
                relevantPart1.findEnd();
                relevantPart2.findEnd();
                if (!relevantPart1.relevantSubstringMatches(relevantPart2)) {
                    return false;
                }
                relevantPart1.skipIgnoredPart();
                relevantPart2.skipIgnoredPart();
            }
            return !relevantPart1.hasNext() && !relevantPart2.hasNext();
        }

        static class RelevantPartIterator {
            private final String str;
            private final int length;
            // (start, end) is the (first, last) index of current relevant part, moved through string during iteration:
            private int start = 0;
            private int end;

            RelevantPartIterator(String str) {
                this.str = str;
                this.length = str.length();
            }

            boolean hasNext() {
                return start < length;
            }

            void findEnd() {
                end = Math.min(nextIndexOfCharacterOrEndOfString(':'), nextIndexOfCharacterOrEndOfString('$'));
            }

            private int nextIndexOfCharacterOrEndOfString(char ch) {
                int i = str.indexOf(ch, start);
                return i < 0 ? length - 1 : i;
            }

            boolean relevantSubstringMatches(RelevantPartIterator other) {
                return this.end - this.start == other.end - other.start
                        && this.getRelevantSubstring().equals(other.getRelevantSubstring());
            }

            private String getRelevantSubstring() {
                return str.substring(start, end + 1);
            }

            void skipIgnoredPart() {
                int i = start = end + 1;
                while (i < length && isDigit(str.charAt(i))) {
                    i++;
                }
                if (str.charAt(end) == ':') {
                    boolean foundNumber = i > start;
                    boolean foundClosingParenthesisAfterNumber = foundNumber && i < length && str.charAt(i) == ')';
                    start = foundClosingParenthesisAfterNumber ? i + 1 : start;
                } else {  // str.charAt(end) == '$'
                    start = i;
                }
            }
        }
    }
}
