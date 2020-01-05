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
package com.tngtech.archunit.library.plantuml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.library.plantuml.PlantUmlPatterns.PlantUmlComponentMatcher;
import com.tngtech.archunit.library.plantuml.PlantUmlPatterns.PlantUmlDependencyMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.base.Predicates.not;
import static java.nio.charset.StandardCharsets.UTF_8;

class PlantUmlParser {
    private final PlantUmlPatterns plantUmlPatterns = new PlantUmlPatterns();

    PlantUmlDiagram parse(URL url) {
        checkNotNull(url, "URL must not be null");
        return createDiagram(readLines(url));
    }

    private PlantUmlDiagram createDiagram(List<String> rawDiagramLines) {
        List<String> diagramLines = filterOutComments(rawDiagramLines);
        Set<PlantUmlComponent> components = parseComponents(diagramLines);
        PlantUmlComponents plantUmlComponents = new PlantUmlComponents(components);

        List<ParsedDependency> dependencies = parseDependencies(plantUmlComponents, diagramLines);

        return new PlantUmlDiagram.Builder(plantUmlComponents)
                .withDependencies(dependencies)
                .build();
    }

    private List<String> filterOutComments(List<String> lines) {
        return FluentIterable.from(lines).filter(not(containsPattern("^\\s*'"))).toList();
    }

    private List<String> readLines(URL url) {
        try (InputStreamReader in = new InputStreamReader(url.openStream(), UTF_8)) {
            return CharStreams.readLines(in);
        } catch (IOException e) {
            throw new PlantUmlParseException("Could not parse diagram from " + url, e);
        }
    }

    private Set<PlantUmlComponent> parseComponents(List<String> plantUmlDiagramLines) {
        return plantUmlPatterns.filterComponents(plantUmlDiagramLines)
                .transform(toPlantUmlComponent())
                .toSet();
    }

    private ImmutableList<ParsedDependency> parseDependencies(PlantUmlComponents plantUmlComponents, List<String> plantUmlDiagramLines) {
        ImmutableList.Builder<ParsedDependency> result = ImmutableList.builder();
        for (PlantUmlDependencyMatcher matcher : plantUmlPatterns.matchDependencies(plantUmlDiagramLines)) {
            PlantUmlComponent origin = findComponentMatching(plantUmlComponents, matcher.matchOrigin());
            PlantUmlComponent target = findComponentMatching(plantUmlComponents, matcher.matchTarget());
            result.add(new ParsedDependency(origin.getIdentifier(), target.getIdentifier()));
        }
        return result.build();
    }

    private Function<String, PlantUmlComponent> toPlantUmlComponent() {
        return new Function<String, PlantUmlComponent>() {
            @Override
            public PlantUmlComponent apply(String input) {
                return createNewComponent(input);
            }
        };
    }

    private PlantUmlComponent createNewComponent(String input) {
        PlantUmlComponentMatcher matcher = plantUmlPatterns.matchComponent(input);

        ComponentName componentName = new ComponentName(matcher.matchComponentName());
        ImmutableSet<Stereotype> immutableStereotypes = identifyStereotypes(matcher, componentName);
        Optional<Alias> alias = Optional.fromNullable(matcher.matchAlias().transform(TO_ALIAS).orNull());

        return new PlantUmlComponent.Builder()
                .withComponentName(componentName)
                .withStereotypes(immutableStereotypes)
                .withAlias(alias)
                .build();
    }

    private ImmutableSet<Stereotype> identifyStereotypes(PlantUmlComponentMatcher matcher, ComponentName componentName) {
        ImmutableSet.Builder<Stereotype> stereotypes = ImmutableSet.builder();
        for (String stereotype : matcher.matchStereoTypes()) {
            stereotypes.add(new Stereotype(stereotype));
        }

        ImmutableSet<Stereotype> result = stereotypes.build();
        if (result.isEmpty()) {
            throw new IllegalDiagramException(String.format("Components must include at least one stereotype"
                    + " specifying the package identifier(<<..>>), but component '%s' does not", componentName.asString()));
        }
        return result;
    }

    private PlantUmlComponent findComponentMatching(PlantUmlComponents plantUmlComponents, String originOrTargetString) {
        originOrTargetString = originOrTargetString.trim()
                .replaceAll("^\\[", "")
                .replaceAll("]$", "");

        return plantUmlComponents.findComponentWith(originOrTargetString);
    }

    private static final Function<String, Alias> TO_ALIAS = new Function<String, Alias>() {
        @Override
        public Alias apply(String value) {
            return new Alias(value);
        }
    };
}