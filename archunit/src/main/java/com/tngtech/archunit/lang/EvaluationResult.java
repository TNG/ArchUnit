/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents the result of evaluating an {@link ArchRule} against some {@link JavaClasses}.
 * To react to failures during evaluation of the rule, one can use {@link #handleViolations(ViolationHandler)}:
 * <br><br>
 * <pre><code>
 * result.handleViolations(new ViolationHandler&lt;JavaAccess&lt;?&gt;&gt;() {
 *     {@literal @}Override
 *     public void handle(Collection&lt;JavaAccess&lt;?&gt;&gt; violatingObjects, String message) {
 *         // do some reporting or react in any way to violation
 *     }
 * });
 * </code></pre>
 */
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
        return new FailureReport(rule, priority, events.getFailureDescriptionLines());
    }

    @PublicAPI(usage = ACCESS)
    public void add(EvaluationResult part) {
        for (ConditionEvent event : part.events) {
            events.add(event);
        }
    }

    /**
     * @see ConditionEvents#handleViolations(ViolationHandler)
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public void handleViolations(ViolationHandler<?> violationHandler) {
        events.handleViolations(violationHandler);
    }

    @PublicAPI(usage = ACCESS)
    public boolean hasViolation() {
        return events.containViolation();
    }

    @PublicAPI(usage = ACCESS)
    public Priority getPriority() {
        return priority;
    }

    /**
     * Filters all recorded {@link ConditionEvent ConditionEvents} by their textual description.
     * I.e. the lines of the description of an event are passed to the supplied predicate to
     * decide if the event is relevant.
     * @param linePredicate A predicate to determine which lines of events match. Predicate.apply(..) == true will imply the violation will be preserved.
     * @return A new {@link EvaluationResult} containing only matching events
     */
    @PublicAPI(usage = ACCESS)
    public EvaluationResult filterDescriptionsMatching(Predicate<String> linePredicate) {
        ConditionEvents filtered = new ConditionEvents();
        for (ConditionEvent event : events) {
            filtered.add(new FilteredEvent(event, linePredicate));
        }
        return new EvaluationResult(rule, filtered, priority);
    }

    private static class FilteredEvent implements ConditionEvent {
        private final ConditionEvent delegate;
        private final Predicate<String> linePredicate;

        private FilteredEvent(ConditionEvent delegate, Predicate<String> linePredicate) {
            this.delegate = delegate;
            this.linePredicate = linePredicate;
        }

        @Override
        public boolean isViolation() {
            return delegate.isViolation() && !getDescriptionLines().isEmpty();
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            delegate.addInvertedTo(events);
        }

        @Override
        public void describeTo(CollectsLines messages) {
            throw new UnsupportedOperationException("Method should already be obsolete");
        }

        @Override
        public List<String> getDescriptionLines() {
            List<String> result = new ArrayList<>();
            for (String line : delegate.getDescriptionLines()) {
                if (linePredicate.apply(line)) {
                    result.add(line);
                }
            }
            return result;
        }

        @Override
        public void handleWith(Handler handler) {
            delegate.handleWith(new FilteredHandler(handler, linePredicate));
        }
    }

    private static class FilteredHandler implements ConditionEvent.Handler {
        private final ConditionEvent.Handler delegate;
        private final Predicate<String> linePredicate;

        private FilteredHandler(ConditionEvent.Handler delegate, Predicate<String> linePredicate) {
            this.delegate = delegate;
            this.linePredicate = linePredicate;
        }

        @Override
        public void handle(Collection<?> correspondingObjects, String message) {
            if (linePredicate.apply(message)) {
                delegate.handle(correspondingObjects, message);
            }
        }
    }
}
