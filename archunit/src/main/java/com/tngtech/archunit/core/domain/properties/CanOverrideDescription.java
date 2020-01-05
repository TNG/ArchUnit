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
package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface CanOverrideDescription<SELF> {
    /**
     * Allows to adjust the description of this object. Note that this method will not modify the current object,
     * but instead return a new object with adjusted description.
     *
     * @param newDescription The description the result of this method will hold
     * @return A <b>new</b> equivalent object with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    SELF as(String newDescription);
}
