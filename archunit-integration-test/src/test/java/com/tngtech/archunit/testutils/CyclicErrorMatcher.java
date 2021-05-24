package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.getLast;
import static java.util.regex.Pattern.quote;

public class CyclicErrorMatcher implements MessageAssertionChain.Link {
    private final List<String> cycleDescriptions = new ArrayList<>();
    private final Multimap<String, ExpectedRelation> details = LinkedHashMultimap.create();

    private CyclicErrorMatcher() {
    }

    public static CyclicErrorMatcher cycle() {
        return new CyclicErrorMatcher();
    }

    private String cycleText() {
        String cycleDetected = "Cycle detected: ";
        String indent = Strings.repeat(" ", cycleDetected.length());
        return cycleDetected +
                Joiner.on(" -> " + System.lineSeparator() + indent).join(FluentIterable.from(cycleDescriptions).append(cycleDescriptions.get(0)));
    }

    private String detailText() {
        return System.lineSeparator() + Joiner.on(System.lineSeparator()).join(detailLines());
    }

    private List<String> detailLines() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Collection<ExpectedRelation>> detail : details.asMap().entrySet()) {
            result.add(dependenciesOfSliceHeaderPattern(detail.getKey()));
            result.addAll(transform(detail.getValue(), r -> detailLinePattern(r.toString())));
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
                .containsText(cycleText());

        for (String sliceName : details.asMap().keySet()) {
            builder.matchesLine(dependenciesOfSliceHeaderPattern(sliceName));
        }

        for (ExpectedRelation relation : details.values()) {
            relation.associateLines(new ExpectedRelation.LineAssociation() {
                @Override
                public void associateIfPatternMatches(String pattern) {
                    builder.matchesLine(pattern);
                }

                @Override
                public void associateIfStringIsContained(String string) {
                    builder.matchesLine(detailLinePattern(string));
                }
            });
        }

        return builder.build(lines);
    }

    private String dependenciesOfSliceHeaderPattern(String sliceName) {
        return "\\s*\\d+. Dependencies of " + quote(sliceName);
    }

    private String detailLinePattern(String string) {
        return ".*" + quote(string) + ".*";
    }

    @Override
    public String getDescription() {
        return String.format("Message contains cycle description '%s' and details '%s'",
                cycleText(), detailText());
    }
}
