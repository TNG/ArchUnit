/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain.properties;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.ThrowsDeclarations;

import java.util.List;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.Formatters.formatThrowsDeclarationTypeNames;
import static com.tngtech.archunit.core.domain.ThrowsDeclaration.namesOf;
import static com.tngtech.archunit.core.domain.ThrowsDeclarations.GET_NAMES;

public interface HasThrowsDeclarations {
    @PublicAPI(usage = ACCESS)
    ThrowsDeclarations getThrowsDeclarations();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsDeclarations> throwsDeclarations(final Class<?>... types) {
            return throwsDeclarations(namesOf(types));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsDeclarations> throwsDeclarations(final String... types) {
            return throwsDeclarations(ImmutableList.copyOf(types));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsDeclarations> throwsDeclarations(final List<String> typeNames) {
            return throwsDeclarations(equalTo(typeNames).onResultOf(GET_NAMES)
                    .as("[%s]", formatThrowsDeclarationTypeNames(typeNames)));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsDeclarations> throwsDeclarations(final DescribedPredicate<ThrowsDeclarations> predicate) {
            return new ThrowsTypesPredicate(predicate);
        }

        private static class ThrowsTypesPredicate extends DescribedPredicate<HasThrowsDeclarations> {
            private final DescribedPredicate<ThrowsDeclarations> predicate;

            ThrowsTypesPredicate(DescribedPredicate<ThrowsDeclarations> predicate) {
                super("throws types " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(HasThrowsDeclarations input) {
                return predicate.apply(input.getThrowsDeclarations());
            }
        }
    }
}
