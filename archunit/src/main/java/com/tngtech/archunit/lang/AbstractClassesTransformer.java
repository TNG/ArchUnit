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
package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Default base implementation of {@link ClassesTransformer}, where only {@link #doTransform(JavaClasses)}
 * has to be implemented, while description and filtering via {@link #that(DescribedPredicate)} are provided.
 */
@PublicAPI(usage = INHERITANCE)
public abstract class AbstractClassesTransformer<T> implements ClassesTransformer<T> {
    private final String description;

    protected AbstractClassesTransformer(String description) {
        this.description = description;
    }

    @Override
    public final DescribedIterable<T> transform(JavaClasses collection) {
        return DescribedIterable.From.iterable(doTransform(collection), description);
    }

    public abstract Iterable<T> doTransform(JavaClasses collection);

    @Override
    public final ClassesTransformer<T> that(final DescribedPredicate<? super T> predicate) {
        return new AbstractClassesTransformer<T>(description + " that " + predicate.getDescription()) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                Iterable<T> transformed = AbstractClassesTransformer.this.doTransform(collection);
                return Guava.Iterables.filter(transformed, predicate);
            }
        };
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final ClassesTransformer<T> as(String description) {
        return new AbstractClassesTransformer<T>(description) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                return AbstractClassesTransformer.this.doTransform(collection);
            }
        };
    }

    @Override
    public String toString() {
        return ClassesTransformer.class.getSimpleName() + "{" + getDescription() + "}";
    }
}
