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

import java.util.Set;

import com.google.gson.annotations.Expose;
import com.tngtech.archunit.base.Optional;

abstract class JsonElement {
    static final String DEFAULT_ROOT = "default";

    @Expose
    protected String name;
    @Expose
    protected String fullName;
    @Expose
    protected String type;

    JsonElement(String name, String fullName, String type) {
        this.name = name;
        this.fullName = fullName;
        this.type = type;
    }

    String getPath() {
        return fullName.equals(name) ? DEFAULT_ROOT : fullName.substring(0, fullName.length() - name.length() - 1);
    }

    abstract Set<? extends JsonElement> getChildren();

    Optional<? extends JsonElement> getChild(String fullNameChild) {
        if (fullName.equals(fullNameChild)) {
            return Optional.of(this);
        }
        for (JsonElement el : getChildren()) {
            if (fullNameChild.startsWith(el.fullName)) {
                return el.getChild(fullNameChild);
            }
        }
        return Optional.absent();
    }

    abstract void insertJavaElement(JsonJavaElement el);
}
