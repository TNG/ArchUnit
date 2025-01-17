/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

/**
 * A version of {@link ConditionEvents} that tracks both violated and satisfied {@link ConditionEvent events},
 * so specific {@link ArchCondition ArchConditions} can use them to create composite events.<br>
 * E.g. {@link ContainAnyCondition} needs to track satisfied events to be able to invert the event
 * if it is used in the context of {@link NeverCondition}.
 */
final class ViolatedAndSatisfiedConditionEvents implements ConditionEvents {
    private final List<ConditionEvent> allowedEvents = new ArrayList<>();
    private final List<ConditionEvent> violatingEvents = new ArrayList<>();
    private Optional<String> informationAboutNumberOfViolations = Optional.empty();

    @Override
    public void add(ConditionEvent event) {
        if (event.isViolation()) {
            violatingEvents.add(event);
        } else {
            allowedEvents.add(event);
        }
    }

    @Override
    public Optional<String> getInformationAboutNumberOfViolations() {
        return informationAboutNumberOfViolations;
    }

    @Override
    public void setInformationAboutNumberOfViolations(String informationAboutNumberOfViolations) {
        this.informationAboutNumberOfViolations = Optional.of(informationAboutNumberOfViolations);
    }

    @Override
    public Collection<ConditionEvent> getViolating() {
        return violatingEvents;
    }

    Collection<ConditionEvent> getAllowed() {
        return allowedEvents;
    }

    @Override
    public boolean containViolation() {
        return !getViolating().isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "Allowed Events: " + getAllowed() +
                "; Violating Events: " + getViolating() +
                '}';
    }
}
