package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.getLast;

public class CyclicErrorMatcher implements MessageAssertionChain.Link {
    private final List<String> cycleDescriptions = new ArrayList<>();
    private final Multimap<String, ExpectedRelation> details = LinkedHashMultimap.create();

    private CyclicErrorMatcher() {
    }

    public static CyclicErrorMatcher cycle() {
        return new CyclicErrorMatcher();
    }

    private String cycleText() {
        return "Cycle detected: " +
                Joiner.on(" -> ").join(FluentIterable.from(cycleDescriptions).append(cycleDescriptions.get(0)));
    }

    private String detailText() {
        return System.lineSeparator() + Joiner.on(System.lineSeparator()).join(detailLines());
    }

    private List<String> detailLines() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Collection<ExpectedRelation>> detail : details.asMap().entrySet()) {
            result.add("Dependencies of " + detail.getKey());
            result.addAll(transform(detail.getValue(), toStringFunction()));
        }
        return result;
    }

    public CyclicErrorMatcher from(String sliceName) {
        cycleDescriptions.add(sliceName);
        return this;
    }

    public CyclicErrorMatcher by(ExpectedRelation dependency) {
        details.put(getLast(cycleDescriptions), dependency);
        return this;
    }

    @Override
    public MessageAssertionChain.Link.Result filterMatching(List<String> lines) {
        final Result.Builder builder = new Result.Builder()
                .containsLine(cycleText());

        for (String sliceName : details.asMap().keySet()) {
            builder.containsLine("Dependencies of " + sliceName);
        }

        for (ExpectedRelation relation : details.values()) {
            relation.associateLines(new ExpectedRelation.LineAssociation() {
                @Override
                public void associateIfPatternMatches(String pattern) {
                    builder.matchesLine(pattern);
                }

                @Override
                public void associateIfStringIsContained(String string) {
                    builder.containsLine(string);
                }
            });
        }

        return builder.build(lines);
    }

    @Override
    public String getDescription() {
        return String.format("Message contains cycle description '%s' and details '%s'",
                cycleText(), detailText());
    }
}
