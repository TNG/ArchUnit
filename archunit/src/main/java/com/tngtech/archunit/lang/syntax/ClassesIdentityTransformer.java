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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ClassesTransformer;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A {@link ClassesTransformer} that simply returns the supplied collection of {@link JavaClass}
 * (i.e. the identity transformation)
 *
 * @deprecated There is no use for this as part of the ArchUnit API, since users should always use {@link ArchRuleDefinition#classes()}
 */
@Deprecated
public final class ClassesIdentityTransformer extends AbstractClassesTransformer<JavaClass> {
    ClassesIdentityTransformer() {
        super("classes");
    }

    /**
     * @see ClassesIdentityTransformer
     */
    @PublicAPI(usage = ACCESS)
    public static ClassesTransformer<JavaClass> classes() {
        return new ClassesIdentityTransformer();
    }

    @Override
    public Iterable<JavaClass> doTransform(JavaClasses collection) {
        return collection;
    }
}
