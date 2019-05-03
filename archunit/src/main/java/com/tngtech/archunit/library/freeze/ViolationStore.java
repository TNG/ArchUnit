/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;

/**
 * Allows to provide some sort of storage for existing violations. In particular on the first check of a {@link FreezingArchRule}, all existing
 * violations will be persisted to the configured {@link ViolationStore}.
 */
@PublicAPI(usage = INHERITANCE)
public interface ViolationStore {

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

    class Factory {
        static final String FREEZE_STORE_PROPERTY = "freeze.store";

        static ViolationStore create() {
            return ArchConfiguration.get().containsProperty(FREEZE_STORE_PROPERTY)
                    ? createInstance(ArchConfiguration.get().getProperty(FREEZE_STORE_PROPERTY))
                    : DefaultViolationStoreFactory.create();
        }

        private static ViolationStore createInstance(String violationStoreClassName) {
            try {
                return (ViolationStore) newInstanceOf(Class.forName(violationStoreClassName));
            } catch (Exception e) {
                String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                        ViolationStore.class.getSimpleName(), FREEZE_STORE_PROPERTY, violationStoreClassName);
                throw new StoreInitializationFailedException(message, e);
            }
        }
    }
}
