/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingList;
import com.tngtech.archunit.base.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public class FailureMessages extends ForwardingList<String> {
    private final List<String> failures;
    private final Optional<String> informationAboutNumberOfViolations;

    FailureMessages(ImmutableList<String> failures, Optional<String> informationAboutNumberOfViolations) {
        this.failures = failures;
        this.informationAboutNumberOfViolations = informationAboutNumberOfViolations;
    }

    /**
     * @return Textual information about the number of failures. By default will be 'x times' where 'x'
     *         is the number of messages contained (assuming one line is one violation).
     */
    @PublicAPI(usage = ACCESS)
    public String getInformationAboutNumberOfViolations() {
        return informationAboutNumberOfViolations.orElse(failures.size() + " times");
    }

    FailureMessages filter(Predicate<String> predicate) {
        ImmutableList.Builder<String> filtered = ImmutableList.builder();
        for (String message : failures) {
            if (predicate.apply(message)) {
                filtered.add(message);
            }
        }
        return new FailureMessages(filtered.build(), informationAboutNumberOfViolations);
    }

    @Override
    protected List<String> delegate() {
        return failures;
    }
}
