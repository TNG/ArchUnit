package com.tngtech.archunit.integration;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.junit.ExpectedViolation.ExpectedAccess;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

class SliceDependencyErrorMatcher extends TypeSafeDiagnosingMatcher<AssertionError> {
    private String dependencyDescription;
    private final Set<ExpectedAccess> expectedAccesses = new HashSet<>();

    static SliceDependencyErrorMatcher sliceDependency() {
        return new SliceDependencyErrorMatcher();
    }

    private SliceDependencyErrorMatcher() {
    }

    @Override
    protected boolean matchesSafely(AssertionError item, Description mismatchDescription) {
        boolean matches = item.getMessage().contains(dependencyDescription);
        if (!matches) {
            mismatchDescription.appendText("Description ").appendText(dependencyDescription)
                    .appendText(" was missing").appendText(System.lineSeparator());
        }
        for (ExpectedAccess expectedAccess : expectedAccesses) {
            if (!item.getMessage().contains(expectedAccess.toString())) {
                mismatchDescription.appendText("Expected Access ").appendText(expectedAccess.toString())
                        .appendText(" was missing").appendText(System.lineSeparator());
                matches = false;
            }
        }
        if (!matches) {
            mismatchDescription.appendText("##### Complete Error Message").appendText(System.lineSeparator())
                    .appendText(item.getMessage()).appendText(System.lineSeparator()).appendText("#####");
        }
        return matches;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Description: ").appendText(dependencyDescription).appendText(System.lineSeparator())
                .appendText("And all Accesses:").appendText(System.lineSeparator())
                .appendText(Joiner.on(System.lineSeparator()).join(expectedAccesses));
    }

    public SliceDependencyErrorMatcher described(String description) {
        dependencyDescription = description;
        return this;
    }

    public SliceDependencyErrorMatcher byAccess(ExpectedAccess expectedAccess) {
        expectedAccesses.add(expectedAccess);
        return this;
    }
}
