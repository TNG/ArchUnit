/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;
import com.tngtech.archunit.core.domain.JavaClass;

class JsonJavaClass extends JsonJavaElement {
    private static final String TYPE = "class";

    @Expose
    private String superclass;

    JsonJavaClass(JavaClass clazz, boolean withSuperclass) {
        super(clazz.getSimpleName(), clazz.getName(), TYPE);
        this.superclass = withSuperclass && clazz.getSuperClass().isPresent() ? clazz.getSuperClass().get().getName() : "";
    }
}
