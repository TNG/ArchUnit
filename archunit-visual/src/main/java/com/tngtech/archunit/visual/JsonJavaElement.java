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
    private Set<JsonFieldAccess> fieldAccesses = new HashSet<>();
    @Expose
    private Set<JsonMethodCall> methodCalls = new HashSet<>();
    @Expose
    private Set<JsonConstructorCall> constructorCalls = new HashSet<>();
    // FIXME: Don't use cryptic shortcuts like 'anonImpl', esp. within public API
    @Expose
    private Set<String> anonImpl = new HashSet<>();
    @Expose
    private Set<JsonJavaElement> children = new HashSet<>();

    JsonJavaElement(String name, String fullName, String type) {
        super(name, fullName, type);
    }

    @Override
    void insertJavaElement(JsonJavaElement el) {
        this.children.add(el);
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }

    void addInterface(String i) {
        interfaces.add(i);
    }

    void addFieldAccess(JsonFieldAccess f) {
        fieldAccesses.add(f);
    }

    void addMethodCall(JsonMethodCall m) {
        methodCalls.add(m);
    }

    void addConstructorCall(JsonConstructorCall c) {
        constructorCalls.add(c);
    }

    void addAnonImpl(String i) {
        anonImpl.add(i);
    }
}
