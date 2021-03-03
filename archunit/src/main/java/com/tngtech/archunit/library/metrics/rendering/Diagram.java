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
package com.tngtech.archunit.library.metrics.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.lineSeparator;

public class Diagram {
    private final Map<String, Component> components;
    private final Set<Dependency> dependencies;
    private final Optional<Legend> legend;

    private Diagram(Map<String, Component> components, Set<Dependency> dependencies, Optional<Legend> legend) {
        this.components = components;
        this.dependencies = dependencies;
        this.legend = legend;
    }

    public String render(DiagramSpec diagramSpec) {
        List<String> lines = new ArrayList<>();
        for (Component component : components.values()) {
            lines.add(component.render(diagramSpec));
        }
        lines.add(lineSeparator());
        for (Dependency dependency : dependencies) {
            lines.add(dependency.render(diagramSpec));
        }
        lines.add(lineSeparator());
        if (legend.isPresent()) {
            lines.add(legend.get().render(diagramSpec));
        }
        return diagramSpec.diagramTemplate().replace("${body}", Joiner.on(lineSeparator()).join(lines));
    }

    public static Builder builder() {
        return new Builder();
    }

    private static class Component {
        private static final String validIdentifierCharacters = "A-Za-z0-9";
        private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[" + validIdentifierCharacters + "]+");

        private final String identifier;
        private final List<String> description;

        private Component(String identifier, String description) {
            checkArgument(IDENTIFIER_PATTERN.matcher(identifier).matches(), "Diagram component identifier must match %s", IDENTIFIER_PATTERN);

            this.identifier = identifier;
            this.description = Splitter.onPattern("\r?\n").splitToList(description);
        }

        String render(DiagramSpec diagramSpec) {
            return diagramSpec.renderComponent(identifier, description);
        }
    }

    private static class Dependency {
        private final Component origin;
        private final Component target;

        private Dependency(Component origin, Component target) {
            this.origin = checkNotNull(origin);
            this.target = checkNotNull(target);
        }

        public String render(DiagramSpec diagramSpec) {
            return diagramSpec.renderDependency(origin.identifier, target.identifier);
        }
    }

    private static class Legend {
        private final String text;

        private Legend(String text) {
            this.text = text;
        }

        String render(DiagramSpec diagramSpec) {
            return diagramSpec.renderLegend(text);
        }
    }

    public static class Builder {
        private static final Pattern INVALID_IDENTIFIER_CHAR_PATTERN = Pattern.compile("[^" + Component.validIdentifierCharacters + "]");
        private final ImmutableMap.Builder<String, Component> componentBuilders = ImmutableMap.builder();
        private final SetMultimap<String, String> dependenciesFromSelf = HashMultimap.create();
        private Optional<Legend> legend = Optional.absent();

        private Builder() {
        }

        public void addComponent(String identifier, String text) {
            String componentIdentifier = sanitize(identifier);
            componentBuilders.put(componentIdentifier, new Component(componentIdentifier, text));
        }

        public void addDependency(String originIdentifier, String targetIdentifier) {
            dependenciesFromSelf.put(sanitize(originIdentifier), sanitize(targetIdentifier));
        }

        public void addLegend(String text) {
            legend = Optional.of(new Legend(text));
        }

        private String sanitize(String identifier) {
            return INVALID_IDENTIFIER_CHAR_PATTERN.matcher(identifier).replaceAll("");
        }

        public Diagram build() {
            ImmutableMap<String, Component> components = componentBuilders.build();
            ImmutableSet.Builder<Dependency> dependencies = ImmutableSet.builder();
            for (Map.Entry<String, String> dependencyEntry : dependenciesFromSelf.entries()) {
                dependencies.add(new Dependency(components.get(dependencyEntry.getKey()), components.get(dependencyEntry.getValue())));
            }
            return new Diagram(components, dependencies.build(), legend);
        }
    }
}
