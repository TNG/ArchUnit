package com.tngtech.archunit.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.tngtech.archunit.junit.ExpectedViolation.ExpectedAccess;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static com.google.common.collect.Iterables.getLast;

class CyclicErrorMatcher extends TypeSafeDiagnosingMatcher<AssertionError> {
    private final List<String> cycleDescriptions = new ArrayList<>();
    private final Multimap<String, ExpectedAccess> details = LinkedHashMultimap.create();

    static CyclicErrorMatcher cycle() {
        return new CyclicErrorMatcher();
    }

    private String cycleText() {
        return Joiner.on(" -> ").join(FluentIterable.from(cycleDescriptions).append(cycleDescriptions.get(0)));
    }

    private String detailText() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Collection<ExpectedAccess>> detail : details.asMap().entrySet()) {
            sb.append(System.lineSeparator())
                    .append("Dependencies of ").append(detail.getKey())
                    .append(System.lineSeparator())
                    .append(Joiner.on(System.lineSeparator()).join(detail.getValue()));
        }
        return sb.toString();
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText(String.format(" message to contain cycle description '%s'", cycleText()))
                .appendText(String.format(" and details '%s'", detailText()));
    }

    @Override
    protected boolean matchesSafely(AssertionError item, Description mismatchDescription) {
        boolean cycleTextMatches = item.getMessage().contains(cycleText());
        if (!cycleTextMatches) {
            mismatchDescription.appendText(String.format("message '%s' did not contain '%s'", item.getMessage(), cycleText()));
        }
        boolean detailTextMatches = item.getMessage().contains(detailText());
        if (!detailTextMatches) {
            mismatchDescription.appendText(String.format("message '%s' did not contain '%s'", item.getMessage(), detailText()));
        }
        return cycleTextMatches && detailTextMatches;
    }

    public CyclicErrorMatcher from(String sliceName) {
        cycleDescriptions.add(sliceName);
        return this;
    }

    public CyclicErrorMatcher byAccess(ExpectedAccess access) {
        details.put(getLast(cycleDescriptions), access);
        return this;
    }
}
