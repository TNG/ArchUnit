/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;

import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;

class ViolationStoreFactory {
    static final String FREEZE_STORE_PROPERTY_NAME = "freeze.store";

    static ViolationStore create() {
        return ArchConfiguration.get().containsProperty(FREEZE_STORE_PROPERTY_NAME)
                ? createInstance(ArchConfiguration.get().getProperty(FREEZE_STORE_PROPERTY_NAME))
                : new TextFileBasedViolationStore();
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static ViolationStore createInstance(String violationStoreClassName) {
        try {
            return (ViolationStore) newInstanceOf(Class.forName(violationStoreClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    ViolationStore.class.getSimpleName(), FREEZE_STORE_PROPERTY_NAME, violationStoreClassName);
            throw new StoreInitializationFailedException(message, e);
        }
    }
}
