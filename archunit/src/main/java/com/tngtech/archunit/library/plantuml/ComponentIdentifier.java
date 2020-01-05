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

import java.util.Objects;

import com.tngtech.archunit.base.Optional;

class ComponentIdentifier {
    private final ComponentName componentName;
    private final Optional<Alias> alias;

    ComponentIdentifier(ComponentName componentName) {
        this(componentName, Optional.<Alias>absent());
    }

    ComponentIdentifier(ComponentName componentName, Alias alias) {
        this(componentName, Optional.of(alias));
    }

    private ComponentIdentifier(ComponentName componentName, Optional<Alias> alias) {
        this.componentName = componentName;
        this.alias = alias;
    }

    ComponentName getComponentName() {
        return componentName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ComponentIdentifier other = (ComponentIdentifier) obj;
        return Objects.equals(this.componentName, other.componentName)
                && Objects.equals(this.alias, other.alias);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "componentName=" + componentName +
                ", alias=" + alias +
                '}';
    }
}
