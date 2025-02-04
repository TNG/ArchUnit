/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents the version of a JVM class file.
 */
@PublicAPI(usage = ACCESS)
public final class JavaClassVersion {

    private final int major;
    private final int minor;

    /**
     * Create a new instance of {@link JavaClassVersion}.
     *
     * @param major The major version of the class file.
     * @param minor The minor version of the class file.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClassVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Get the Java version corresponding to the major version of the class file.
     *
     * @return The Java version
     */
    @PublicAPI(usage = ACCESS)
    public int getJavaVersion() {
        // see e.g. https://javaalmanac.io/bytecode/versions/; Java 2 has class file version 46 etc.
        return major - 44;
    }

    /**
     * Get the major version of the class file.
     *
     * @return The major version
     */
    @PublicAPI(usage = ACCESS)
    public int getBytecodeMajorVersion() {
        return major;
    }

    /**
     * Get the minor version of the class file.
     *
     * @return The minor version
     */
    @PublicAPI(usage = ACCESS)
    public int getBytecodeMinorVersion() {
        return minor;
    }

    /**
     * Create a new instance of {@link JavaClassVersion} from the version as provided by ASM.
     *
     * @return A representation of the class file version
     */
    @PublicAPI(usage = ACCESS)
    public static JavaClassVersion of(int asmVersion) {
        int major = asmVersion & 0xFF;
        int minor = asmVersion >> 16;

        return new JavaClassVersion(major, minor);
    }

}
