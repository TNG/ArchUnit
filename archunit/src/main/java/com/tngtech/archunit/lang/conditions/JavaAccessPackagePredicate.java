/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
    private final Function<JavaAccess<?>, String> getPackageName;
    private final PackageMatchers packageMatchers;

    private JavaAccessPackagePredicate(String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackageName) {
        super(String.format("any package ['%s']", Joiner.on("', '").join(packageIdentifiers)));
        this.getPackageName = getPackageName;
        packageMatchers = PackageMatchers.of(packageIdentifiers);
    }

    static Creator forAccessOrigin() {
        return new Creator(new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getOriginOwner().getPackageName();
            }
        });
    }

    static Creator forAccessTarget() {
        return new Creator(new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getTargetOwner().getPackageName();
            }
        });
    }

    @Override
    public boolean apply(JavaAccess<?> input) {
        return packageMatchers.apply(getPackageName.apply(input));
    }

    static class Creator {
        private final Function<JavaAccess<?>, String> getPackageName;

        private Creator(Function<JavaAccess<?>, String> getPackageName) {
            this.getPackageName = getPackageName;
        }

        JavaAccessPackagePredicate matching(final String... packageIdentifiers) {
            return new JavaAccessPackagePredicate(packageIdentifiers, getPackageName);
        }
    }
}
