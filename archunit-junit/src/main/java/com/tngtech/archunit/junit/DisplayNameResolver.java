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
package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchConfiguration;

final class DisplayNameResolver {

    static final String JUNIT_DISPLAYNAME_REPLACE_UNDERSCORES_BY_SPACES_PROPERTY_NAME = "junit.displayName.replaceUnderscoresBySpaces";

    static String determineDisplayName(String elementName) {
        return replaceUnderscoresBySpaces() ? underscoresReplacedBySpaces(elementName) : elementName;
    }

    private static String underscoresReplacedBySpaces(String elementName) {
        return elementName.replace('_', ' ');
    }

    private static boolean replaceUnderscoresBySpaces() {
        String replaceUnderscoresBySpaces = ArchConfiguration.get()
                .getPropertyOrDefault(JUNIT_DISPLAYNAME_REPLACE_UNDERSCORES_BY_SPACES_PROPERTY_NAME, Boolean.FALSE.toString());
        return Boolean.parseBoolean(replaceUnderscoresBySpaces);
    }
}
