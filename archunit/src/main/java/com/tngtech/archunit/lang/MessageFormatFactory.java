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
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;

import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;
import static java.lang.System.lineSeparator;

class MessageFormatFactory {
    private static final String MESSAGE_FORMAT_PROPERTY = "messageFormat";
    private static final MessageFormat DEFAULT_MESSAGE_FORMAT = new DefaultMessageFormat();

    static MessageFormat create() {
        return ArchConfiguration.get().containsProperty(MESSAGE_FORMAT_PROPERTY)
                ? createInstance(ArchConfiguration.get().getProperty(MESSAGE_FORMAT_PROPERTY))
                : DEFAULT_MESSAGE_FORMAT;
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static MessageFormat createInstance(String messageFormatClassName) {
        try {
            return (MessageFormat) newInstanceOf(Class.forName(messageFormatClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    MessageFormat.class.getSimpleName(), MESSAGE_FORMAT_PROPERTY, messageFormatClassName);
            throw new MessageFormatInitializationFailedException(message, e);
        }
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
