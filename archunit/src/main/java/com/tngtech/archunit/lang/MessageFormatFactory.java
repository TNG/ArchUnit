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

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.HasDescription;

import static java.lang.System.lineSeparator;

class MessageFormatFactory {

    private static final MessageFormat instance = new DefaultMessageFormat();
    static MessageFormat create() {
        return instance;
    }

    private static class DefaultMessageFormat implements MessageFormat {
        @Override
        public String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority) {
            String violationTexts = Joiner.on(lineSeparator()).join(failureMessages);
            return String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%s):%n%s",
                                 priority.asString(), rule.getDescription(), failureMessages.getInformationAboutNumberOfViolations(), violationTexts);
        }
    }
}
