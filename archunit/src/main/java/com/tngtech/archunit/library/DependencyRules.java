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
package com.tngtech.archunit.library;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.Slices;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DependencyRules provides a set of general {@link ArchCondition ArchConditions}
 * and {@link ArchRule ArchRules} for checking dependencies between classes.
 *
 * <p>
 * For checking dependencies between classes that belong to different
 * architectural concepts, see also {@link Architectures} and {@link Slices}.
 * </p>
 */
@PublicAPI(usage = ACCESS)
public final class DependencyRules {
    private DependencyRules() {
    }

    /**
     * A rule that checks that none of the given classes directly depends on
     * classes from upper packages. For an description what "directly depends"
     * means, see {@link JavaClass#getDirectDependenciesFromSelf()}.
     *
     * <p>
     * This rule is good practice, because otherwise that might prevent
     * packages on that level from being split into separate artifacts
     * in a clean way in the future.
     * </p>
     *
     * <p>
     * Example that satisfies the rule:
     * <pre>{@code
     * mycomponent
     *   |-- api
     *   |       |-- interface MyPublicInterface
     *   |-- subComponentOne
     *   |       |-- class MyPublicInterfaceImplOne implements MyPublicInterface
     *   |-- subComponentTwo
     *           |-- class MyPublicInterfaceImplTwo implements MyPublicInterface
     * }</pre>
     * </p>
     *
     * <p>
     * Example that violates the rule:
     * <pre>{@code
     * mycomponent
     *   |-- interface MyPublicInterface
     *   |-- subComponentOne
     *   |       |-- class MyPublicInterfaceImplOne implements MyPublicInterface // violation
     *   |-- subComponentTwo
     *           |-- class MyPublicInterfaceImplTwo implements MyPublicInterface // violation
     * }</pre>
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES =
            noClasses().should(dependOnUpperPackages())
                    .because("that might prevent packages on that level from being split into separate artifacts in a clean way");

    /**
     * Returns a condition that matches classes that directly depend on classes
     * from upper packages. For an description what "directly depend" means,
     * see {@link JavaClass#getDirectDependenciesFromSelf()}.
     *
     * <p>
     * Example that satisfies the rule:
     * <pre>{@code
     * mycomponent
     *   |-- api
     *   |       |-- interface MyPublicInterface
     *   |-- subComponentOne
     *   |       |-- class MyPublicInterfaceImplOne implements MyPublicInterface
     *   |-- subComponentTwo
     *           |-- class MyPublicInterfaceImplTwo implements MyPublicInterface
     * }</pre>
     * </p>
     *
     * <p>
     * Example that violates the rule:
     * <pre>{@code
     * mycomponent
     *   |-- interface MyPublicInterface
     *   |-- subComponentOne
     *   |       |-- class MyPublicInterfaceImplOne implements MyPublicInterface // violation
     *   |-- subComponentTwo
     *           |-- class MyPublicInterfaceImplTwo implements MyPublicInterface // violation
     * }</pre>
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> dependOnUpperPackages() {
        return new DependOnUpperPackagesCondition();
    }

    private static class DependOnUpperPackagesCondition extends ArchCondition<JavaClass> {
        DependOnUpperPackagesCondition() {
            super("depend on upper packages");
        }

        @Override
        public void check(final JavaClass clazz, final ConditionEvents events) {
            for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                boolean dependencyOnUpperPackage = isDependencyOnUpperPackage(dependency.getOriginClass(), dependency.getTargetClass());
                events.add(new SimpleConditionEvent(dependency, dependencyOnUpperPackage, dependency.getDescription()));
            }
        }

        private boolean isDependencyOnUpperPackage(JavaClass origin, JavaClass target) {
            String originPackageName = origin.getPackageName();
            String targetSubPackagePrefix = target.getPackageName() + ".";
            return originPackageName.startsWith(targetSubPackagePrefix);
        }
    }
}
