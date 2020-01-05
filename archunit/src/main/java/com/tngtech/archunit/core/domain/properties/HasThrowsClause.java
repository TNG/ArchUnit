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
package com.tngtech.archunit.core.domain.properties;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.Formatters.formatThrowsDeclarationTypeNames;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaClassList.GET_NAMES;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

public interface HasThrowsClause<LOCATION extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>> {
    @PublicAPI(usage = ACCESS)
    ThrowsClause<? extends LOCATION> getThrowsClause();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        @SafeVarargs
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseWithTypes(final Class<? extends Throwable>... types) {
            return throwsClauseWithTypes(namesOf(types));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseWithTypes(final String... typeNames) {
            return throwsClauseWithTypes(ImmutableList.copyOf(typeNames));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseWithTypes(final List<String> typeNames) {
            return throwsClause(equalTo(typeNames).onResultOf(ThrowsClause.Functions.GET_TYPES.then(GET_NAMES))
                    .as("[%s]", formatThrowsDeclarationTypeNames(typeNames)));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseContainingType(final Class<? extends Throwable> type) {
            return throwsClauseContainingType(type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseContainingType(final String typeName) {
            return throwsClauseContainingType(name(typeName).as(typeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClauseContainingType(DescribedPredicate<? super JavaClass> predicate) {
            DescribedPredicate<ThrowsDeclaration<?>> declarationPredicate = GET_RAW_TYPE.is(predicate).forSubType();
            return throwsClause(anyElementThat(declarationPredicate)).as("throws clause containing type " + predicate.getDescription());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasThrowsClause<?>> throwsClause(final DescribedPredicate<? super ThrowsClause<?>> predicate) {
            return new ThrowsTypesPredicate(predicate);
        }

        private static class ThrowsTypesPredicate extends DescribedPredicate<HasThrowsClause<?>> {
            private final DescribedPredicate<? super ThrowsClause<?>> predicate;

            ThrowsTypesPredicate(DescribedPredicate<? super ThrowsClause<?>> predicate) {
                super("throws types " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(HasThrowsClause<?> input) {
                return predicate.apply(input.getThrowsClause());
            }
        }
    }
}
