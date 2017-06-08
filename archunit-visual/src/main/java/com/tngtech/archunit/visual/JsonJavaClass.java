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

class JsonJavaClass extends JsonJavaElement {
    @Expose
    private String superclass;

    // FIXME: Can we use Formatters.ensureSimpleName() to derive name from fullName??
    // FiXME: Can't we just take JavaClass as input??
    // FIXME: Isn't type always 'class' for a JsonJavaClass? Why do we have to supply it from outside?
    JsonJavaClass(String name, String fullName, String type, String superclass) {
        super(name, fullName, type);
        this.superclass = superclass;
    }

    boolean directlyExtends(String fullName) {
        return superclass.equals(fullName);
    }
}
