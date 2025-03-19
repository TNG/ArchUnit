/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

class PlantUmlComponentDependency {
    private final PlantUmlComponent origin;
    private final PlantUmlComponent target;

    PlantUmlComponentDependency(PlantUmlComponent origin, PlantUmlComponent target) {
        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
    }

    PlantUmlComponent getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PlantUmlComponentDependency other = (PlantUmlComponentDependency) obj;
        return Objects.equals(this.origin, other.origin)
                && Objects.equals(this.target, other.target);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "origin=" + origin +
                ", target=" + target +
                '}';
    }
}
