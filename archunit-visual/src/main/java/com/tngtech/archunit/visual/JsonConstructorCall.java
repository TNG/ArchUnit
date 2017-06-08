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

class JsonConstructorCall {
    @Expose
    private String to;
    @Expose
    private String startCodeUnit;
    @Expose
    private String targetElement;

    JsonConstructorCall(String to, String startCodeUnit, String targetElement) {
        this.to = to;
        this.startCodeUnit = startCodeUnit;
        this.targetElement = targetElement;
    }
}
