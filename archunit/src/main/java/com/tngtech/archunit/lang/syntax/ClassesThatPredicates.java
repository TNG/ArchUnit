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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class ClassesThatPredicates {
    static DescribedPredicate<HasName> haveNameNotMatching(String regex) {
        return have(not(nameMatching(regex)).as("name not matching '%s'", regex));
    }

    static DescribedPredicate<HasModifiers> arePublic() {
        return modifier(PUBLIC).as("are public");
    }

    static DescribedPredicate<HasModifiers> areNotPublic() {
        return not(modifier(PUBLIC)).as("are not public");
    }

    static DescribedPredicate<HasModifiers> areProtected() {
        return modifier(PROTECTED).as("are protected");
    }

    static DescribedPredicate<HasModifiers> areNotProtected() {
        return not(modifier(PROTECTED)).as("are not protected");
    }

    static DescribedPredicate<HasModifiers> arePackagePrivate() {
        return not(modifier(PUBLIC).or(modifier(PROTECTED).or(modifier(PRIVATE)))).as("are package private");
    }

    static DescribedPredicate<HasModifiers> areNotPackagePrivate() {
        return modifier(PUBLIC).or(modifier(PROTECTED).or(modifier(PRIVATE))).as("are not package private");
    }

    static DescribedPredicate<HasModifiers> arePrivate() {
        return modifier(PRIVATE).as("are private");
    }

    static DescribedPredicate<HasModifiers> areNotPrivate() {
        return not(modifier(PRIVATE)).as("are not private");
    }

    static DescribedPredicate<HasName> areNamed(String name) {
        return are(named(name));
    }

    private static DescribedPredicate<HasName> named(String name) {
        return name(name).as("named '%s'", name);
    }

    static DescribedPredicate<HasName> areNotNamed(String name) {
        return are(not(named(name)));
    }

    static DescribedPredicate<JavaClass> haveSimpleName(String name) {
        return have(simpleName(name));
    }

    static DescribedPredicate<JavaClass> dontHaveSimpleName(String name) {
        return dont(have(simpleName(name)));
    }

    static DescribedPredicate<HasModifiers> haveModifier(JavaModifier modifier) {
        return have(modifier(modifier));
    }

    static DescribedPredicate<HasModifiers> dontHaveModifier(JavaModifier modifier) {
        return dont(have(modifier(modifier)));
    }

    // Conscious copy to keep visibility reduced -> ArchConditions
    static DescribedPredicate<JavaClass> implementPredicate(DescribedPredicate<JavaClass> assignablePredicate) {
        return not(INTERFACES).and(assignablePredicate)
                .as(assignablePredicate.getDescription().replace("assignable to", "implement"));
    }
}
