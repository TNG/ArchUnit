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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchConditions.fullyQualifiedName;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class SyntaxPredicates {
    static DescribedPredicate<HasName> haveNameNotMatching(String regex) {
        return have(not(nameMatching(regex)).as("name not matching '%s'", regex));
    }

    static DescribedPredicate<JavaClass> haveSimpleNameNotStartingWith(String prefix) {
        return have(not(simpleNameStartingWith(prefix)).as("simple name not starting with '%s'", prefix));
    }

    static DescribedPredicate<JavaClass> haveSimpleNameNotContaining(String infix) {
        return have(not(simpleNameContaining(infix)).as("simple name not containing '%s'", infix));
    }

    static DescribedPredicate<JavaClass> haveSimpleNameNotEndingWith(String suffix) {
        return have(not(simpleNameEndingWith(suffix)).as("simple name not ending with '%s'", suffix));
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

    static DescribedPredicate<HasModifiers> areStatic() {
        return modifier(STATIC).as("are static");
    }

    static DescribedPredicate<HasModifiers> areNotStatic() {
        return not(modifier(STATIC)).as("are not static");
    }

    static DescribedPredicate<HasModifiers> areFinal() {
        return modifier(FINAL).as("are final");
    }

    static DescribedPredicate<HasModifiers> areNotFinal() {
        return not(modifier(FINAL)).as("are not final");
    }

    static DescribedPredicate<HasName> haveFullyQualifiedName(String name) {
        return have(fullyQualifiedName(name));
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveFullyQualifiedName(String)}
     */
    @Deprecated
    static DescribedPredicate<HasName> dontHaveFullyQualifiedName(String name) {
        return dont(have(fullyQualifiedName(name)));
    }

    static DescribedPredicate<HasName> doNotHaveFullyQualifiedName(String name) {
        return doNot(have(fullyQualifiedName(name)));
    }

    static DescribedPredicate<JavaClass> haveSimpleName(String name) {
        return have(simpleName(name));
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveSimpleName(String)}
     */
    @Deprecated
    static DescribedPredicate<JavaClass> dontHaveSimpleName(String name) {
        return dont(have(simpleName(name)));
    }

    static DescribedPredicate<JavaClass> doNotHaveSimpleName(String name) {
        return doNot(have(simpleName(name)));
    }

    static DescribedPredicate<HasModifiers> haveModifier(JavaModifier modifier) {
        return have(modifier(modifier));
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveModifier(JavaModifier)}
     */
    @Deprecated
    static DescribedPredicate<HasModifiers> dontHaveModifier(JavaModifier modifier) {
        return dont(have(modifier(modifier)));
    }

    static DescribedPredicate<HasModifiers> doNotHaveModifier(JavaModifier modifier) {
        return doNot(have(modifier(modifier)));
    }
}
