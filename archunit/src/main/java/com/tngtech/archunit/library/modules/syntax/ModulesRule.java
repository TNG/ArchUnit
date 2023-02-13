/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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

import java.util.function.Predicate;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface ModulesRule extends ArchRule {

    @Override
    @PublicAPI(usage = ACCESS)
    ModulesRule as(String newDescription);

    @Override
    @PublicAPI(usage = ACCESS)
    ModulesRule because(String reason);

    @Override
    @PublicAPI(usage = ACCESS)
    ModulesRule allowEmptyShould(boolean allowEmptyShould);

    /**
     * Ignores all class dependencies from the given origin class to the given target class.
     *
     * @param origin the origin class of dependencies to ignore
     * @param target the target class of dependencies to ignore
     */
    @PublicAPI(usage = ACCESS)
    ModulesRule ignoreDependency(Class<?> origin, Class<?> target);

    /**
     * Ignores all class dependencies from the origin class with the given fully qualified origin class name
     * to the target class with the given fully qualified target class name.
     *
     * @param originFullyQualifiedClassName the fully qualified origin class of dependencies to ignore
     * @param targetFullyQualifiedClassName the fully qualified target class of dependencies to ignore
     */
    @PublicAPI(usage = ACCESS)
    ModulesRule ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName);

    /**
     * Ignores all class dependencies from any origin class matching the given origin class predicate
     * to any target class matching the given target class predicate.
     *
     * @param originPredicate predicate determining for which origins of dependencies the dependency should be ignored
     * @param targetPredicate predicate determining for which targets of dependencies the dependency should be ignored
     */
    @PublicAPI(usage = ACCESS)
    ModulesRule ignoreDependency(Predicate<? super JavaClass> originPredicate, Predicate<? super JavaClass> targetPredicate);

    /**
     * Ignores all class dependencies matching the given predicate.
     */
    @PublicAPI(usage = ACCESS)
    ModulesRule ignoreDependency(Predicate<? super Dependency> dependencyPredicate);
}
