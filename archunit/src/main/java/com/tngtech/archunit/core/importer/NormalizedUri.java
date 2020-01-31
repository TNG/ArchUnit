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

import java.net.URI;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

class NormalizedUri {
    private final URI uri;
    private final String firstSegment;
    private final String tailSegments;

    private NormalizedUri(URI uri) {
        String uriString = uri.normalize().toString();
        uriString = uriString.replaceAll("://*", ":/"); // this is how getClass().getResource(..) returns URLs
        uriString = !uriString.endsWith("/") && !uriString.endsWith(".class") ? uriString + "/" : uriString; // we always want folders to end in '/'
        this.uri = URI.create(uriString);
        List<String> path = Splitter.on("/").omitEmptyStrings().splitToList(this.uri.toString().replaceAll("^.*:", ""));
        firstSegment = path.get(0);
        tailSegments = path.size() < 2 ? "" : Joiner.on("/").join(path.subList(1, path.size()));
    }

    URI toURI() {
        return uri;
    }

    String getScheme() {
        return uri.getScheme();
    }

    public String getFirstSegment() {
        return firstSegment;
    }

    public String getTailSegments() {
        return tailSegments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NormalizedUri other = (NormalizedUri) obj;
        return Objects.equals(this.uri, other.uri);
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    static NormalizedUri from(URI uri) {
        return new NormalizedUri(uri);
    }

    static NormalizedUri from(String uriString) {
        return from(URI.create(uriString));
    }
}
