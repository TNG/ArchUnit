/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.plantuml.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

class PlantUmlPatterns {
    private static final String COMPONENT_NAME_GROUP_NAME = "componentName";
    private static final String COMPONENT_NAME_FORMAT = "\\[" + capture(anythingBut("\\[\\]"), COMPONENT_NAME_GROUP_NAME) + "]";

    private static final String STEREOTYPE_FORMAT = "(?:<<" + capture(anythingBut("<>")) + ">>\\s*)";
    private static final Pattern STEREOTYPE_PATTERN = Pattern.compile(STEREOTYPE_FORMAT);

    private static final String ALIAS_GROUP_NAME = "alias";
    private static final String ALIAS_FORMAT = "\\s*(?:as \"?" + capture("[^\" ]+", ALIAS_GROUP_NAME) + "\"?)?";

    private static final String COLOR_FORMAT = "\\s*(?:#" + anyOf("\\w|/\\\\-") + "+)?";

    private static final Pattern PLANTUML_COMPONENT_PATTERN = Pattern.compile(
            "^\\s*" + COMPONENT_NAME_FORMAT + "\\s*" + STEREOTYPE_FORMAT + "*" + ALIAS_FORMAT + COLOR_FORMAT + "\\s*");

    private static String capture(String pattern) {
        return "(" + pattern + ")";
    }

    private static String capture(String pattern, String name) {
        return "(?<" + name + ">" + pattern + ")";
    }

    @SuppressWarnings("SameParameterValue")
    private static String anyOf(String charsJoined) {
        return "[" + charsJoined + "]+";
    }

    private static String anythingBut(String charsJoined) {
        return "[^" + charsJoined + "]+";
    }

    Stream<String> filterComponents(List<String> lines) {
        return lines.stream().filter(matches(PLANTUML_COMPONENT_PATTERN));
    }

    PlantUmlComponentMatcher matchComponent(String input) {
        return new PlantUmlComponentMatcher(input);
    }

    private Predicate<String> matches(Pattern pattern) {
        return input -> pattern.matcher(input).matches();
    }

    Iterable<PlantUmlDependencyMatcher> matchDependencies(List<String> diagramLines) {
        List<PlantUmlDependencyMatcher> result = new ArrayList<>();
        for (String line : diagramLines) {
            result.addAll(PlantUmlDependencyMatcher.tryParseFromLeftToRight(line));
            result.addAll(PlantUmlDependencyMatcher.tryParseFromRightToLeft(line));
        }
        return result;
    }

    static class PlantUmlComponentMatcher {
        private final Matcher componentMatcher;
        private final Matcher stereotypeMatcher;

        PlantUmlComponentMatcher(String input) {
            componentMatcher = PLANTUML_COMPONENT_PATTERN.matcher(input);
            checkState(componentMatcher.matches(), "input %s does not match pattern %s", input, PLANTUML_COMPONENT_PATTERN);

            stereotypeMatcher = STEREOTYPE_PATTERN.matcher(input);
        }

        String matchComponentName() {
            return componentMatcher.group(COMPONENT_NAME_GROUP_NAME);
        }

        Set<String> matchStereoTypes() {
            Set<String> result = new HashSet<>();
            while (stereotypeMatcher.find()) {
                result.add(stereotypeMatcher.group(1));
            }
            return result;
        }

        Optional<String> matchAlias() {
            return Optional.ofNullable(componentMatcher.group(ALIAS_GROUP_NAME));
        }
    }

    static class PlantUmlDependencyMatcher {
        private static final String COLOR_REGEX = "\\[[^]]+]"; // for arrows like '--[#green]->'
        private static final String DEPENDENCY_ARROW_CENTER_REGEX = "(left|right|up|down|" + COLOR_REGEX + ")?";
        private static final Pattern DEPENDENCY_RIGHT_ARROW_PATTERN = Pattern.compile("\\s-+" + DEPENDENCY_ARROW_CENTER_REGEX + "-*>\\s");
        private static final Pattern DEPENDENCY_LEFT_ARROW_PATTERN = Pattern.compile("\\s<-*" + DEPENDENCY_ARROW_CENTER_REGEX + "-+\\s");

        private final String origin;
        private final String target;

        PlantUmlDependencyMatcher(String origin, String target) {
            this.origin = checkNotNull(origin, "Origin must not be null");
            this.target = checkNotNull(target, "Target must not be null");
        }

        String matchOrigin() {
            return origin;
        }

        String matchTarget() {
            return target;
        }

        static Collection<PlantUmlDependencyMatcher> tryParseFromLeftToRight(String line) {
            return isDependencyFromLeftToRight(line) ?
                    singletonList(parseDependencyFromLeftToRight(line)) :
                    emptyList();
        }

        private static boolean isDependencyFromLeftToRight(String line) {
            return DEPENDENCY_RIGHT_ARROW_PATTERN.matcher(line).find();
        }

        private static PlantUmlDependencyMatcher parseDependencyFromLeftToRight(String line) {
            List<String> parts = parseParts(line, DEPENDENCY_RIGHT_ARROW_PATTERN);
            return new PlantUmlDependencyMatcher(parts.get(0), parts.get(1));
        }

        static Collection<PlantUmlDependencyMatcher> tryParseFromRightToLeft(String line) {
            return isDependencyFromRightToLeft(line) ?
                    singletonList(parseDependencyFromRightToLeft(line)) :
                    emptyList();
        }

        private static boolean isDependencyFromRightToLeft(String line) {
            return DEPENDENCY_LEFT_ARROW_PATTERN.matcher(line).find();
        }

        private static PlantUmlDependencyMatcher parseDependencyFromRightToLeft(String line) {
            List<String> parts = parseParts(line, DEPENDENCY_LEFT_ARROW_PATTERN);
            return new PlantUmlDependencyMatcher(parts.get(1), parts.get(0));
        }

        private static List<String> parseParts(String line, Pattern dependencyRightArrowPattern) {
            line = removeOptionalDescription(line);
            return Splitter.on(dependencyRightArrowPattern).trimResults().limit(2).splitToList(line);
        }

        private static String removeOptionalDescription(String line) {
            return line.replaceAll(":.*", "");
        }
    }
}
