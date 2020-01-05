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

import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaModifier;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasModifiers {
    @PublicAPI(usage = ACCESS)
    Set<JavaModifier> getModifiers();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasModifiers> modifier(final JavaModifier modifier) {
            return new ModifierPredicate(modifier);
        }

        private static class ModifierPredicate extends DescribedPredicate<HasModifiers> {
            private final JavaModifier modifier;

            ModifierPredicate(JavaModifier modifier) {
                super("modifier " + modifier);
                this.modifier = modifier;
            }

            @Override
            public boolean apply(HasModifiers input) {
                return input.getModifiers().contains(modifier);
            }
        }
    }
}
