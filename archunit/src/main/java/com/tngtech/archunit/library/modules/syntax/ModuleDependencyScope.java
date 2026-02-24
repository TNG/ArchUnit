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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.domain.PackageMatchers;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.ArchModule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Used to specify which dependencies should be checked by the respective {@link ArchRule}. Possible options are:
 * <ul>
 *     <li>{@link #consideringAllDependencies()}</li>
 *     <li>{@link #consideringOnlyDependenciesBetweenModules()}</li>
 *     <li>{@link #consideringOnlyDependenciesInAnyPackage(String, String...)}</li>
 * </ul>
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class ModuleDependencyScope implements HasDescription {
    private final String description;
    private final Function<Collection<ArchModule<?>>, Predicate<Dependency>> createPredicate;

    private ModuleDependencyScope(String description, Function<Collection<ArchModule<?>>, Predicate<Dependency>> createPredicate) {
        this.description = checkNotNull(description);
        this.createPredicate = checkNotNull(createPredicate);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unchecked")
    Predicate<Dependency> asPredicate(Collection<? extends ArchModule<?>> modules) {
        return createPredicate.apply((Collection<ArchModule<?>>) modules);
    }

    /**
     * Considers all dependencies of every imported class, including basic Java classes like {@link Object}.
     *
     * @see #consideringOnlyDependenciesBetweenModules()
     * @see #consideringOnlyDependenciesInAnyPackage(String, String...)
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static ModuleDependencyScope consideringAllDependencies() {
        return new ModuleDependencyScope("considering all dependencies", __ -> ___ -> true);
    }

    /**
     * Considers only dependencies of the imported classes between two modules.
     * I.e. origin and target classes of the dependency must be contained within modules under test to be considered.
     * This makes it easy to ignore dependencies to irrelevant classes like {@link Object}, but bears the
     * danger of missing dependencies to classes that are falsely not covered by the declared module structure.
     *
     * @see #consideringAllDependencies()
     * @see #consideringOnlyDependenciesInAnyPackage(String, String...)
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static ModuleDependencyScope consideringOnlyDependenciesBetweenModules() {
        return new ModuleDependencyScope(
                "considering only dependencies between modules",
                modules -> dependency -> modules.stream().anyMatch(it -> it.contains(dependency.getTargetClass()))
        );
    }

    /**
     * Considers only dependencies of imported classes that target packages matching the given the {@link PackageMatcher package identifiers}.
     * This can for example be used to limit checked dependencies to those contained in the own project,
     * e.g. '<code>com.myapp..</code>'.<br>
     * Note that module dependencies, i.e. dependencies between two modules, will never be filtered,
     * so this {@link ModuleDependencyScope} will never limit the amount of considered dependencies
     * more than {@link #consideringOnlyDependenciesBetweenModules()}.
     *
     * @see #consideringAllDependencies()
     * @see #consideringOnlyDependenciesBetweenModules()
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static ModuleDependencyScope consideringOnlyDependenciesInAnyPackage(String packageIdentifier, String... furtherPackageIdentifiers) {
        List<String> packageIdentifiers = Stream.concat(Stream.of(packageIdentifier), stream(furtherPackageIdentifiers)).collect(toList());
        PackageMatchers packageMatchers = PackageMatchers.of(packageIdentifiers);
        String description = String.format("considering only dependencies in any package [%s]", joinSingleQuoted(packageIdentifiers));
        return new ModuleDependencyScope(description, __ -> dependency -> packageMatchers.test(dependency.getTargetClass().getPackageName()));
    }
}
