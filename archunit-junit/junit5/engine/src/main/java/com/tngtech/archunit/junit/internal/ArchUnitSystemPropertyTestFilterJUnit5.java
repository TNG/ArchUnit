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
package com.tngtech.archunit.junit.internal;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import static com.tngtech.archunit.junit.internal.ArchUnitTestDescriptor.FIELD_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.internal.ArchUnitTestDescriptor.METHOD_SEGMENT_TYPE;

class ArchUnitSystemPropertyTestFilterJUnit5 {
    private static final String JUNIT_TEST_FILTER_PROPERTY_NAME = "junit.testFilter";
    private static final Set<String> MEMBER_SEGMENT_TYPES = ImmutableSet.of(FIELD_SEGMENT_TYPE, METHOD_SEGMENT_TYPE);

    void filter(TestDescriptor descriptor) {
        ArchConfiguration configuration = ArchConfiguration.get();
        if (!configuration.containsProperty(JUNIT_TEST_FILTER_PROPERTY_NAME)) {
            return;
        }

        String testFilterProperty = configuration.getProperty(JUNIT_TEST_FILTER_PROPERTY_NAME);
        List<String> memberNames = Splitter.on(",").splitToList(testFilterProperty);
        Predicate<TestDescriptor> shouldRunPredicate = testDescriptor -> memberNameMatches(testDescriptor, memberNames);
        removeNonMatching(descriptor, shouldRunPredicate);
    }

    private void removeNonMatching(TestDescriptor descriptor, Predicate<TestDescriptor> shouldRunPredicate) {
        ImmutableSet.copyOf(descriptor.getChildren())
                .forEach(child -> removeNonMatching(child, shouldRunPredicate));

        if (!descriptor.isRoot() && descriptor.getChildren().isEmpty() && !shouldRunPredicate.test(descriptor)) {
            descriptor.removeFromHierarchy();
        }
    }

    private static boolean memberNameMatches(TestDescriptor testDescriptor, List<String> memberNames) {
        UniqueId.Segment lastSegment = testDescriptor.getUniqueId().getLastSegment();
        return MEMBER_SEGMENT_TYPES.contains(lastSegment.getType())
                && memberNames.stream().anyMatch(it -> lastSegment.getValue().equals(it));
    }
}
