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
package com.tngtech.archunit.library.modules.syntax;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.stream.Collectors.joining;

/**
 * Defines which module may depend on which other modules by {@link ArchModule#getName() module name}.<br>
 * Start the definition by following the fluent API through {@link #allow()}.<br>
 * Extend the definition by calling {@link #fromModule(String)} multiple times.
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class AllowedModuleDependencies {
    private final SetMultimap<String, String> allowedModuleDependenciesByName = LinkedHashMultimap.create();

    private AllowedModuleDependencies() {
    }

    private AllowedModuleDependencies allowDependencies(String originModuleName, Set<String> allowedTargetModuleNames) {
        allowedModuleDependenciesByName.putAll(originModuleName, allowedTargetModuleNames);
        return this;
    }

    /**
     * Adds allowed {@link ModuleDependency dependencies} that originate from the module with name {@code moduleName}.
     * Finish this definition via {@link RequiringAllowedTargets#toModules(String...)}.
     *
     * @param moduleName A {@link ArchModule#getName() module name}
     * @return An object that allows to specify the allowed targets for this module.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public RequiringAllowedTargets fromModule(String moduleName) {
        return new RequiringAllowedTargets(moduleName);
    }

    DescribedPredicate<ModuleDependency<?>> asPredicate() {
        return DescribedPredicate.describe(
                getDescription(),
                moduleDependency -> allowedModuleDependenciesByName.get(moduleDependency.getOrigin().getName())
                        .contains(moduleDependency.getTarget().getName())
        );
    }

    private String getDescription() {
        return allowedModuleDependenciesByName.asMap().entrySet().stream()
                .map(originToTargets -> originToTargets.getKey() + " -> " + originToTargets.getValue())
                .collect(joining(", ", "{ ", " }"));
    }

    /**
     * Starts the definition of {@link AllowedModuleDependencies}. Follow up via {@link Creator#fromModule(String)}.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static Creator allow() {
        return new Creator();
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class Creator {
        private Creator() {
        }

        /**
         * @see AllowedModuleDependencies#fromModule(String)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public RequiringAllowedTargets fromModule(String moduleName) {
            return new AllowedModuleDependencies().new RequiringAllowedTargets(moduleName);
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public final class RequiringAllowedTargets {
        private final String originModuleName;

        private RequiringAllowedTargets(String originModuleName) {
            this.originModuleName = originModuleName;
        }

        /**
         * Defines the allowed target {@link ArchModule#getName() module names} for the current origin {@link ArchModule#getName() module name}
         *
         * @param targetModuleNames An array of allowed target {@link ArchModule#getName() module names}
         * @return {@link AllowedModuleDependencies}
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public AllowedModuleDependencies toModules(String... targetModuleNames) {
            return allowDependencies(originModuleName, ImmutableSet.copyOf(targetModuleNames));
        }
    }
}
