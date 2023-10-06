/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.cycle_detection;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

class SimpleEdge<NODE> implements Edge<NODE> {
    private final NODE origin;
    private final NODE target;

    SimpleEdge(NODE origin, NODE target) {
        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
    }

    @Override
    public NODE getOrigin() {
        return origin;
    }

    @Override
    public NODE getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleEdge<?> that = (SimpleEdge<?>) o;
        return origin.equals(that.origin) && target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("origin", origin)
                .add("target", target)
                .toString();
    }
}
