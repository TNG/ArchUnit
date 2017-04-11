package com.tngtech.archunit.lang.conditions;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.JavaAccess;

class JavaAccessPackagePredicate extends DescribedPredicate<JavaAccess<?>> {
    private final Function<JavaAccess<?>, String> getPackage;
    private final Set<PackageMatcher> packageMatchers;

    private JavaAccessPackagePredicate(String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackage) {
        super(String.format("any package ['%s']", Joiner.on("', '").join(packageIdentifiers)));
        this.getPackage = getPackage;
        ImmutableSet.Builder<PackageMatcher> matchers = ImmutableSet.builder();
        for (String identifier : packageIdentifiers) {
            matchers.add(PackageMatcher.of(identifier));
        }
        packageMatchers = matchers.build();
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
        boolean matches = false;
        for (PackageMatcher matcher : packageMatchers) {
            matches = matches || matcher.matches(getPackage.apply(input));
        }
        return matches;
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
