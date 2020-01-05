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
package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.CLASS_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.FIELD_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.METHOD_SEGMENT_TYPE;

class ElementResolver {
    private final ArchUnitEngineDescriptor engineDescriptor;
    private final UniqueId processedId;
    private final Deque<UniqueId.Segment> segmentsToResolve;

    private ElementResolver(ArchUnitEngineDescriptor engineDescriptor, UniqueId processedId, Deque<UniqueId.Segment> segmentsToResolve) {
        this.engineDescriptor = checkNotNull(engineDescriptor);
        this.processedId = checkNotNull(processedId);
        this.segmentsToResolve = checkNotNull(segmentsToResolve);
    }

    PossiblyResolvedClass resolveClass() {
        UniqueId.Segment nextSegment = checkNotNull(segmentsToResolve.peekFirst());
        if (!CLASS_SEGMENT_TYPE.equals(nextSegment.getType())) {
            return new ClassNotRequested();
        }

        return tryResolveClass(classOf(nextSegment), processedId.append(nextSegment));
    }

    PossiblyResolvedClass resolveClass(Class<?> clazz) {
        return tryResolveClass(clazz, processedId.append(CLASS_SEGMENT_TYPE, clazz.getName()));
    }

    PossiblyResolvedMember resolveMethod(Method method) {
        return resolveMember(method, METHOD_SEGMENT_TYPE);
    }

    PossiblyResolvedMember resolveField(Field field) {
        return resolveMember(field, FIELD_SEGMENT_TYPE);
    }

    private PossiblyResolvedMember resolveMember(Member member, String segmentType) {
        UniqueId requestedId = processedId.append(segmentType, member.getName());
        return engineDescriptor.findByUniqueId(requestedId).isPresent()
                ? new SuccessfullyResolvedMember()
                : new UnresolvedMember(member, segmentType);
    }

    void resolve(String segmentType, String segmentValue, Consumer<ElementResolver> doIfResolved) {
        if (segmentsToResolve.isEmpty()) {
            handleNewSegment(segmentType, segmentValue, doIfResolved);
        } else {
            handleRequestedSegment(segmentType, segmentValue, doIfResolved);
        }
    }

    UniqueId getUniqueId() {
        return processedId;
    }

    private PossiblyResolvedClass tryResolveClass(Class<?> clazz, UniqueId classId) {
        ElementResolver childResolver = new ElementResolver(engineDescriptor, classId, tail(segmentsToResolve));
        return engineDescriptor.findByUniqueId(classId)
                .<PossiblyResolvedClass>map(testDescriptor ->
                        new RequestedAndSuccessfullyResolvedClass(testDescriptor, childResolver))
                .orElseGet(() -> new RequestedButUnresolvedClass(clazz, childResolver));
    }

    private Deque<UniqueId.Segment> tail(Deque<UniqueId.Segment> segmentsToResolve) {
        LinkedList<UniqueId.Segment> result = new LinkedList<>(segmentsToResolve);
        result.pollFirst();
        return result;
    }

    @MayResolveTypesViaReflection(reason = "Within the ArchUnitTestEngine we may resolve types via reflection, since they are needed anyway")
    private Class<?> classOf(UniqueId.Segment segment) {
        try {
            return Class.forName(segment.getValue());
        } catch (ClassNotFoundException e) {
            throw new ArchTestInitializationException(e, "Failed to load class from %s segment %s",
                    UniqueId.class.getSimpleName(), segment);
        }
    }

    private void handleRequestedSegment(String segmentType, String segmentValue, Consumer<ElementResolver> doIfResolved) {
        UniqueId.Segment nextSegment = checkNotNull(segmentsToResolve.peekFirst());
        if (matches(segmentType, segmentValue).test(nextSegment)) {
            doIfResolved.accept(new ElementResolver(engineDescriptor, processedId.append(nextSegment), tail(segmentsToResolve)));
        }
    }

    private Predicate<UniqueId.Segment> matches(String segmentType, String segmentValue) {
        return nextSegment -> nextSegment.getType().equals(segmentType) && nextSegment.getValue().equals(segmentValue);
    }

    private void handleNewSegment(String segmentType, String segmentValue, Consumer<ElementResolver> doIfResolved) {
        doIfResolved.accept(new ElementResolver(engineDescriptor, processedId.append(segmentType, segmentValue), new LinkedList<>()));
    }

