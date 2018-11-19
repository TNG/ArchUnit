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
import com.tngtech.archunit.core.domain.Dependency;

public class JsonJavaDependency {
    @Expose
    private String type;
    @Expose
    private String description;
    @Expose
    private String originClass;
    @Expose
    private String targetClass;

    private JsonJavaDependency(String type, String description, String originClass, String targetClass) {
        this.type = type;
        this.description = description;
        this.originClass = originClass;
        this.targetClass = targetClass;
    }

    public static JsonJavaDependency from(Dependency d) {
        return new JsonJavaDependency(d.getType().name(), d.getDescription(), d.getOriginClass().getName(), d.getTargetClass().getName());
    }

    public static JsonJavaDependency fromDependencyOfAnonymousClass(Dependency d, JsonJavaElement enclosingClass) {
        return new JsonJavaDependency(d.getType().name(), d.getDescription(), enclosingClass.fullName, d.getTargetClass().getName());
    }
}
