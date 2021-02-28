/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.metrics.components;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class MetricsElementDependency<T> {
    private final T origin;
    private final T target;

    private MetricsElementDependency(T origin, T target) {
        this.origin = origin;
        this.target = target;
    }

    public T getOrigin() {
        return origin;
    }

    public T getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("origin", origin)
                .add("target", target)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetricsElementDependency)) {
            return false;
        }
        MetricsElementDependency<?> that = (MetricsElementDependency<?>) o;
        return Objects.equals(origin, that.origin) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    public static <T> MetricsElementDependency<T> of(T origin, T target) {
        return new MetricsElementDependency<>(origin, target);
    }
}
