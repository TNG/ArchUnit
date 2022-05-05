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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeCalledSpecification;

class OnlyBeCalledSpecificationInternal<SHOULD extends AbstractMembersShouldInternal<? extends JavaCodeUnit, SHOULD>> implements OnlyBeCalledSpecification<SHOULD> {
    private final SHOULD codeUnitsShould;

    OnlyBeCalledSpecificationInternal(SHOULD codeUnitsShould) {
        this.codeUnitsShould = codeUnitsShould;
    }

    @Override
    public SHOULD byClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return codeUnitsShould.addCondition(ArchConditions.onlyBeCalledByClassesThat(predicate));
    }

    @Override
    public ClassesThat<SHOULD> byClassesThat() {
        return new ClassesThatInternal<>(predicate -> codeUnitsShould.addCondition(ArchConditions.onlyBeCalledByClassesThat(predicate)));
    }

    @Override
    public SHOULD byCodeUnitsThat(DescribedPredicate<? super JavaMember> predicate) {
        return codeUnitsShould.addCondition(ArchConditions.onlyBeCalledByCodeUnitsThat(predicate));
    }

    @Override
    public SHOULD byMethodsThat(DescribedPredicate<? super JavaMember> predicate) {
        return codeUnitsShould.addCondition(ArchConditions.onlyBeCalledByMethodsThat(predicate));
    }

    @Override
    public SHOULD byConstructorsThat(DescribedPredicate<? super JavaMember> predicate) {
        return codeUnitsShould.addCondition(ArchConditions.onlyBeCalledByConstructorsThat(predicate));
    }
}
