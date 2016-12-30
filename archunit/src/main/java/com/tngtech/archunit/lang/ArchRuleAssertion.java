package com.tngtech.archunit.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;

import static com.google.common.io.Resources.readLines;
import static java.nio.charset.StandardCharsets.UTF_8;

class ArchRuleAssertion {
    static final String ARCHUNIT_IGNORE_PATTERNS_FILE_NAME = "archunit_ignore_patterns.txt";

    private ArchRule.ClosedArchRule<?> rule;

    private ArchRuleAssertion(ArchRule.ClosedArchRule<?> rule) {
        this.rule = rule;
    }

    void assertNoViolations(Priority priority) {
        ConditionEvents events = rule.evaluate();
        FailureMessages failureMessages = new FailureMessages();
        events.describeFailuresTo(failureMessages);
        FailureMessages messagesToReport = filterIgnoredMessagesFrom(failureMessages);

        if (!messagesToReport.isEmpty()) {
            String message = ConfiguredMessageFormat.get().format(rule, messagesToReport, priority);
            throw new ArchAssertionError(priority, message);
        }
    }

    private FailureMessages filterIgnoredMessagesFrom(FailureMessages messages) {
        Set<Pattern> patterns = readPatternsFrom(ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
        FailureMessages result = new FailureMessages();
        for (String message : messages) {
            if (noPatternMatches(patterns, message)) {
                result.add(message);
            }
        }
        return result;
    }

    private boolean noPatternMatches(Set<Pattern> patterns, String message) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(message.replaceAll("\r*\n", " ")).matches()) {
                return false;
            }
        }
        return true;
    }

    private Set<Pattern> readPatternsFrom(String fileNameInClassPath) {
        URL ignorePatternsResource = getClass().getResource('/' + fileNameInClassPath);
        if (ignorePatternsResource == null) {
            return Collections.emptySet();
        }

        try {
            return readPatternsFrom(ignorePatternsResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Pattern> readPatternsFrom(URL ignorePatternsResource) throws IOException {
        ImmutableSet.Builder<Pattern> result = ImmutableSet.builder();
        for (String line : readLines(ignorePatternsResource, UTF_8)) {
            result.add(Pattern.compile(line));
        }
        return result.build();
    }

    static ArchRuleAssertion from(ArchRule.ClosedArchRule<?> rule) {
        return new ArchRuleAssertion(rule);
    }
}
