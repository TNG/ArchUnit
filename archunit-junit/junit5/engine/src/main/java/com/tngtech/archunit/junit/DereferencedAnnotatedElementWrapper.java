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
import java.util.stream.Stream;

class DereferencedAnnotatedElementWrapper implements AnnotatedElement {

    private final AnnotatedElement target;

    DereferencedAnnotatedElementWrapper(AnnotatedElement target) {
        this.target = target;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T result = target.getAnnotation(annotationClass);
        if (result != null) {
            return result;
        }
        return Arrays.stream(getAnnotations())
                .filter(annotationClass::isInstance)
                .findAny()
                .map(annotationClass::cast)
                .orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
        return Stream.of(target.getAnnotations())
                .map(AnnotationUtils::dereferenceAnnotation)
                .toArray(Annotation[]::new);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return Stream.of(target.getDeclaredAnnotations())
                .map(AnnotationUtils::dereferenceAnnotation)
                .toArray(Annotation[]::new);
    }
}
