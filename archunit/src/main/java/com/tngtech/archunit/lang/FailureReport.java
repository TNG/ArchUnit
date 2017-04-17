/*
 * Copyright 2017 TNG Technology Consulting GmbH
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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.properties.HasDescription;

public class FailureReport implements CollectsLines {
    private final Set<String> failureMessages = new TreeSet<>();
    private final HasDescription rule;
    private final Priority priority;

    FailureReport(HasDescription rule, Priority priority) {
        this.rule = rule;
        this.priority = priority;
    }

    public boolean isEmpty() {
        return failureMessages.isEmpty();
    }

    public List<String> getDetails() {
        return ImmutableList.copyOf(failureMessages);
    }

    @Override
    public void add(String message) {
        failureMessages.add(message);
    }

    @Override
    public String toString() {
        return ConfiguredMessageFormat.get().formatFailure(rule, failureMessages, priority);
    }

    FailureReport filter(Predicate<String> predicate) {
        FailureReport result = new FailureReport(rule, priority);
        for (String message : failureMessages) {
            if (predicate.apply(message)) {
                result.add(message);
            }
        }
        return result;
    }
}
