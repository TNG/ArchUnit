/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.conditions;

import java.util.EnumSet;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.accessType;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.getDescriptionTemplateFor;
import static java.util.Collections.singleton;

class FieldAccessCondition extends ArchCondition<JavaFieldAccess> {
    private final DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier;
    private final String descriptionTemplate;

    FieldAccessCondition(DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier) {
        this(fieldAccessIdentifier, EnumSet.allOf(AccessType.class));
    }

    FieldAccessCondition(DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier, Set<AccessType> accessTypes) {
        super(String.format("access field where %s and access type is one of %s",
                fieldAccessIdentifier.getDescription(), accessTypes));

        this.fieldAccessIdentifier = fieldAccessIdentifier;
        this.descriptionTemplate = getDescriptionTemplateFor(accessTypes);
    }

    @Override
    public void check(JavaFieldAccess item, ConditionEvents events) {
        String message = item.getDescriptionWithTemplate(descriptionTemplate);
        events.add(new SimpleConditionEvent(item, fieldAccessIdentifier.apply(item), message));
    }

    static class FieldGetAccessCondition extends FieldAccessCondition {
        FieldGetAccessCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(predicate.<JavaFieldAccess>forSubType().and(accessType(GET)), singleton(GET));
        }
    }

    static class FieldSetAccessCondition extends FieldAccessCondition {
        FieldSetAccessCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(predicate.<JavaFieldAccess>forSubType().and(accessType(SET)), singleton(SET));
        }
    }
}
