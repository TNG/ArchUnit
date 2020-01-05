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
package com.tngtech.archunit.base;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class PackageMatchers extends DescribedPredicate<String> {
    private final Set<PackageMatcher> packageMatchers;

    private PackageMatchers(Set<String> packageIdentifiers) {
        super("matches any of ['%s']", Joiner.on("', '").join(packageIdentifiers));
        ImmutableSet.Builder<PackageMatcher> matchers = ImmutableSet.builder();
        for (String identifier : packageIdentifiers) {
            matchers.add(PackageMatcher.of(identifier));
        }
        packageMatchers = matchers.build();
    }

    @PublicAPI(usage = ACCESS)
    public static PackageMatchers of(String... packageIdentifiers) {
        return of(ImmutableSet.copyOf(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static PackageMatchers of(Collection<String> packageIdentifiers) {
        return new PackageMatchers(ImmutableSet.copyOf(packageIdentifiers));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean apply(String aPackage) {
        boolean matches = false;
        for (PackageMatcher matcher : packageMatchers) {
            matches = matches || matcher.matches(aPackage);
        }
        return matches;
    }
}
