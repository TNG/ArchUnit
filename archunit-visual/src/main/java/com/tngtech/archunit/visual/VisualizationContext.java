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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

abstract class VisualizationContext {
    private VisualizationContext() {
    }

    boolean isElementIncluded(Optional<JavaClass> javaClassOptional) {
        return javaClassOptional.isPresent() && isElementIncluded(javaClassOptional.get());
    }

    boolean isElementIncluded(JavaClass javaClass) {
        return isElementIncluded(javaClass.getName());
    }

    abstract boolean isElementIncluded(String fullName);

    Set<JavaClass> filterIncluded(JavaClasses classes) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaClass clazz : classes) {
            if (isElementIncluded(clazz)) {
                result.add(clazz);
            }
        }
        return result.build();
    }

    static VisualizationContext includeOnly(String rootPackage, String... furtherRootPackages) {
        return new Restricted(ImmutableSet.<String>builder()
                .add(rootPackage)
                .add(furtherRootPackages)
                .build());
    }

    static VisualizationContext everything() {
        return new Everything();
    }

    private static class Restricted extends VisualizationContext {
        private final Set<String> rootPackages;

        private Restricted(Set<String> rootPackages) {
            this.rootPackages = ImmutableSet.copyOf(rootPackages);
        }

        @Override
        boolean isElementIncluded(String fullName) {
            for (String pkg : rootPackages) {
                if (isInPackage(fullName, pkg)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isInPackage(String fullName, String pkg) {
            if (!fullName.startsWith(pkg)) {
                return false;
            }
            String rest = fullName.substring(pkg.length());
            return rest.isEmpty() || rest.startsWith(".");
        }
    }

    private static class Everything extends VisualizationContext {
        @Override
        boolean isElementIncluded(String fullName) {
            return true;
        }
    }
}
