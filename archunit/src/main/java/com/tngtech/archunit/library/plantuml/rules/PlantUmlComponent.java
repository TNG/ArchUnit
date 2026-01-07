/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

class PlantUmlComponent {
    private final ComponentName componentName;
    private final Set<Stereotype> stereotypes;
    private final Optional<Alias> alias;
    private List<PlantUmlComponentDependency> dependencies = emptyList();

    private PlantUmlComponent(Builder builder) {
        this.componentName = checkNotNull(builder.componentName);
        this.stereotypes = checkNotNull(builder.stereotypes);
        this.alias = checkNotNull(builder.alias);
    }

    List<PlantUmlComponent> getDependencies() {
        return dependencies.stream().map(PlantUmlComponentDependency::getTarget).collect(toList());
    }

    ComponentName getComponentName() {
        return componentName;
    }

    Collection<Stereotype> getStereotypes() {
        return stereotypes;
    }

    Optional<Alias> getAlias() {
        return alias;
    }

    void finish(List<PlantUmlComponentDependency> dependencies) {
        this.dependencies = ImmutableList.copyOf(dependencies);
    }

    ComponentIdentifier getIdentifier() {
        return alias.map(it -> new ComponentIdentifier(componentName, it))
                .orElseGet(() -> new ComponentIdentifier(componentName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, stereotypes, alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PlantUmlComponent other = (PlantUmlComponent) obj;
        return Objects.equals(this.componentName, other.componentName)
                && Objects.equals(this.stereotypes, other.stereotypes)
                && Objects.equals(this.alias, other.alias);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "componentName=" + componentName +
                ", stereotypes=" + stereotypes +
                ", alias=" + alias +
                ", dependencies=" + dependencies +
                '}';
    }

    static class Functions {
        static final Function<PlantUmlComponent, Alias> TO_EXISTING_ALIAS =
                input -> {
                    checkState(input.getAlias().isPresent(), "Alias does not exist");
                    return input.getAlias().get();
                };
    }

    static class Builder {
        private ComponentName componentName;
        private Set<Stereotype> stereotypes;
        private Optional<Alias> alias;

        Builder withComponentName(ComponentName componentName) {
            this.componentName = componentName;
            return this;
        }

        Builder withStereotypes(Set<Stereotype> stereotypes) {
            this.stereotypes = stereotypes;
            return this;
        }

        Builder withAlias(Optional<Alias> alias) {
            this.alias = alias;
            return this;
        }

        PlantUmlComponent build() {
            return new PlantUmlComponent(this);
        }
    }
}
