package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.core.properties.HasModifiers;
import com.tngtech.archunit.core.properties.HasName;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.properties.HasName.Predicates.nameMatching;
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
        DescribedPredicate<JavaClass> haveSimpleName = have(simpleName(name));
        return not(haveSimpleName).as("don't " + haveSimpleName.getDescription());
    }

    static DescribedPredicate<HasModifiers> haveModifier(JavaModifier modifier) {
        return have(modifier(modifier));
    }

    static DescribedPredicate<HasModifiers> dontHaveModifier(JavaModifier modifier) {
        DescribedPredicate<HasModifiers> haveModifier = have(modifier(modifier));
        return not(haveModifier).as("don't " + haveModifier.getDescription());
    }
}
