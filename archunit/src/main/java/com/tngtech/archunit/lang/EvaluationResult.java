package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class EvaluationResult {
    private final HasDescription rule;
    private final ConditionEvents events;
    private final Priority priority;

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, Priority priority) {
        this(rule, new ConditionEvents(), priority);
    }

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, ConditionEvents events, Priority priority) {
        this.rule = rule;
        this.events = events;
        this.priority = priority;
    }

    @PublicAPI(usage = ACCESS)
    public FailureReport getFailureReport() {
        FailureReport failureReport = new FailureReport(rule, priority);
        events.describeFailuresTo(failureReport);
        return failureReport;
    }

    @PublicAPI(usage = ACCESS)
    public void add(EvaluationResult part) {
        for (ConditionEvent event : part.events) {
            events.add(event);
        }
    }
}
