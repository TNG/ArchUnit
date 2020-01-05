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
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface ClassesTransformer<T> extends HasDescription {
    /**
     * Defines how to transform imported {@link JavaClasses} to the respective objects to test.
     *
     * @param collection Imported {@link JavaClasses}
     * @return A {@link DescribedIterable} holding the transformed objects
     * @see com.tngtech.archunit.library.dependencies.Slices.Transformer
     */
    DescribedIterable<T> transform(JavaClasses collection);

    /**
     * Can be used to further filter the transformation result.
     *
     * @param predicate Predicate to filter the collection of transformed objects
     * @return A transformer that additionally filters the transformed result
     */
    ClassesTransformer<T> that(DescribedPredicate<? super T> predicate);

    /**
     * @param description A new description for this transformer
     * @return A transformer for the same transformation with an adjusted description
     */
    ClassesTransformer<T> as(String description);
}
