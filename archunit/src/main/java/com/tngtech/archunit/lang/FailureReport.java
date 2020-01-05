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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class FailureReport implements CollectsLines {
    private final Set<String> failureMessages;
    private final HasDescription rule;
    private final Priority priority;

    FailureReport(HasDescription rule, Priority priority, Collection<String> failureMessages) {
        this.rule = rule;
        this.priority = priority;
        this.failureMessages = new TreeSet<>(failureMessages);
    }

    @PublicAPI(usage = ACCESS)
    public boolean isEmpty() {
        return failureMessages.isEmpty();
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getDetails() {
        return ImmutableList.copyOf(failureMessages);
    }

    /**
     * @deprecated See {@link CollectsLines#add(String)}
     */
    @Deprecated
    @Override
    public void add(String message) {
        failureMessages.add(message);
    }

    @Override
    public String toString() {
        return ConfiguredMessageFormat.get().formatFailure(rule, failureMessages, priority);
    }

    FailureReport filter(Predicate<String> predicate) {
        Set<String> filtered = new TreeSet<>();
        for (String message : failureMessages) {
            if (predicate.apply(message)) {
                filtered.add(message);
            }
        }
        return new FailureReport(rule, priority, filtered);
    }
}
