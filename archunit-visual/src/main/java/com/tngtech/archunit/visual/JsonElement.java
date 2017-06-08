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
    protected String fullname;
    @Expose
    protected String type;

    JsonElement(String name, String fullname, String type) {
        this.name = name;
        this.fullname = fullname;
        this.type = type;
    }

    String getPath() {
        return fullname.equals(name) ? DEFAULT_ROOT : fullname.substring(0, fullname.length() - name.length() - 1);
    }

    abstract Set<? extends JsonElement> getChildren();

    Optional<? extends JsonElement> getChild(String fullnameChild) {
        if (fullname.equals(fullnameChild)) {
            return Optional.of(this);
        }
        for (JsonElement el : getChildren()) {
            if (fullnameChild.startsWith(el.fullname)) {
                return el.getChild(fullnameChild);
            }
        }
        return Optional.absent();
    }

    abstract void insertJavaElement(JsonJavaElement el);
}
