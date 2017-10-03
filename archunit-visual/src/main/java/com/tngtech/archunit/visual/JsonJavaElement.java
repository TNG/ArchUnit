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

import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;

abstract class JsonJavaElement extends JsonElement {
    @Expose
    private Set<String> interfaces = new HashSet<>();
    @Expose
    private Set<JsonAccess> fieldAccesses = new HashSet<>();
    @Expose
    private Set<JsonAccess> methodCalls = new HashSet<>();
    @Expose
    private Set<JsonAccess> constructorCalls = new HashSet<>();
    @Expose
    private Set<String> anonymousImplementation = new HashSet<>();
    @Expose
    private Set<JsonJavaElement> children = new HashSet<>();

    JsonJavaElement(String name, String fullName, String type) {
        super(name, fullName, type);
    }

    @Override
    void insert(JsonJavaElement jsonJavaElement) {
        this.children.add(jsonJavaElement);
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }

    void addInterface(String className) {
        interfaces.add(className);
    }

    void addFieldAccess(JsonAccess access) {
        fieldAccesses.add(access);
    }

    void addMethodCall(JsonAccess access) {
        methodCalls.add(access);
    }

    void addConstructorCall(JsonAccess access) {
        constructorCalls.add(access);
    }

    void addAnonymousImplementation(String className) {
        anonymousImplementation.add(className);
    }
}
