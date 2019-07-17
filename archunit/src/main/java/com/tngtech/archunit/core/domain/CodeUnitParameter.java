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
package com.tngtech.archunit.core.domain;

import com.google.common.collect.ImmutableList;

public class CodeUnitParameter {

    private JavaClass type;
    private ImmutableList<JavaAnnotation> annotations;

    public CodeUnitParameter(JavaClass type, ImmutableList<JavaAnnotation> annotations) {
        this.type = type;
        this.annotations = annotations;
    }

    public JavaClass getType() {
        return type;
    }

    public void setType(JavaClass type) {
        this.type = type;
    }

    public ImmutableList<JavaAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(ImmutableList<JavaAnnotation> annotations) {
        this.annotations = annotations;
    }
}
