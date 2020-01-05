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
import java.util.List;
import java.util.ListIterator;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public abstract class ForwardingList<T> extends ForwardingCollection<T> implements List<T> {
    protected ForwardingList() {
    }

    @Override
    protected abstract List<T> delegate();

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean addAll(int index, Collection<? extends T> c) {
        return delegate().addAll(index, c);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public T get(int index) {
        return delegate().get(index);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public T set(int index, T element) {
        return delegate().set(index, element);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void add(int index, T element) {
        delegate().add(index, element);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public T remove(int index) {
        return delegate().remove(index);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public int indexOf(Object o) {
        return delegate().indexOf(o);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public int lastIndexOf(Object o) {
        return delegate().lastIndexOf(o);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ListIterator<T> listIterator() {
        return delegate().listIterator();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ListIterator<T> listIterator(int index) {
        return delegate().listIterator(index);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public List<T> subList(int fromIndex, int toIndex) {
        return delegate().subList(fromIndex, toIndex);
    }
}
