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
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.gson.annotations.Expose;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;

class JsonViolation {
    @Expose
    private String origin;
    @Expose
    private String target;

    private JsonViolation(String origin, String target) {
        this.origin = origin;
        this.target = target;
    }

    static JsonViolation from(JavaAccess fieldAccess) {
        return new JsonViolation(
                fieldAccess.getOrigin().getFullName(),
                fieldAccess.getTarget().getFullName()
        );
    }

    public static JsonViolation from(Dependency violatingObject) {
        Set<JavaAccess> javaAccesses = violatingObject.convertTo(JavaAccess.class);
        if (javaAccesses.size() == 1) {
            JavaAccess<?> access = Iterables.getOnlyElement(javaAccesses);
            return new JsonViolation(access.getOrigin().getFullName(), access.getTarget().getFullName());
        }
        return new JsonViolation(violatingObject.getOriginClass().getName(), violatingObject.getTargetClass().getName());
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
        final JsonViolation other = (JsonViolation) obj;
        return Objects.equals(this.origin, other.origin)
                && Objects.equals(this.target, other.target);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("origin", origin)
                .add("target", target)
                .toString();
    }
}
