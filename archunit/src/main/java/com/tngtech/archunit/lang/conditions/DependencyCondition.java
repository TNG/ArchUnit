/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class DependencyCondition extends ArchCondition<Dependency> {
    private final DescribedPredicate<? super Dependency> conditionPredicate;

    DependencyCondition(DescribedPredicate<? super Dependency> conditionPredicate) {
        super(conditionPredicate.getDescription());
        this.conditionPredicate = conditionPredicate;
    }

    @Override
    public void check(Dependency item, ConditionEvents events) {
        events.add(new SimpleConditionEvent(item, conditionPredicate.apply(item), item.getDescription()));
    }
}
