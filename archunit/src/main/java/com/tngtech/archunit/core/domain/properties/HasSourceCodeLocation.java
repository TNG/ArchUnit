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
import com.tngtech.archunit.core.domain.SourceCodeLocation;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasSourceCodeLocation {
    /**
     * @return The {@link SourceCodeLocation} of this object, i.e. how to locate the respective object within the set of source files.
     */
    @PublicAPI(usage = ACCESS)
    SourceCodeLocation getSourceCodeLocation();
}
