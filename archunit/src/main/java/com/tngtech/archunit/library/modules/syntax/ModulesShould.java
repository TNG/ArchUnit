/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface ModulesShould<DESCRIPTOR extends ArchModule.Descriptor> {

    /**
     * Creates a rule to check that the {@link ArchModule}s under consideration don't have any forbidden
     * {@link ArchModule#getModuleDependenciesFromSelf() module dependencies} according to the passed
     * {@code allowedDependencyPredicate}. It is possible to adjust which {@link Dependency class dependencies}
     * the rule considers relevant by the passed {@link ModuleDependencyScope dependencyScope}.
     *
     * @param allowedDependencyPredicate Decides which {@link ModuleDependency module dependencies} are allowed.
     *                                   If the {@link DescribedPredicate} returns {@code true} the dependency is allowed,
     *                                   otherwise it is forbidden.
     * @param dependencyScope            Allows to adjust which {@link Dependency dependencies} are considered relevant by the rule
     * @return An {@link ArchRule} to be checked against a set of {@link JavaClasses}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesRule<DESCRIPTOR> respectTheirAllowedDependencies(
            DescribedPredicate<? super ModuleDependency<DESCRIPTOR>> allowedDependencyPredicate,
            ModuleDependencyScope dependencyScope
    );

    /**
     * Convenience API for {@link #respectTheirAllowedDependencies(DescribedPredicate, ModuleDependencyScope)}
     * that allows to statically define which {@link ArchModule modules} may depend on which other {@link ArchModule modules}
     * by {@link ArchModule#getName() module name}.
     * <br><br>
     * Example:
     * <pre><code>
     * modules()
     *   .definedByPackages("..example.(*)..")
     *   .should().respectTheirAllowedDependencies(
     *     AllowedModuleDependencies.allow()
     *       .fromModule("Module One").toModules("Module Three", "Module Four")
     *       .fromModule("Module Two").toModules("Module Four"),
     *     consideringOnlyDependenciesInAnyPackage("..example.."));
     * </code></pre>
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesRule<DESCRIPTOR> respectTheirAllowedDependencies(AllowedModuleDependencies allowedDependencies, ModuleDependencyScope dependencyScope);

    /**
     * Checks that the {@link Dependency#getTargetClass() target classes} of each {@link Dependency}
     * that originate from one {@link ArchModule} and target another {@link ArchModule} satisfy
     * the passed {@code predicate}.
     *
     * @param predicate A {@link DescribedPredicate} to determine which {@link Dependency#getTargetClass() target classes}
     *                  of {@link Dependency dependencies} are allowed.
     * @return An {@link ArchRule} to be checked against a set of {@link JavaClasses}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesRule<DESCRIPTOR> onlyDependOnEachOtherThroughClassesThat(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Like {@link #onlyDependOnEachOtherThroughClassesThat(DescribedPredicate)} but allows to specify the predicate in a fluent way.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ClassesThat<ModulesRule<DESCRIPTOR>> onlyDependOnEachOtherThroughClassesThat();

    /**
     * Checks that the {@link ArchModule}s under consideration don't have any cyclic dependencies within their
     * {@link ArchModule#getModuleDependenciesFromSelf() module dependencies}.
     *
     * @return An {@link ArchRule} to be checked against a set of {@link JavaClasses}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesRule<DESCRIPTOR> beFreeOfCycles();
}
