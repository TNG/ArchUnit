/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.plantuml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import static com.google.common.base.Preconditions.checkState;

class PlantUmlPatterns {
    private static final String COMPONENT_NAME_GROUP_NAME = "componentName";
    private static final String COMPONENT_NAME_FORMAT = "\\[" + capture(anythingBut("\\[\\]"), COMPONENT_NAME_GROUP_NAME) + "]";

    private static final String STEREOTYPE_FORMAT = "(?:<<" + capture(anythingBut("<>")) + ">>\\s*)";
    private static final Pattern STEREOTYPE_PATTERN = Pattern.compile(STEREOTYPE_FORMAT);

    private static final String ALIAS_GROUP_NAME = "alias";
    private static final String ALIAS_FORMAT = "\\s*(?:as \"?" + capture("[^\"]+", ALIAS_GROUP_NAME) + "\"?)?";

    private static final Pattern PLANTUML_COMPONENT_PATTERN = Pattern.compile(
            "^[^'\\S]*" + COMPONENT_NAME_FORMAT + "\\s*" + STEREOTYPE_FORMAT + "*" + ALIAS_FORMAT + "\\s*");

    private static final String DEPENDENCY_ORIGIN_GROUP_NAME = "origin";
    private static final String DEPENDENCY_ORIGIN_FORMAT = "\\[?" + capture(anythingBut("\\'-"), DEPENDENCY_ORIGIN_GROUP_NAME) + "]?";
    private static final String DEPENDENCY_TARGET_GROUP_NAME = "target";
    private static final String DEPENDENCY_TARGET_FORMAT = "\\[?" + capture(anythingBut("-"), DEPENDENCY_TARGET_GROUP_NAME) + "]?";

    private static final Pattern PLANTUML_DEPENDENCY_PATTERN = Pattern.compile(
            "^[^'\\S]*" + DEPENDENCY_ORIGIN_FORMAT + "\\s*" + "-+>" + "\\s*" + DEPENDENCY_TARGET_FORMAT + "\\s*$");

    private static String capture(String pattern) {
        return "(" + pattern + ")";
    }

    private static String capture(String pattern, String name) {
        return "(?<" + name + ">" + pattern + ")";
    }

    private static String anythingBut(String charsJoined) {
        return "[^" + charsJoined + "]+";
    }

    FluentIterable<String> filterComponents(List<String> lines) {
        return FluentIterable.from(lines)
                .filter(matches(PLANTUML_COMPONENT_PATTERN));
    }

    FluentIterable<String> filterDependencies(List<String> lines) {
        return FluentIterable.from(lines)
                .filter(matches(PLANTUML_DEPENDENCY_PATTERN));
    }

    PlantUmlComponentMatcher matchComponent(String input) {
        return new PlantUmlComponentMatcher(input);
    }

    PlantUmlDependencyMatcher matchDependency(String input) {
        return new PlantUmlDependencyMatcher(input);
    }

    private Predicate<String> matches(final Pattern pattern) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return pattern.matcher(input).matches();
            }
        };
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
            return Optional.fromNullable(componentMatcher.group(ALIAS_GROUP_NAME));
        }
    }

    static class PlantUmlDependencyMatcher {
        private final Matcher matcher;

        PlantUmlDependencyMatcher(String input) {
            matcher = PLANTUML_DEPENDENCY_PATTERN.matcher(input);
            checkState(matcher.matches(), "input %s does not match pattern %s", input, PLANTUML_DEPENDENCY_PATTERN);
        }

        String matchOrigin() {
            return matcher.group(DEPENDENCY_ORIGIN_GROUP_NAME);
        }

        String matchTarget() {
            return matcher.group(DEPENDENCY_TARGET_GROUP_NAME);
        }
    }
}
