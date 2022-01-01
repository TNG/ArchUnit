/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain.properties;

import java.util.List;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaTypeVariable;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasTypeParameters<OWNER extends HasDescription> {
    /**
     * @return the type parameters of this object, e.g. for any generic method
     *         <pre><code>&lt;A, B&gt; B someMethod(A a) {..}</code></pre> this would return
     *         the {@link JavaTypeVariable JavaTypeVariables} {@code [A, B]}.<br>
     *         If this object is non-generic, e.g. a method <pre><code>void someMethod() {..}</code></pre>
     *         an empty list will be returned.
     */
    @PublicAPI(usage = ACCESS)
    List<? extends JavaTypeVariable<? extends OWNER>> getTypeParameters();
}
