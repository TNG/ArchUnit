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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;

class FieldsShouldInternal extends MembersShouldInternal<JavaField> {

    FieldsShouldInternal(
            ClassesTransformer<? extends JavaField> classesTransformer,
            Priority priority,
            ArchCondition<JavaField> condition,
            Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }
}
