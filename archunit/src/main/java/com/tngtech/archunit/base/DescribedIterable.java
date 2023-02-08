/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.base;

import java.util.Iterator;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface DescribedIterable<T> extends Iterable<T>, HasDescription {
    @PublicAPI(usage = ACCESS)
    final class From {
        private From() {
        }

        @PublicAPI(usage = ACCESS)
        public static <T> DescribedIterable<T> iterable(Iterable<T> iterable, String description) {
            return new DescribedIterable<T>() {
                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public Iterator<T> iterator() {
                    return iterable.iterator();
                }
            };
        }
    }
}
