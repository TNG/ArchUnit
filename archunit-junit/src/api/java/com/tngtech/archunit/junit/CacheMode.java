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
package com.tngtech.archunit.junit;

import java.lang.ref.SoftReference;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Determines how the JUnit test support caches classes.<br>
 * The test support can cache imported classes according to their location between several runs
 * of different test classes, i.e. if <code>ATest</code> analyses <code>file:///some/path</code> and
 * <code>BTest</code> analyses the same classes, the classes imported for <code>ATest</code>
 * will be reused for <code>BTest</code>. If this is not desired, the {@link CacheMode}.{@link #PER_CLASS}
 * can be used to completely deactivate caching between different test classes.
 */
public enum CacheMode {
    /**
     * Signals that imported Java classes should be cached for the current test class only, and discarded afterwards.
     */
    @PublicAPI(usage = ACCESS)
    PER_CLASS,

    /**
     * Signals that imported Java classes should be cached by location
     * (i.e. the combination of URLs used to import these classes).
     * The cache uses {@link SoftReference SoftReferences}, i.e. the heap will be
     * freed, once it is needed, but this might cause a noticeable delay, once the garbage collector starts
     * removing all those references at the last possible moment.
     */
    @PublicAPI(usage = ACCESS)
    FOREVER
}
