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
package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.Optional;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Collects {@link ConditionEvent events} that occur when checking {@link ArchCondition ArchConditions}.
 */
@PublicAPI(usage = INHERITANCE)
public interface ConditionEvents {

    /**
     * Adds a {@link ConditionEvent} to these events.
     * @param event A {@link ConditionEvent} caused by an {@link ArchCondition} when checking some element
     */
    void add(ConditionEvent event);

    Optional<String> getInformationAboutNumberOfViolations();

    /**
     * Can be used to override the information about the number of violations. If absent the violated rule
     * will simply report the number of violation lines as the number of violations (which is typically
     * correct, since ArchUnit usually reports one violation per line). However, in cases where
     * violations are omitted (e.g. because a limit of reported violations is configured), this information
     * can be supplied here to inform users that there actually were more violations than reported.
     * @param informationAboutNumberOfViolations The text to be shown for the number of times a rule has been violated
     */
    void setInformationAboutNumberOfViolations(String informationAboutNumberOfViolations);

    /**
     * @return All {@link ConditionEvent events} that correspond to violations.
     */
    Collection<ConditionEvent> getViolating();

    /**
     * @return {@code true}, if these events contain any {@link #getViolating() violating} event, otherwise {@code false}
     */
    boolean containViolation();

    @PublicAPI(usage = ACCESS)
    final class Factory {
        private Factory() {
        }

        @PublicAPI(usage = ACCESS)
        public static ConditionEvents create() {
            return new SimpleConditionEvents();
        }
    }
}
