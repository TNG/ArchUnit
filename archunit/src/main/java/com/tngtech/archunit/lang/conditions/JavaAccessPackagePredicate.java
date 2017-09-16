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
package com.tngtech.archunit.lang.conditions;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatchers;
import com.tngtech.archunit.core.domain.JavaAccess;

class JavaAccessPackagePredicate extends DescribedPredicate<JavaAccess<?>> {
    private final Function<JavaAccess<?>, String> getPackage;
    private final PackageMatchers packageMatchers;

    private JavaAccessPackagePredicate(String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackage) {
        super(String.format("any package ['%s']", Joiner.on("', '").join(packageIdentifiers)));
        this.getPackage = getPackage;
        packageMatchers = PackageMatchers.of(packageIdentifiers);
    }

    static Creator forAccessOrigin() {
        return new Creator(new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getOriginOwner().getPackage();
            }
        });
    }

    static Creator forAccessTarget() {
        return new Creator(new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getTargetOwner().getPackage();
            }
        });
    }

    @Override
    public boolean apply(JavaAccess<?> input) {
        return packageMatchers.apply(getPackage.apply(input));
    }

    static class Creator {
        private final Function<JavaAccess<?>, String> getPackage;

        private Creator(Function<JavaAccess<?>, String> getPackage) {
            this.getPackage = getPackage;
        }

        JavaAccessPackagePredicate matching(final String... packageIdentifiers) {
            return new JavaAccessPackagePredicate(packageIdentifiers, getPackage);
        }
    }
}
