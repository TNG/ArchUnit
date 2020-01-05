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
package com.tngtech.archunit.library.freeze;

import java.util.List;
import java.util.Properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Provides some sort of storage for violations to {@link FreezingArchRule}.
 */
@PublicAPI(usage = INHERITANCE)
public interface ViolationStore {

    /**
     * Provides custom initialization with properties derived from
     * {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}
     * by considering the sub properties of {@code freeze.store}.
     * <br><br>
     * If {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME} contains, e.g.,
     *
     * <pre><code>
     * freeze.store.propOne=valueOne
     * freeze.store.propTwo=valueTwo
     * </code></pre>
     *
     * then this method will be called with properties containing
     *
     * <pre><code>
     * propOne=valueOne
     * propTwo=valueTwo</code></pre>
     *
     * @param properties The properties derived from the {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME} prefix
     *                   {@code freeze.store}.
     */
    void initialize(Properties properties);

    /**
     * @param rule An {@link ArchRule}
     * @return true, if and only if this {@link ViolationStore} contains stored violations for the passed {@link ArchRule}
     */
    boolean contains(ArchRule rule);

    /**
     * Provides a way to initially store or later update violations of an {@link ArchRule}. If there are violations currently stored for the passed
     * rule, those violations will be completely overwritten.
     *
     * @param rule An {@link ArchRule} to store violations for
     * @param violations A list of lines of violations of an {@link ArchRule}
     */
    void save(ArchRule rule, List<String> violations);

    /**
     * @param rule An {@link ArchRule}
     * @return The lines of violations currently stored for the passed {@link ArchRule}
     */
    List<String> getViolations(ArchRule rule);
}
