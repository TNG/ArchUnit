package com.tngtech.archunit.lang.conditions;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class AccessPackageCondition extends ArchCondition<JavaAccess<?>> {
    private final List<PackageMatcher> packageMatchers = new ArrayList<>();
    private final Function<JavaAccess<?>, String> getPackage;

    private AccessPackageCondition(String packageType, String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackage) {
        super(String.format("%s package matches any ['%s']", packageType, Joiner.on("', ").join(packageIdentifiers)));
        this.getPackage = getPackage;
        for (String identifier : packageIdentifiers) {
            packageMatchers.add(PackageMatcher.of(identifier));
        }
    }

    static Creator forAccessOrigin() {
        return new Creator("origin", new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getOriginOwner().getPackage();
            }
        });
    }

    static Creator forAccessTarget() {
        return new Creator("target", new Function<JavaAccess<?>, String>() {
            @Override
            public String apply(JavaAccess<?> input) {
                return input.getTargetOwner().getPackage();
            }
        });
    }

    @Override
    public void check(JavaAccess<?> item, ConditionEvents events) {
        boolean matches = false;
        for (PackageMatcher matcher : packageMatchers) {
            matches = matches || matcher.matches(getPackage.apply(item));
        }
        events.add(new SimpleConditionEvent<>(item, matches, item.getDescription()));
    }

    static class Creator {
        private final Function<JavaAccess<?>, String> getPackage;
        private final String packageType;

        private Creator(String packageType, Function<JavaAccess<?>, String> getPackage) {
            this.getPackage = getPackage;
            this.packageType = packageType;
        }

        public AccessPackageCondition matching(String[] packageIdentifiers) {
            return new AccessPackageCondition(packageType, packageIdentifiers, getPackage);
        }
    }
}
