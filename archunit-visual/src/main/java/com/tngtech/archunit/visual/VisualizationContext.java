/*
 * Copyright 2017 TNG Technology Consulting GmbH
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

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;

class VisualizationContext {
    private final Set<String> rootPackages;

    private VisualizationContext(Set<String> rootPackages) {
        this.rootPackages = ImmutableSet.copyOf(rootPackages);
    }

    boolean isElementIncluded(Optional<JavaClass> javaClassOptional) {
        return javaClassOptional.isPresent() && isElementIncluded(javaClassOptional.get());
    }

    boolean isElementIncluded(JavaClass javaClass) {
        return isElementIncluded(javaClass.getName());
    }

    boolean isElementIncluded(String fullName) {
        if (rootPackages.isEmpty()) {
            return true;
        }
        for (String s : rootPackages) {
            if (fullName.equals(s) || (fullName.startsWith(s) && fullName.substring(s.length()).matches("(\\.|\\$).*"))) {
                return true;
            }
        }
        return false;
    }

    static class Builder {
        private Set<String> rootPackages = new HashSet<>();

        Builder includeOnly(String... rootPackages) {
            return includeOnly(ImmutableSet.copyOf(rootPackages));
        }

        Builder includeOnly(Set<String> rootPackages) {
            this.rootPackages = rootPackages;
            return this;
        }

        VisualizationContext build() {
            return new VisualizationContext(rootPackages);
        }
    }
}
