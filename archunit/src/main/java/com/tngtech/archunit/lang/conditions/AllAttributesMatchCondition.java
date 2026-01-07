/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.Collection;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;

abstract class AllAttributesMatchCondition<ATTRIBUTE, OWNER> extends ArchCondition<OWNER> {
    private final ArchCondition<ATTRIBUTE> condition;

    AllAttributesMatchCondition(String description, ArchCondition<ATTRIBUTE> condition) {
        super(description);
        this.condition = condition;
    }

    @Override
    public final void check(OWNER item, ConditionEvents events) {
        containOnlyElementsThat(condition).check(relevantAttributes(item), events);
    }

    abstract Collection<? extends ATTRIBUTE> relevantAttributes(OWNER item);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }
}
