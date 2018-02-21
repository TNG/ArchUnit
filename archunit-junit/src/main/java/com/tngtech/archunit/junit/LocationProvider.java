/*
 * Copyright 2018 TNG Technology Consulting GmbH
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

import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.importer.Location;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Allows to provide a custom implementation, that supplies {@link Location Locations}
 * to be imported by the JUnit test infrastructure.
 * <p>
 * The implementation must offer a public default (i.e. no arg) constructor.
 * </p>
 */
@PublicAPI(usage = INHERITANCE)
public interface LocationProvider {
    /**
     * Returns locations to be imported for the current test run. The test class parameter
     * can for example be used to evaluate custom annotations on the current test class.
     *
     * @param testClass The class object of the test currently executed
     * @return The locations to import
     */
    Set<Location> get(Class<?> testClass);
}
