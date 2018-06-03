package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class SliceDependencyErrorMatcher implements MessageAssertionChain.Link {
    private String dependencyDescription;
    private final Set<ExpectedAccess> expectedAccesses = new HashSet<>();

    public static SliceDependencyErrorMatcher sliceDependency() {
        return new SliceDependencyErrorMatcher();
    }

    private SliceDependencyErrorMatcher() {
    }

    public SliceDependencyErrorMatcher described(String description) {
        dependencyDescription = description;
        return this;
    }

    public SliceDependencyErrorMatcher by(ExpectedAccess expectedAccess) {
        expectedAccesses.add(expectedAccess);
        return this;
    }

    @Override
    public Result filterMatching(List<String> lines) {
        List<String> mismatches = new ArrayList<>();
        List<String> remainingLines = new ArrayList<>(lines);
        boolean matches = remainingLines.remove(dependencyDescription + ":");
        if (!matches) {
            mismatches.add("Description " + dependencyDescription + " was missing");
        }
        for (ExpectedAccess expectedAccess : expectedAccesses) {
            if (!remainingLines.remove(expectedAccess.toString())) {
                mismatches.add("Expected Access " + expectedAccess.toString() + " was missing");
                matches = false;
            }
        }
        if (!matches) {
            return new Result(false, lines, Joiner.on(System.lineSeparator()).join(mismatches));
        }
        return new Result(true, remainingLines);
    }

    @Override
    public String getDescription() {
        return Joiner.on(System.lineSeparator()).join(ImmutableList.builder()
                .add("Description: " + dependencyDescription)
                .add("And all Accesses:")
                .addAll(expectedAccesses)
                .build());
    }
}
