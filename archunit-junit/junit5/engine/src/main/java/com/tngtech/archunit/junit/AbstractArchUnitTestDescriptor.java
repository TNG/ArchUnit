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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.junit.NamespacedStore.NamespacedKey;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

abstract class AbstractArchUnitTestDescriptor extends AbstractTestDescriptor implements Node<ArchUnitEngineExecutionContext> {
    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    private final AnnotatedElementComposite annotatedElement;
    private final Map<NamespacedKey, Object> store;

    AbstractArchUnitTestDescriptor(UniqueId uniqueId, String displayName, TestSource source, AnnotatedElement... annotatedElements) {
        super(uniqueId, displayName, source);
        this.annotatedElement = AnnotatedElementComposite.of(annotatedElements);
        this.store = new ConcurrentHashMap<>();
    }

    private boolean shouldBeUnconditionallyIgnored() {
        return streamAnnotations(annotatedElement, ArchIgnore.class, Disabled.class)
                .findFirst()
                .isPresent();
    }

    private Set<TestTag> findTagsOn(AnnotatedElementComposite annotatedElement) {
        return streamRepeatableAnnotations(annotatedElement, ArchTag.class, Tag.class)
                .map(annotation -> TestTag.create(ReflectionUtils.invokeMethod(annotation, "value")))
                .collect(toSet());
    }

    @Override
    public ArchUnitEngineExecutionContext prepare(ArchUnitEngineExecutionContext context) throws Exception {
        getExtensionsFromTestSource().forEach(context::registerExtension);
        return context;
    }

    @SuppressWarnings("unchecked")
    protected Collection<Extension> getExtensionsFromTestSource() {
        return ((Stream<ExtendWith>) streamRepeatableAnnotations(annotatedElement, ExtendWith.class))
                .map(ExtendWith::value)
                .flatMap(Arrays::stream)
                .map(ReflectionUtils::newInstanceOf)
                .collect(Collectors.toList());
    }

    @Override
    public SkipResult shouldBeSkipped(ArchUnitEngineExecutionContext context) {
        if (shouldBeUnconditionallyIgnored()) {
            return SkipResult.skip("Ignored using @Disabled / @ArchIgnore");
        }
        return toSkipResult(conditionEvaluator.evaluate(
                context,
                new ArchUnitExtensionContext(this, context))
        );
    }

    @Override
    public Set<TestTag> getTags() {
        Set<TestTag> result = findTagsOn(annotatedElement);
        result.addAll(getParent().map(TestDescriptor::getTags).orElse(emptySet()));
        return result;
    }

    private SkipResult toSkipResult(ConditionEvaluationResult evaluationResult) {
        if (evaluationResult.isDisabled()) {
            return SkipResult.skip(evaluationResult.getReason().orElse("<unknown>"));
        }
        return SkipResult.doNotSkip();
    }

    @SafeVarargs
    private static Stream<? extends Annotation> streamRepeatableAnnotations(AnnotatedElementComposite element, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations)
                .flatMap(annotationType ->
                        element.getChildren().stream()
                                .map(child -> AnnotationSupport.findRepeatableAnnotations(child, annotationType)))
                .flatMap(Collection::stream);
    }

    @SafeVarargs
    private static Stream<? extends Annotation> streamAnnotations(AnnotatedElementComposite element, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations)
                .flatMap(annotationType ->
                        element.getChildren().stream()
                                .map(child -> AnnotationSupport.findAnnotation(child, annotationType)))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    AnnotatedElementComposite getAnnotatedElement() {
        return annotatedElement;
    }

    Map<NamespacedKey, Object> getStore() {
        return store;
    }

}
