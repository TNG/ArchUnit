/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.ArchConfiguration;

enum AllowEmptyShould {
    TRUE {
        @Override
        public boolean isAllowed() {
            return true;
        }
    },
    FALSE {
        @Override
        public boolean isAllowed() {
            return false;
        }
    },
    AS_CONFIGURED {
        @Override
        public boolean isAllowed() {
            return ArchConfiguration.get().getPropertyOrDefault(FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME, TRUE.toString())
                    .equalsIgnoreCase(FALSE.toString());
        }
    };

    private static final String FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME = "archRule.failOnEmptyShould";

    abstract boolean isAllowed();

    static AllowEmptyShould fromBoolean(boolean allow) {
        return allow ? TRUE : FALSE;
    }
}
