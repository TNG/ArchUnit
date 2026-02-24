/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.PluginLoader;

/**
 * Resolved via {@link PluginLoader}
 */
@SuppressWarnings("unused")
class Java9DomainPlugin implements DomainPlugin {
    @Override
    public void plugInAnnotationFormatter(InitialConfiguration<AnnotationFormatter> propertiesFormatter) {
        propertiesFormatter.set(
                AnnotationFormatter.formatAnnotationType(JavaClass::getName)
                        .formatProperties(config -> config
                                .formattingArraysWithCurlyBrackets()
                                .formattingTypesAsClassNames()
                                .quotingStrings()
                        ));
    }
}
