/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
    private static final String CLASS_SEPARATOR = "$";

    @Expose
    private String superclass;

    JsonJavaClass(JavaClass clazz, boolean withSuperclass) {
        super(clazz.getSimpleName(), clazz.getName(), TYPE);
        this.superclass = withSuperclass && clazz.getSuperClass().isPresent() ? clazz.getSuperClass().get().getName() : "";
    }

    private JsonJavaClass(String simpleName, String fullName, JsonJavaElement innerClass) {
        super(simpleName, fullName, TYPE);
        insert(innerClass);
    }

    static JsonJavaClass createEnclosingClassOf(JsonJavaElement innerClass, String existingPath) {
        String innerClassPath = innerClass.getPath();
        int beginIndex = existingPath.length();
        if (innerClassPath.indexOf(JsonJavaPackage.PACKAGE_SEPARATOR, beginIndex + 1) != -1) {
            throw new RuntimeException("the package of a class was not added to the package-structure ---" + innerClass.fullName);
        }
        int endIndex = innerClassPath.indexOf(CLASS_SEPARATOR, beginIndex + 1);
        if (endIndex == -1) {
            String simpleName = innerClassPath.substring(beginIndex + 1);
            return new JsonJavaClass(simpleName, innerClassPath, innerClass);
        }
        else {
            String simpleName = innerClassPath.substring(beginIndex + 1, endIndex);
            String fullName = innerClassPath.substring(0, endIndex);
            return new JsonJavaClass(simpleName, fullName, createEnclosingClassOf(innerClass, fullName));
        }
    }
}
