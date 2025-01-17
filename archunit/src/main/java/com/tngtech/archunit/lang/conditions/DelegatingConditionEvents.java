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

import java.util.Collection;
import java.util.Optional;

import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

abstract class DelegatingConditionEvents implements ConditionEvents {
    final ConditionEvents delegate;

    DelegatingConditionEvents(ConditionEvents delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<String> getInformationAboutNumberOfViolations() {
        return delegate.getInformationAboutNumberOfViolations();
    }

    @Override
    public void setInformationAboutNumberOfViolations(String informationAboutNumberOfViolations) {
        delegate.setInformationAboutNumberOfViolations(informationAboutNumberOfViolations);
    }

    @Override
    public Collection<ConditionEvent> getViolating() {
        return delegate.getViolating();
    }

    @Override
    public boolean containViolation() {
        return delegate.containViolation();
    }
}
