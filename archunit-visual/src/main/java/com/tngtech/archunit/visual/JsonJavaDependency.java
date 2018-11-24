/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.visual;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.Expose;
import com.tngtech.archunit.core.domain.Dependency;

class JsonJavaDependency {
    @Expose
    private String type;
    @Expose
    private String description;
    @Expose
    private String originClass;
    @Expose
    private String targetClass;

    private JsonJavaDependency(String type, String description, String originClass, String targetClass) {
        this.type = type;
        this.description = description;
        this.originClass = originClass;
        this.targetClass = targetClass;
    }

    static JsonJavaDependency from(Dependency d) {
        return new JsonJavaDependency(d.getType().name(), d.getDescription(), d.getOriginClass().getName(), d.getTargetClass().getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonJavaDependency other = (JsonJavaDependency) obj;
        return Objects.equals(this.description, other.description);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("description", description)
                .add("originClass", originClass)
                .add("targetClass", targetClass)
                .toString();
    }
}
