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
package com.tngtech.archunit.htmlvisualization;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import static com.google.common.base.MoreObjects.toStringHelper;

class JsonSerializable {

    final String name;
    final String fullName;
    final String type;
    final Set<JsonSerializable> children;

    JsonSerializable(String name, String fullName, String type, Set<JsonSerializable> children) {
        this.name = name;
        this.fullName = fullName;
        this.type = type;
        this.children = children;
    }

    JsonSerializable withName(String newName) {
        return new JsonSerializable(newName, fullName, type, children);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("fullName", fullName)
                .add("type", type)
                .add("children", namesOf(children))
                .toString();
    }

    private static String namesOf(Set<JsonSerializable> nodes) {
        return FluentIterable.from(nodes).transform(new Function<JsonSerializable, String>() {
            public String apply(JsonSerializable input) {
                return input.name;
            }
        }).toSet().toString();
    }
}
