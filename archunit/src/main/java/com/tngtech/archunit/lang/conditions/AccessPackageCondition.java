package com.tngtech.archunit.lang.conditions;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class AccessPackageCondition extends ArchCondition<JavaAccess<?>> {
    private final List<PackageMatcher> packageMatchers = new ArrayList<>();
    private final Function<JavaAccess<?>, String> getPackage;

    private AccessPackageCondition(String[] packageIdentifiers, Function<JavaAccess<?>, String> getPackage) {
        this.getPackage = getPackage;
        for (String identifier : packageIdentifiers) {
            packageMatchers.add(PackageMatcher.of(identifier));
        }
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
    public void check(JavaAccess<?> item, ConditionEvents events) {
        boolean matches = false;
        for (PackageMatcher matcher : packageMatchers) {
            matches = matches || matcher.matches(getPackage.apply(item));
        }
        events.add(new ConditionEvent(matches, item.getDescription()));
    }

    static class Creator {
        private final Function<JavaAccess<?>, String> getPackage;

        private Creator(Function<JavaAccess<?>, String> getPackage) {
            this.getPackage = getPackage;
        }

        public AccessPackageCondition matching(String[] packageIdentifiers) {
            return new AccessPackageCondition(packageIdentifiers, getPackage);
        }
    }
}
