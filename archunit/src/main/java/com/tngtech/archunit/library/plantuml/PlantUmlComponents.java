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
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import static com.tngtech.archunit.library.plantuml.PlantUmlComponent.Functions.GET_COMPONENT_NAME;
import static com.tngtech.archunit.library.plantuml.PlantUmlComponent.Functions.TO_EXISTING_ALIAS;

class PlantUmlComponents {
    private final Map<ComponentName, PlantUmlComponent> componentsByName;
    private final Map<Alias, PlantUmlComponent> componentsByAlias;

    PlantUmlComponents(Set<PlantUmlComponent> components) {
        componentsByName = FluentIterable.from(components).uniqueIndex(GET_COMPONENT_NAME);
        componentsByAlias = FluentIterable.from(components).filter(WITH_ALIAS).uniqueIndex(TO_EXISTING_ALIAS);
    }

    Collection<PlantUmlComponent> getAllComponents() {
        return componentsByName.values();
    }

    Collection<PlantUmlComponent> getComponentsWithAlias() {
        return componentsByAlias.values();
    }

    PlantUmlComponent findComponentWith(String nameOrAlias) {
        ComponentName componentName = new ComponentName(nameOrAlias);
        Alias alias = new Alias(nameOrAlias);
        PlantUmlComponent result = componentsByAlias.containsKey(alias) ? componentsByAlias.get(alias) : componentsByName.get(componentName);
        if (result == null) {
            throw new IllegalDiagramException("There is no Component with name or alias = '%s'. %s", nameOrAlias,
                    "Components must be specified separately from dependencies.");
        }
        return result;
    }

    PlantUmlComponent findComponentWith(ComponentIdentifier identifier) {
        return componentsByName.get(identifier.getComponentName());
    }

    private static final Predicate<PlantUmlComponent> WITH_ALIAS = new Predicate<PlantUmlComponent>() {
        @Override
        public boolean apply(PlantUmlComponent input) {
            return input.getAlias().isPresent();
        }
    };
}
