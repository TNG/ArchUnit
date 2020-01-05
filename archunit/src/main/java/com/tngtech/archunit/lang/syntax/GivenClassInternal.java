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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClass;

class GivenClassInternal implements GivenClass {
    private final Priority priority;
    private final ClassesTransformer<JavaClass> classesTransformer;
    private final Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition;

    GivenClassInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this.priority = priority;
        this.classesTransformer = classesTransformer;
        this.prepareCondition = prepareCondition;
    }

    @Override
    public ClassesShould should() {
        return new ClassesShouldInternal(classesTransformer, priority, prepareCondition);
    }

    @Override
    public ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition) {
        return new ClassesShouldInternal(classesTransformer, priority, condition.<JavaClass>forSubType(), prepareCondition);
    }
}
