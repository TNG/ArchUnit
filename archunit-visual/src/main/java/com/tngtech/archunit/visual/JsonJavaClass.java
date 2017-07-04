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
    static final String INNER_CLASS_SEPARATOR = "$";
    private static final String TYPE = "class";

    @Expose
    private String superclass;

    private JsonJavaClass(String name, String fullname) {
        super(name, fullname, TYPE);
        this.superclass = "";
    }

    JsonJavaClass(JavaClass clazz, boolean withSuperclass) {
        super(clazz.getSimpleName(), clazz.getName(), TYPE);
        this.superclass = withSuperclass && clazz.getSuperClass().isPresent() ? clazz.getSuperClass().get().getName() : "";
    }

    // FIXME AU-18: ArchUnit shows fqn of inner classes with '$', so we should do this here as well, to be consistent
    /*private static String getCleanedFullName(String fullName) {
        return fullName.replace(INNER_CLASS_SEPARATOR, JsonJavaPackage.PACKAGE_SEPARATOR);
    }*/

    boolean directlyExtends(String fullName) {
        return superclass.equals(fullName);
    }
}
