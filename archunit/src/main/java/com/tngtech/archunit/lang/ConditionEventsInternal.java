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
package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.reflect.TypeToken;
import com.tngtech.archunit.base.Optional;

final class ConditionEventsInternal implements ConditionEvents {
    private final Multimap<Type, ConditionEvent> eventsByViolation = ArrayListMultimap.create();
    private Optional<String> informationAboutNumberOfViolations = Optional.empty();

    ConditionEventsInternal() {
    }

    @Override
    public void add(ConditionEvent event) {
        eventsByViolation.get(Type.from(event.isViolation())).add(event);
    }

    @Override
    public void setInformationAboutNumberOfViolations(String informationAboutNumberOfViolations) {
        this.informationAboutNumberOfViolations = Optional.of(informationAboutNumberOfViolations);
    }

    @Override
    public Collection<ConditionEvent> getViolating() {
        return eventsByViolation.get(Type.VIOLATION);
    }

    @Override
    public Collection<ConditionEvent> getAllowed() {
        return eventsByViolation.get(Type.ALLOWED);
    }

    @Override
    public boolean containViolation() {
        return !getViolating().isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return getAllowed().isEmpty() && getViolating().isEmpty();
    }

    @Override
    public FailureMessages getFailureMessages() {
        ImmutableList<String> result = FluentIterable.from(getViolating())
                .transformAndConcat(TO_DESCRIPTION_LINES)
                .toSortedList(Ordering.natural());
        return new FailureMessages(result, informationAboutNumberOfViolations);
    }

    @Override
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

    private static final Function<ConditionEvent, Iterable<String>> TO_DESCRIPTION_LINES = new Function<ConditionEvent, Iterable<String>>() {
        @Override
        public Iterable<String> apply(ConditionEvent input) {
            return input.getDescriptionLines();
        }
    };

    private enum Type {
        ALLOWED, VIOLATION;

        private static Type from(boolean violation) {
            return violation ? VIOLATION : ALLOWED;
        }
    }
}
