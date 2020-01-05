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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByClassesThat;

class OnlyBeAccessedSpecificationInternal implements OnlyBeAccessedSpecification<ClassesShouldConjunction> {
    private final ClassesShouldInternal classesShould;

    OnlyBeAccessedSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ClassesShouldConjunction byAnyPackage(String... packageIdentifiers) {
        return classesShould.addCondition(onlyBeAccessedByAnyPackage(packageIdentifiers));
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> byClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return classesShould.addCondition(ArchConditions.onlyBeAccessedByClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction byClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return classesShould.addCondition(onlyBeAccessedByClassesThat(predicate));
    }
}
