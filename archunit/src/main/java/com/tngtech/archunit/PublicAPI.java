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
package com.tngtech.archunit;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;

import static com.tngtech.archunit.PublicAPI.State.STABLE;

/**
 * Marks classes and members that are part of ArchUnit's public API. I.e. users of ArchUnit should ONLY use
 * those classes and members.<br><br>
 * Furthermore the specified {@link #usage()} defines the way, this public API should be used.<br>
 * {@link Usage#ACCESS} defines that this class or member should only be accessed (e.g. calling a method)
 * by users of ArchUnit. {@link Usage#INHERITANCE} defines that this class / interface may be extended / implemented
 * by users of ArchUnit. Note that this naturally includes permission to access any accessible members
 * of this class / interface.<br><br>
 * Any usage of ArchUnit's classes outside of this contract, is not supported and may break with any (even minor)
 * release.
 */
@Internal
@Inherited
@Documented
public @interface PublicAPI {
    /**
     * Marks how this API is supposed to be used.
     *
     * @see Usage
     */
    Usage usage();

    /**
     * Marks the state of this API, i.e. the maturity. The default is {@link State#STABLE STABLE}, if not
     * explicitly marked otherwise.
     *
     * @see State
     */
    State state() default STABLE;

    /**
     * @see #INHERITANCE
     * @see #ACCESS
     */
    @Internal
    enum Usage {
        /**
         * This API is intended to be used via inheritance, i.e. by extending/implementing this class.
         */
        INHERITANCE,
        /**
         * This API is intended to be accessed, and nothing else. In particular, this API is NOT intended
         * to be implemented/overridden in any way, but instances should always be provided by ArchUnit itself.
         */
        ACCESS
    }

    /**
     * @see #STABLE
     * @see #EXPERIMENTAL
     */
    @Internal
    enum State {
        /**
         * This API is stable for the foreseeable future and will not change in the next couple of major releases.
         */
        STABLE,
        /**
         * This API is still volatile. It might not be suitable for its intended purpose and might change in
         * any way with the next release (even be removed completely).
         */
        EXPERIMENTAL
    }
}
