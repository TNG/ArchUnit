package com.tngtech.archunit.lang;

public class ArchRuleAssertion {
    private ClosedArchRule<?> rule;
    private FailureMessages failureMessages = new FailureMessages();

    private ArchRuleAssertion(ClosedArchRule<?> rule) {
        this.rule = rule;
    }

    void assertNoViolations(Priority priority) {
        ConditionEvents events = rule.evaluate();
        events.describeFailuresTo(failureMessages);

        if (!failureMessages.isEmpty()) {
            String message = ConfiguredMessageFormat.get().format(rule, failureMessages, priority);
            throw new ArchAssertionError(priority, message);
        }
    }

    public static ArchRuleAssertion from(ClosedArchRule<?> rule) {
        return new ArchRuleAssertion(rule);
    }
}
