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
package com.tngtech.archunit.library.plantuml;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

class Stereotype {
    private String value;

    Stereotype(String value) {
        this.value = checkNotNull(value);
    }

    String asString() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Stereotype other = (Stereotype) obj;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "value='" + value + '\'' +
                '}';
    }
}
