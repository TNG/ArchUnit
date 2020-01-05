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

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Generic interface for an object that gathers lines of text.
 * @deprecated The API induced by this interface feels clumsy in most places and at best "not harmful".
 * There seem to be more clients that are interested in the raw lines than meaningful implementations of this interface.
 * In particular it seems hard to imagine any implementation that would not simply add
 * {@link ConditionEvent#getDescriptionLines()} to the supplied {@link CollectsLines}. Moreover this interface
 * violates ArchUnit's typical pattern of immutability.
 */
@Deprecated
@PublicAPI(usage = INHERITANCE)
public interface CollectsLines {
    /**
     * @deprecated See {@link CollectsLines}. This method will be removed in the future and was never intended to be
     * called by clients anyway.
     */
    @Deprecated
    void add(String line);
}
