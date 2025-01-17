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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

final class SimpleConditionEvents implements ConditionEvents {
    private final List<ConditionEvent> violations = new ArrayList<>();
    private Optional<String> informationAboutNumberOfViolations = Optional.empty();

    SimpleConditionEvents() {
    }

    @Override
    public void add(ConditionEvent event) {
        if (event.isViolation()) {
            violations.add(event);
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
        return ImmutableList.copyOf(violations);
    }

    @Override
    public boolean containViolation() {
        return !violations.isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + violations + '}';
    }
}
