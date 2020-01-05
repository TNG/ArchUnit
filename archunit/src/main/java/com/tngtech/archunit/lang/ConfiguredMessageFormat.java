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

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.HasDescription;

class ConfiguredMessageFormat {
    private static final ConfiguredMessageFormat instance = new ConfiguredMessageFormat();

    static ConfiguredMessageFormat get() {
        return instance;
    }

    String formatFailure(HasDescription rule, Collection<String> failureMessages, Priority priority) {
        String violationTexts = Joiner.on(System.lineSeparator()).join(failureMessages);
        return String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%d times):%n%s",
                priority.asString(), rule.getDescription(), failureMessages.size(), violationTexts);
    }

    <T> String formatRuleText(HasDescription itemsUnderTest, ArchCondition<T> condition) {
        return String.format("%s should %s", itemsUnderTest.getDescription(), condition.getDescription());
    }
}
