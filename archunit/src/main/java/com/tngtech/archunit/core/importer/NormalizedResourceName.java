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
package com.tngtech.archunit.core.importer;

import java.util.Objects;

class NormalizedResourceName {
    private final String resourceName;

    private NormalizedResourceName(String resourceName) {
        resourceName = resourceName.replaceAll("^/*", "").replaceAll("/*$", "");
        this.resourceName = resourceName;
    }

    static NormalizedResourceName from(String resourceName) {
        return new NormalizedResourceName(resourceName);
    }

    boolean isStartOf(String string) {
        return string.startsWith(resourceName);
    }

    public boolean startsWith(NormalizedResourceName prefix) {
        return equals(prefix) || isAncestorPath(prefix);
    }

    private boolean isAncestorPath(NormalizedResourceName prefix) {
        return resourceName.startsWith(prefix.resourceName) &&
                resourceName.substring(prefix.resourceName.length()).startsWith("/");
    }

    /**
     * @return The resourceName as if it was an absolute path
     *         (i.e. starting with '/' and ending with '/' in case of directories)
     */
    String toAbsolutePath() {
        String result = "/" + resourceName;
        if (!result.endsWith(".class") && !result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    boolean belongsToClassFile() {
        return resourceName.endsWith(".class");
    }

    /**
     * @return The resourceName as if it was an entry of an archive
     *         (i.e. not starting with '/', but ending with '/' in case of directories)
     */
    String toEntryName() {
        return toAbsolutePath().substring(1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NormalizedResourceName other = (NormalizedResourceName) obj;
        return Objects.equals(this.resourceName, other.resourceName);
    }

    @Override
    public String toString() {
        return resourceName;
    }
}