    static ElementResolver create(ArchUnitEngineDescriptor engineDescriptor, UniqueId rootId, UniqueId targetId) {
        return new ElementResolver(engineDescriptor, rootId, getRemainingSegments(rootId, targetId));
    }

    static ElementResolver create(ArchUnitEngineDescriptor engineDescriptor, UniqueId rootId, Class<?> testClass) {
        UniqueId targetId = rootId.append(CLASS_SEGMENT_TYPE, testClass.getName());
        return create(engineDescriptor, rootId, targetId);
    }

    static ElementResolver create(ArchUnitEngineDescriptor engineDescriptor, UniqueId rootId, Class<?> testClass, Method testMethod) {
        UniqueId targetId = rootId
                .append(CLASS_SEGMENT_TYPE, testClass.getName())
                .append(METHOD_SEGMENT_TYPE, testMethod.getName());
        return create(engineDescriptor, rootId, targetId);
    }

    static ElementResolver create(ArchUnitEngineDescriptor engineDescriptor, UniqueId rootId, Class<?> testClass, Field testField) {
        UniqueId targetId = rootId
                .append(CLASS_SEGMENT_TYPE, testClass.getName())
                .append(FIELD_SEGMENT_TYPE, testField.getName());
        return create(engineDescriptor, rootId, targetId);
    }

    private static Deque<UniqueId.Segment> getRemainingSegments(UniqueId rootId, UniqueId targetId) {
        Deque<UniqueId.Segment> remainingSegments = new LinkedList<>(targetId.getSegments());
        rootId.getSegments().forEach(segment -> {
            checkState(segment.equals(remainingSegments.peekFirst()),
                    "targetId %s should start with rootId %s", targetId, rootId);
            remainingSegments.pollFirst();
        });
        return remainingSegments;
    }

    abstract class PossiblyResolvedClass {
        void ifRequestedButUnresolved(BiConsumer<Class<?>, ElementResolver> doIfResolved) {
        }

        PossiblyResolvedClass ifRequestedAndResolved(BiConsumer<CreatesChildren, ElementResolver> doIfResolved) {
            return this;
        }
    }

    private class RequestedAndSuccessfullyResolvedClass extends PossiblyResolvedClass {
        private final CreatesChildren classDescriptor;
        private final ElementResolver childResolver;

        RequestedAndSuccessfullyResolvedClass(TestDescriptor classDescriptor, ElementResolver childResolver) {
            checkArgument(classDescriptor instanceof CreatesChildren,
                    "descriptor with uniqueId %s is expected to implement %s",
                    classDescriptor.getUniqueId(), CreatesChildren.class.getSimpleName());

            this.classDescriptor = (CreatesChildren) classDescriptor;
            this.childResolver = childResolver;
        }

        @Override
        RequestedAndSuccessfullyResolvedClass ifRequestedAndResolved(BiConsumer<CreatesChildren, ElementResolver> doIfResolved) {
            doIfResolved.accept(classDescriptor, childResolver);
            return this;
        }
    }

    private class RequestedButUnresolvedClass extends PossiblyResolvedClass {
        private final Class<?> clazz;
        private final ElementResolver childResolver;

        RequestedButUnresolvedClass(Class<?> clazz, ElementResolver childResolver) {
            this.clazz = clazz;
            this.childResolver = childResolver;
        }

        @Override
        void ifRequestedButUnresolved(BiConsumer<Class<?>, ElementResolver> doWithChildResolver) {
            doWithChildResolver.accept(clazz, childResolver);
        }
    }

    private class ClassNotRequested extends PossiblyResolvedClass {
    }

    abstract class PossiblyResolvedMember {
        abstract void ifUnresolved(Consumer<ElementResolver> childResolver);
    }

    private class SuccessfullyResolvedMember extends PossiblyResolvedMember {
        @Override
        void ifUnresolved(Consumer<ElementResolver> childResolver) {
        }
    }

    private class UnresolvedMember extends PossiblyResolvedMember {
        private final Member member;
        private String segmentType;

        UnresolvedMember(Member member, String segmentType) {
            this.member = member;
            this.segmentType = segmentType;
        }

        @Override
        void ifUnresolved(Consumer<ElementResolver> doWithChildResolver) {
            resolve(segmentType, member.getName(), doWithChildResolver);
        }
    }
}
