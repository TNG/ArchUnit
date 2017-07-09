/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class ConditionEvents implements Iterable<ConditionEvent> {

    @PublicAPI(usage = ACCESS)
    public ConditionEvents() {
    }

    private final Multimap<Type, ConditionEvent> eventsByViolation = ArrayListMultimap.create();

    @PublicAPI(usage = ACCESS)
    public void add(ConditionEvent event) {
        eventsByViolation.get(Type.from(event.isViolation())).add(event);
    }

    @PublicAPI(usage = ACCESS)
    public Collection<ConditionEvent> getViolating() {
        return eventsByViolation.get(Type.VIOLATION);
    }

    @PublicAPI(usage = ACCESS)
    public Collection<ConditionEvent> getAllowed() {
        return eventsByViolation.get(Type.ALLOWED);
    }

    @PublicAPI(usage = ACCESS)
    public boolean containViolation() {
        return !getViolating().isEmpty();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isEmpty() {
        return getAllowed().isEmpty() && getViolating().isEmpty();
    }

    @PublicAPI(usage = ACCESS)
    public void describeFailuresTo(CollectsLines messages) {
        for (ConditionEvent event : getViolating()) {
            event.describeTo(messages);
        }
    }

    @PublicAPI(usage = ACCESS)
    public void handleViolations(ViolationHandler<?> violationHandler) {
        ConditionEvent.Handler eventHandler = convertToEventHandler(violationHandler);
        for (final ConditionEvent event : eventsByViolation.get(Type.VIOLATION)) {
            event.handleWith(eventHandler);
        }
    }

    private <T> ConditionEvent.Handler convertToEventHandler(final ViolationHandler<T> handler) {
        final Class<?> supportedElementType = TypeToken.of(handler.getClass())
                .resolveType(ViolationHandler.class.getTypeParameters()[0]).getRawType();

        return new ConditionEvent.Handler() {
            @Override
            public void handle(Collection<?> correspondingObjects, String message) {
                if (allElementTypesMatch(correspondingObjects, supportedElementType)) {
                    // If all elements are assignable to T (= supportedElementType), covariance of Collection allows this cast
                    @SuppressWarnings("unchecked")
                    Collection<T> collection = (Collection<T>) correspondingObjects;
                    handler.handle(collection, message);
                }
            }
        };
    }

    private boolean allElementTypesMatch(Collection<?> violatingObjects, Class<?> supportedElementType) {
        for (Object violatingObject : violatingObjects) {
            if (!supportedElementType.isInstance(violatingObject)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Iterator<ConditionEvent> iterator() {
        return ImmutableSet.copyOf(eventsByViolation.values()).iterator();
    }

    @Override
    public String toString() {
        return "ConditionEvents{" +
                "Allowed Events: " + getAllowed() +
                "; Violating Events: " + getViolating() +
                '}';
    }

    private enum Type {
        ALLOWED, VIOLATION;

        private static Type from(boolean violation) {
            return violation ? VIOLATION : ALLOWED;
        }
    }
}
