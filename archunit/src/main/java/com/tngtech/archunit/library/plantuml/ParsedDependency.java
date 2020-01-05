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

import com.google.common.base.Function;

class ParsedDependency {
    private final ComponentIdentifier origin;
    private final ComponentIdentifier target;

    ParsedDependency(ComponentIdentifier origin, ComponentIdentifier target) {
        this.origin = origin;
        this.target = target;
    }

    ComponentIdentifier getOrigin() {
        return origin;
    }

    ComponentIdentifier getTarget() {
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
        final ParsedDependency other = (ParsedDependency) obj;
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

    static final Function<ParsedDependency, ComponentIdentifier> GET_ORIGIN = new Function<ParsedDependency, ComponentIdentifier>() {
        @Override
        public ComponentIdentifier apply(ParsedDependency input) {
            return input.getOrigin();
        }
    };
}
