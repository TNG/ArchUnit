/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.htmlvisualization;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
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
        this.fullName = parseFullName(fullName);
        this.name = name.isEmpty() ? this.fullName.substring(this.fullName.lastIndexOf('$') + 1) : name;
        this.type = type;
    }

    // FIXME: The JsonExporter does not handle array types correctly (name starts with '['). As a quick fix we convert
    //        it to the canonical name in this case. The JsonExporter should be completely overhauled, all those string
    //        "let's look for some index of '.'" or similar operations must be replaced by robust domain object methods.
    //        This can probably be massively simplified by using defaultPackage.accept(packageVisitor). Also the whole
    //        exporter code is procedural in disguise, objects and many small methods, but in fact many times the arguments
    //        passed to some method are just manipulated from the outside
    protected abstract String parseFullName(String fullName);

    public String getFullName() {
        return fullName;
    }

    final String getPath() {
        return getFullName().equals(name) ? DEFAULT_ROOT : getFullName().substring(0, getFullName().length() - name.length() - 1);
    }

    abstract Set<? extends JsonElement> getChildren();

    Optional<? extends JsonElement> getChild(String fullNameChild) {
        if (getFullName().equals(fullNameChild)) {
            return Optional.of(this);
        }
        for (JsonElement el : getChildren()) {
            if (fullNameChild.startsWith(el.getFullName())) {
                return el.getChild(fullNameChild);
            }
        }
        return Optional.absent();
    }

    abstract void addClass(JsonJavaElement element);

    void insert(JsonJavaElement element) {
        if (getFullName().equals(element.getPath())) {
            addClass(element);
        } else {
            insertToChild(element);
        }
    }

    private void insertToChild(JsonJavaElement jsonJavaElement) {
        for (JsonElement child : getChildren()) {
            if (jsonJavaElement.getFullName().startsWith(child.getFullName())
                    && jsonJavaElement.getFullName().substring(child.getFullName().length()).matches("([.$]).*")) {
                child.insert(jsonJavaElement);
                return;
            }
        }

        /* create dummy-enclosing-class, if no parent-class is present
         * (this can occur when a dependency to a class exists, but no dependency to its enclosing class)
         **/
        JsonJavaElement enclosingClass = JsonJavaClass.createEnclosingClassOf(jsonJavaElement, getFullName());
        addClass(enclosingClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonElement other = (JsonElement) obj;
        return Objects.equals(this.getFullName(), other.getFullName());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fullName", getFullName())
                .toString();
    }
}
