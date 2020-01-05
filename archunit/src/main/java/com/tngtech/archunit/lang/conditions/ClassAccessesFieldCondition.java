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
package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;

class ClassAccessesFieldCondition extends AnyAttributeMatchesCondition<JavaFieldAccess> {
    ClassAccessesFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
        this(new FieldAccessCondition(predicate));
    }

    ClassAccessesFieldCondition(FieldAccessCondition condition) {
        super(condition.getDescription(), condition);
    }

    @Override
    Collection<JavaFieldAccess> relevantAttributes(JavaClass item) {
        return item.getFieldAccessesFromSelf();
    }

    static class ClassGetsFieldCondition extends ClassAccessesFieldCondition {
        ClassGetsFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(new FieldGetAccessCondition(predicate));
        }
    }

    static class ClassSetsFieldCondition extends ClassAccessesFieldCondition {
        ClassSetsFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(new FieldSetAccessCondition(predicate));
        }
    }
}
