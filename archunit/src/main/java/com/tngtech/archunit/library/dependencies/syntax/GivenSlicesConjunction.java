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
package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.library.dependencies.Slice;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenSlicesConjunction extends GivenConjunction<Slice> {

    /**
     * Like {@link #should(ArchCondition)} but allows to pick the {@link ArchCondition} by a fluent API.
     */
    @PublicAPI(usage = ACCESS)
    SlicesShould should();

    @Override
    @PublicAPI(usage = ACCESS)
    GivenSlicesConjunction and(DescribedPredicate<? super Slice> predicate);

    @Override
    @PublicAPI(usage = ACCESS)
    GivenSlicesConjunction or(DescribedPredicate<? super Slice> predicate);

    /**
     * Customizes the description of the slices under test, i.e. the part before the
     * 'should' of the {@link ArchRule}. E.g.
     *
     * <pre><code>
     * slices().matching("..some.pattern.(*)..").as("My specific components").should()...
     * </code></pre>
     *
     * yields the rule text {@code My specific components should...}
     *
     * @param description The description of the slices within the slice rule
     * @return A syntax element, which can be used to restrict the classes under consideration
     *         or form a complete {@link ArchRule}
     */
    @PublicAPI(usage = ACCESS)
    GivenSlicesConjunction as(String description);
}
