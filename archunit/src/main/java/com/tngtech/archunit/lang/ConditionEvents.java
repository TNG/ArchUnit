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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.lang.System.lineSeparator;

public final class ConditionEvents implements Iterable<ConditionEvent> {
    private static final String HANDLER_METHOD_NAME = "handle";

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
    public void handleViolations(ViolationHandler<?> handler) {
        handleViolationsInternal(handler);
    }

    private <T> void handleViolationsInternal(final ViolationHandler<T> handler) {
        Class<T> supportedType = findSupportedTypeFor(handler);
        for (final ConditionEvent event : eventsByViolation.get(Type.VIOLATION)) {
            handleIfPossible(handler, supportedType, event);
        }
    }

    private <T> void handleIfPossible(ViolationHandler<T> handler, Class<T> supportedType, ConditionEvent event) {
        if (supportedType.isInstance(event.getCorrespondingObject())) {
            T correspondingObject = supportedType.cast(event.getCorrespondingObject());
            CollectsLinesToJoin linesToJoin = new CollectsLinesToJoin();
            event.describeTo(linesToJoin);
            handler.handle(correspondingObject, linesToJoin.joinOn(lineSeparator()));
        }
    }

    private <T> Class<T> findSupportedTypeFor(ViolationHandler<T> handler) {
        Set<Method> candidates = new HashSet<>();
        for (Method method : handler.getClass().getMethods()) {
            if (matchesHandlerMethod(method)) {
                candidates.add(method);
            }
        }

        checkState(candidates.size() == 1,
                "Couldn't find an unique method %s.handle(T, String.class)",
                handler.getClass().getName());

        // Cast is safe, because we identified the handler interface method, thus first parameter T matches handler type <T>
        @SuppressWarnings("unchecked")
        Class<T> result = (Class<T>) getOnlyElement(candidates).getParameterTypes()[0];
        return result;
    }

    private boolean matchesHandlerMethod(Method method) {
        boolean nameMatches = method.getName().equals(HANDLER_METHOD_NAME);
        boolean parametersMatch = method.getParameterTypes().length == 2 &&
                method.getParameterTypes()[1] == String.class;
        boolean methodWasntCreatedByCompiler = !method.isSynthetic() && !method.isBridge();

        return nameMatches && parametersMatch && methodWasntCreatedByCompiler;
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
