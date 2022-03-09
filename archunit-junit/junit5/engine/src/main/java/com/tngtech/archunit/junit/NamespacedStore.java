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
package com.tngtech.archunit.junit;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.common.base.Objects;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

@SuppressWarnings("unchecked")
class NamespacedStore implements Store {

    private final Map<NamespacedKey, Object> storage;
    private final ExtensionContext.Namespace namespace;
    private final Map<NamespacedKey, Object> parent;

    NamespacedStore(ExtensionContext.Namespace namespace,
            Map<NamespacedKey, Object> storage,
            Map<NamespacedKey, Object> parent) {
        this.namespace = namespace;
        this.storage = storage;
        if (parent != null) {
            this.parent = parent;
        } else {
            this.parent = Collections.emptyMap();
        }
    }

    @Override
    public synchronized Object get(Object key) {
        NamespacedKey properKey = NamespacedKey.of(namespace, key);
        if (parent.containsKey(properKey)) {
            return parent.get(properKey);
        }
        return storage.get(properKey);
    }

    @Override
    public <V> V get(Object key, Class<V> requiredType) {
        return (V) get(key);
    }

    @Override
    public synchronized <K, V> Object getOrComputeIfAbsent(K key, Function<K, V> defaultCreator) {
        NamespacedKey properKey = NamespacedKey.of(namespace, key);
        if (parent.containsKey(properKey)) {
            return parent.get(properKey);
        }
        return storage.computeIfAbsent(properKey,
                (missingKey) -> defaultCreator.apply((K) missingKey.getRawKey()));
    }

    @Override
    public synchronized <K, V> V getOrComputeIfAbsent(K key, Function<K, V> defaultCreator, Class<V> requiredType) {
        return (V) getOrComputeIfAbsent(key, defaultCreator);
    }

    @Override
    public void put(Object key, Object value) {
        storage.put(NamespacedKey.of(namespace, key), value);
    }

    @Override
    public Object remove(Object key) {
        return storage.remove(NamespacedKey.of(namespace, key));
    }

    @Override
    public <V> V remove(Object key, Class<V> requiredType) {
        return (V) storage.remove(NamespacedKey.of(namespace, key));
    }

    final static class NamespacedKey {
        private final ExtensionContext.Namespace namespace;
        private final Object key;

        private NamespacedKey(ExtensionContext.Namespace namespace, Object key) {
            this.namespace = namespace;
            this.key = key;
        }

        public static NamespacedKey of(ExtensionContext.Namespace namespace, Object key) {
            return new NamespacedKey(namespace, key);
        }

        public Object getRawKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NamespacedKey that = (NamespacedKey) o;
            return Objects.equal(namespace, that.namespace) && Objects.equal(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(namespace, key);
        }
    }

}
