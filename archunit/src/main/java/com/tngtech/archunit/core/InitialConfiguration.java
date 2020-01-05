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
package com.tngtech.archunit.core;

import com.tngtech.archunit.Internal;

import static com.google.common.base.Preconditions.checkState;

@Internal
public class InitialConfiguration<T> {
    private T value;

    public synchronized void set(T object) {
        checkState(this.value == null, String.format(
                "Configuration may only be set once - current: %s / new: %s", this.value, object));

        this.value = object;
    }

    public synchronized T get() {
        checkState(value != null, "No value was ever set");

        return value;
    }
}
