/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import com.google.common.base.Splitter;
import com.tngtech.archunit.ArchConfiguration;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.ParentRunner;

import static java.util.Objects.requireNonNull;

class ArchUnitSystemPropertyTestFilterJunit4 extends Filter {
    private static final String JUNIT_TEST_FILTER_PROPERTY_NAME = "junit.testFilter";
    private final List<String> memberNames;

    private ArchUnitSystemPropertyTestFilterJunit4(List<String> memberNames) {
        this.memberNames = memberNames;
    }

    @Override
    public boolean shouldRun(Description description) {
        ArchTestMetaInfo metaInfo = requireNonNull(description.getAnnotation(ArchTestMetaInfo.class));
        return memberNames.contains(metaInfo.memberName());
    }

    @Override
    public String describe() {
        return JUNIT_TEST_FILTER_PROPERTY_NAME + " = " + memberNames;
    }

    static void filter(ParentRunner<?> runner) throws NoTestsRemainException {
        ArchConfiguration configuration = ArchConfiguration.get();
        if (!configuration.containsProperty(JUNIT_TEST_FILTER_PROPERTY_NAME)) {
            return;
        }

        String testFilterProperty = configuration.getProperty(JUNIT_TEST_FILTER_PROPERTY_NAME);
        runner.filter(new ArchUnitSystemPropertyTestFilterJunit4(Splitter.on(",").splitToList(testFilterProperty)));
    }
}
