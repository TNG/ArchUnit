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

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;

/**
 * Hack to transport meta-information from {@link ArchTestExecution} to {@link ArchUnitSystemPropertyTestFilterJunit4}.
 * Unfortunately, the {@link Filter} interface doesn't allow access to the original child of the {@link Runner},
 * but only the {@link Description}, which is not suitable to obtain the original member name reliably.
 */
@interface ArchTestMetaInfo {
    String memberName();

    class Instance implements ArchTestMetaInfo, Annotation {
        private final String memberName;

        Instance(String memberName) {
            this.memberName = memberName;
        }

        @Override
        public String memberName() {
            return memberName;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ArchTestMetaInfo.class;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Instance instance = (Instance) o;
            return Objects.equals(memberName, instance.memberName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberName);
        }

        @Override
        public String toString() {
            return "@" + ArchTestMetaInfo.class.getSimpleName() + "(" + memberName + ")";
        }
    }
}
