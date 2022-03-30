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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

class AnnotatedElementComposite implements AnnotatedElement {
    private final List<AnnotatedElement> children;

    private AnnotatedElementComposite(List<AnnotatedElement> children) {
        this.children = children.stream().map(AnnotationUtils::dereferenced).collect(Collectors.toList());
    }

    static AnnotatedElementComposite of(AnnotatedElement... elements) {
        return new AnnotatedElementComposite(Arrays.asList(elements));
    }

    List<AnnotatedElement> getChildren() {
        return children;
    }

    <T extends AnnotatedElement> Optional<T> findChild(Class<T> type) {
        return children.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findAny();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return children.stream()
                .map(child -> child.getAnnotation(annotationClass))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
        return extractAnnotations(AnnotatedElement::getAnnotations);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return extractAnnotations(AnnotatedElement::getDeclaredAnnotations);
    }

    private Annotation[] extractAnnotations(Function<AnnotatedElement, Annotation[]> extractor) {
        return children.stream()
                .map(extractor)
                .flatMap(Arrays::stream)
                .toArray(Annotation[]::new);
    }
}
