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
package com.tngtech.archunit.library.modules;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.Convertible;
import com.tngtech.archunit.core.domain.Dependency;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * A dependency between two {@link ArchModule}s. I.e. {@link #getOrigin() origin} and {@link #getTarget() target}
 * are of type {@link ArchModule} and the dependency reflects the group of all {@link #toClassDependencies() class dependencies}
 * where the {@link Dependency#getOriginClass() origin class} resides in the {@link #getOrigin() origin module} and the
 * {@link Dependency#getTargetClass() target class} resides in the {@link #getTarget() target module}.
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class ModuleDependency<DESCRIPTOR extends ArchModule.Descriptor> implements HasDescription, Convertible {
    private final ArchModule<DESCRIPTOR> origin;
    private final ArchModule<DESCRIPTOR> target;
    private final Set<Dependency> classDependencies;

    private ModuleDependency(ArchModule<DESCRIPTOR> origin, ArchModule<DESCRIPTOR> target, Set<Dependency> classDependencies) {
        this.origin = origin;
        this.target = target;
        this.classDependencies = classDependencies;
    }

    /**
     * @return The {@link ArchModule module} where this dependency originates from
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public ArchModule<DESCRIPTOR> getOrigin() {
        return origin;
    }

    /**
     * @return The {@link ArchModule module} that this dependency targets
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public ArchModule<DESCRIPTOR> getTarget() {
        return target;
    }

    /**
     * @return All the single {@link Dependency class dependencies} that form this {@link ModuleDependency},
     *         i.e. all {@link Dependency dependencies} where the {@link Dependency#getOriginClass() origin class}
     *         resides in the {@link #getOrigin() origin module} and the {@link Dependency#getTargetClass() target class}
     *         resides in the {@link #getTarget() target module}.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public Set<Dependency> toClassDependencies() {
        return classDependencies;
    }

    /**
     * @return A textual representation of this {@link ModuleDependency} that can be used for textual reports.
     */
    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public String getDescription() {
        String classDependencyDescriptions = classDependencies.stream()
                .map(HasDescription::getDescription)
                .collect(joining(lineSeparator()));
        return String.format("Module Dependency [%s -> %s]:%n%s", origin.getName(), target.getName(), classDependencyDescriptions);
    }

    @Override
    @SuppressWarnings("unchecked") // compatibility is explicitly checked
    public <T> Set<T> convertTo(Class<T> type) {
        if (type.isAssignableFrom(getClass())) {
            return (Set<T>) singleton(this);
        }
        return classDependencies.stream().flatMap(it -> it.convertTo(type).stream()).collect(toSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin.getIdentifier(), target.getIdentifier());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModuleDependency<?> other = (ModuleDependency<?>) obj;
        return Objects.equals(this.origin.getIdentifier(), other.origin.getIdentifier())
                && Objects.equals(this.target.getIdentifier(), other.target.getIdentifier());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "origin=" + origin +
                ", target=" + target +
                '}';
    }

    static <D extends ArchModule.Descriptor> Optional<ModuleDependency<D>> tryCreate(ArchModule<D> origin, ArchModule<D> target) {
        Set<Dependency> classDependencies = filterDependenciesBetween(origin, target);
        return !classDependencies.isEmpty()
                ? Optional.of(new ModuleDependency<>(origin, target, classDependencies))
                : Optional.empty();
    }

    private static Set<Dependency> filterDependenciesBetween(ArchModule<?> origin, ArchModule<?> target) {
        return origin.getClassDependenciesFromSelf().stream()
                .filter(dependency -> target.contains(dependency.getTargetClass().getBaseComponentType()))
                .collect(toImmutableSet());
    }
}
