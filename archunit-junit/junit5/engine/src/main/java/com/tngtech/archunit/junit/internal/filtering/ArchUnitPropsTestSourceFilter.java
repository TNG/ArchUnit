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
package com.tngtech.archunit.junit.internal.filtering;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.junit.internal.ArchUnitEngineDescriptor;
import com.tngtech.archunit.junit.internal.filtering.TestSelectorFactory.TestSelector;
import org.junit.platform.engine.EngineDiscoveryRequest;

public class ArchUnitPropsTestSourceFilter extends AbstractTestNameFilter {

    private static final String ARCHUNIT_PREFIX = "archunit";
    public static final Predicate<String> NON_EMPTY_STRING = ((Predicate<String>) Strings::isNullOrEmpty).negate();
    private final SelectorMatcher include;
    private final SelectorMatcher exclude;

    public ArchUnitPropsTestSourceFilter(EngineDiscoveryRequest discoveryRequest, ArchUnitEngineDescriptor engineDescriptor) {
        super(discoveryRequest);
        this.include = buildMatcher(discoveryRequest, engineDescriptor, ArchConfiguration.JUNIT_INCLUDE_TESTS_MATCHING, SelectorMatcher.ACCEPT_ALL);
        this.exclude = buildMatcher(discoveryRequest, engineDescriptor, ArchConfiguration.JUNIT_EXCLUDE_TESTS_MATCHING, SelectorMatcher.EMPTY);
    }

    @Override
    protected boolean shouldRunAccordingToTestingTool(TestSelector selector) {
        return include.matches(selector) && !exclude.matches(selector);
    }

    private SelectorMatcher buildMatcher(EngineDiscoveryRequest discoveryRequest,
            ArchUnitEngineDescriptor engineDescriptor,
            String property, SelectorMatcher
            defaultMatcher) {
        return readProperty(discoveryRequest, engineDescriptor, property)
                .map(ArchUnitPropsTestSourceFilter::toMatcher)
                .orElse(defaultMatcher);
    }

    private static SelectorMatcher toMatcher(String commaSeparatedPatterns) {
        return new SelectorMatcher(toPatterns(commaSeparatedPatterns));
    }

    private static String[] toPatterns(String commaSeparatedPatterns) {
        return commaSeparatedPatterns.split(",");
    }


    public static boolean appliesTo(EngineDiscoveryRequest discoveryRequest, ArchUnitEngineDescriptor engineDescriptor) {
        return isProvided(discoveryRequest, engineDescriptor, ArchConfiguration.JUNIT_INCLUDE_TESTS_MATCHING)
                || isProvided(discoveryRequest, engineDescriptor, ArchConfiguration.JUNIT_EXCLUDE_TESTS_MATCHING);
    }

    private static boolean isProvided(EngineDiscoveryRequest discoveryRequest, ArchUnitEngineDescriptor engineDescriptor, String key) {
        return readProperty(discoveryRequest, engineDescriptor, key).isPresent();
    }

    private static Optional<String> readProperty(EngineDiscoveryRequest discoveryRequest, ArchUnitEngineDescriptor engineDescriptor, String key) {
        return Optional.ofNullable(readFromConfigurationParams(discoveryRequest, key))
                .orElseGet(() -> ArchUnitPropsTestSourceFilter.readFromEngineDescriptor(engineDescriptor, key));
    }

    private static Optional<String> readFromEngineDescriptor(ArchUnitEngineDescriptor engineDescriptor, String key) {
        return Optional.ofNullable(engineDescriptor.getConfiguration().getOrDefault(key, null))
                .map(String.class::cast)
                .filter(NON_EMPTY_STRING);
    }

    private static Optional<String> readFromConfigurationParams(EngineDiscoveryRequest discoveryRequest, String key) {
        return discoveryRequest.getConfigurationParameters().get(toSystemPropertyName(key))
                .filter(NON_EMPTY_STRING);
    }

    private static String toSystemPropertyName(String key) {
        return String.join(".", ARCHUNIT_PREFIX, ArchConfiguration.JUNIT_PREFIX, key);
    }

    private static class SelectorMatcher {

        private static final String SINGLE_ESCAPED_ASTERISK_STRING = "\\\\*";
        private static final Pattern MULTIPLE_ESCAPED_ASTERISK = Pattern.compile("(\\\\\\*)+");
        private static final Pattern SINGLE_ESCAPED_ASTERISK = Pattern.compile("\\*", Pattern.LITERAL);

        private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
        private static final SelectorMatcher EMPTY = new SelectorMatcher(new String[0]);
        private static final SelectorMatcher ACCEPT_ALL = new SelectorMatcher(new String[] {"*"});
        private static final String ESCAPED_INPUT = "\\\\$0";
        private static final String MATCH_ALL_CHARACTERS = "\\.\\*";

        private final Collection<Pattern> patterns;

        public SelectorMatcher(String[] patterns) {
            this.patterns = prepareRegexes(patterns);
        }

        private Collection<Pattern> prepareRegexes(String[] patterns) {
            return Arrays.stream(patterns)
                    .map(this::prepareRegex)
                    .collect(Collectors.toList());
        }

        private Pattern prepareRegex(String pattern) {
            String escaped = SPECIAL_REGEX_CHARS.matcher(pattern).replaceAll(ESCAPED_INPUT);
            escaped = MULTIPLE_ESCAPED_ASTERISK.matcher(escaped).replaceAll(SINGLE_ESCAPED_ASTERISK_STRING);
            escaped = SINGLE_ESCAPED_ASTERISK.matcher(escaped).replaceAll(MATCH_ALL_CHARACTERS);
            return Pattern.compile("(?:^|\\.)" + escaped + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }

        boolean matches(TestSelector selector) {
            return patterns.stream().anyMatch(pattern -> matchesSelector(pattern, selector));
        }

        private boolean matchesSelector(Pattern pattern, TestSelector selector) {
            return
                    pattern.matcher(selector.getFullyQualifiedName()).find()
                            || pattern.matcher(selector.getContainerName()).find();
        }

        @Override
        public String toString() {
            return patterns.toString();
        }
    }
}
