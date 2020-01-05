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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.library.plantuml.PlantUmlComponentDependency.GET_TARGET;
import static java.util.Collections.emptyList;

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
        return FluentIterable.from(dependencies).transform(GET_TARGET).toList();
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
        return alias.isPresent()
                ? new ComponentIdentifier(componentName, alias.get())
                : new ComponentIdentifier(componentName);
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
        final PlantUmlComponent other = (PlantUmlComponent) obj;
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
        static final Function<PlantUmlComponent, ComponentName> GET_COMPONENT_NAME =
                new Function<PlantUmlComponent, ComponentName>() {
                    @Override
                    public ComponentName apply(PlantUmlComponent input) {
                        return input.getComponentName();
                    }
                };

        static final Function<PlantUmlComponent, Alias> TO_EXISTING_ALIAS =
                new Function<PlantUmlComponent, Alias>() {
                    @Override
                    public Alias apply(PlantUmlComponent input) {
                        checkState(input.getAlias().isPresent(), "Alias does not exist");
                        return input.getAlias().get();
                    }
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
