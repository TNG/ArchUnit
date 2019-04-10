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

import com.tngtech.archunit.lang.ArchRule;

class DefaultViolationStoreFactory {
    static ViolationStore create() {
        return new TextFileBasedViolationStore();
    }

    private static class TextFileBasedViolationStore implements ViolationStore {
        @Override
        public boolean contains(ArchRule rule) {
            throw new UnsupportedOperationException("Implement me");
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            throw new UnsupportedOperationException("Implement me");
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            throw new UnsupportedOperationException("Implement me");
        }
    }
}
