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
package com.tngtech.archunit.lang.conditions;

import java.util.function.Function;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.PackageMatchers;

import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;

class JavaAccessPackagePredicate extends DescribedPredicate<JavaAccess<?>> {
    private final Function<JavaAccess<?>, String> getPackageName;
    private final PackageMatchers packageMatchers;

    private JavaAccessPackagePredicate(String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackageName) {
        super(String.format("any package [%s]", joinSingleQuoted(packageIdentifiers)));
        this.getPackageName = getPackageName;
        packageMatchers = PackageMatchers.of(packageIdentifiers);
    }

    static Creator forAccessOrigin() {
        return new Creator(input -> input.getOriginOwner().getPackageName());
    }

    static Creator forAccessTarget() {
        return new Creator(input -> input.getTargetOwner().getPackageName());
    }

    @Override
    public boolean test(JavaAccess<?> input) {
        return packageMatchers.test(getPackageName.apply(input));
    }

    static class Creator {
        private final Function<JavaAccess<?>, String> getPackageName;

        private Creator(Function<JavaAccess<?>, String> getPackageName) {
            this.getPackageName = getPackageName;
        }

        JavaAccessPackagePredicate matching(String... packageIdentifiers) {
            return new JavaAccessPackagePredicate(packageIdentifiers, getPackageName);
        }
    }
}
