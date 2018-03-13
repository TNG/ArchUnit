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
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaCall;

class JsonAccess {
    @Expose
    private String target;
    @Expose
    private String startCodeUnit;
    @Expose
    private String targetCodeElement;

    JsonAccess(JavaAccess<?> access) {
        this.target = access.getTargetOwner().getName();
        this.startCodeUnit = access.getOrigin().getName() + "(" +
                Formatters.formatMethodParameterTypeNames(access.getOrigin().getParameters().getNames()) + ")";
        this.targetCodeElement = access.getTarget().getName();
    }

    JsonAccess(JavaCall<?> javaCall) {
        this.target = javaCall.getTargetOwner().getName();
        this.startCodeUnit = javaCall.getOrigin().getName() + "(" +
                Formatters.formatMethodParameterTypeNames(javaCall.getOrigin().getParameters().getNames()) + ")";
        this.targetCodeElement = javaCall.getTarget().getName() + "(" +
                Formatters.formatMethodParameterTypeNames(javaCall.getTarget().getParameters().getNames()) + ")";
    }
}
