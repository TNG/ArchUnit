package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.domain.properties.HasDescription;

public class EvaluationResult {
    private final HasDescription rule;
    private final ConditionEvents events;
    private final Priority priority;

    public EvaluationResult(HasDescription rule, Priority priority) {
        this(rule, new ConditionEvents(), priority);
    }

    public EvaluationResult(HasDescription rule, ConditionEvents events, Priority priority) {
        this.rule = rule;
        this.events = events;
        this.priority = priority;
    }

    public FailureReport getFailureReport() {
        FailureReport failureReport = new FailureReport(rule, priority);
        events.describeFailuresTo(failureReport);
        return failureReport;
    }

    public void add(EvaluationResult part) {
        for (ConditionEvent event : part.events) {
            events.add(event);
        }
    }
}
