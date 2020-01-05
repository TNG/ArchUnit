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
package com.tngtech.archunit.base;

import java.util.Collection;
import java.util.Iterator;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public abstract class ForwardingCollection<T> implements Collection<T> {
    protected ForwardingCollection() {
    }

    protected abstract Collection<T> delegate();

    @Override
    @PublicAPI(usage = ACCESS)
    public int size() {
        return delegate().size();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean contains(Object o) {
        return delegate().contains(o);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Iterator<T> iterator() {
        return delegate().iterator();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Object[] toArray() {
        return delegate().toArray();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T1> T1[] toArray(T1[] a) {
        return delegate().toArray(a);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean add(T t) {
        return delegate().add(t);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean remove(Object o) {
        return delegate().remove(o);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean containsAll(Collection<?> c) {
        return delegate().containsAll(c);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean addAll(Collection<? extends T> c) {
        return delegate().addAll(c);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean removeAll(Collection<?> c) {
        return delegate().removeAll(c);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean retainAll(Collection<?> c) {
        return delegate().retainAll(c);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void clear() {
        delegate().clear();
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
