package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.properties.HasOwner.Predicates.With;

import static com.tngtech.archunit.core.properties.HasName.Predicates.withName;

public class ArchPredicates {
    private ArchPredicates() {
    }

    public static DescribedPredicate<JavaFieldAccess> ownerAndNameAre(final Class<?> target, final String fieldName) {
        return new DescribedPredicate<JavaFieldAccess>(
                String.format("owner is %s and name is '%s'", target.getName(), fieldName)) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return fieldAccessTarget(With.<JavaClass>owner(withName(target.getName()))).apply(input) &&
                        fieldName.equals(input.getTarget().getName());
            }
        };
    }

    private static DescribedPredicate<JavaFieldAccess> fieldAccessTarget(final DescribedPredicate<? super FieldAccessTarget> predicate) {
        return new DescribedPredicate<JavaFieldAccess>("field access target " + predicate.getDescription()) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return predicate.apply(input.getTarget());
            }
        };
    }

    /**
     * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
     * characters, '..' stands for any sequence of packages.
     * For further details see {@link PackageMatcher}.
     *
     * @param packageIdentifier A string representing the identifier to match packages against
     * @return A {@link DescribedPredicate} returning true iff the package of the
     * tested {@link JavaClass} matches the identifier
     */
    public static DescribedPredicate<JavaFieldAccess> targetTypeResidesIn(String packageIdentifier) {
        final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

        return new DescribedPredicate<JavaFieldAccess>("target type resides in '%s'", packageIdentifier) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                JavaClass fieldType = input.getTarget().getType();
                return packageMatcher.matches(fieldType.getPackage());
            }
        };
    }

    public static DescribedPredicate<JavaCall<?>> callTarget(final DescribedPredicate<? super CodeUnitCallTarget> predicate) {
        return new DescribedPredicate<JavaCall<?>>("call target " + predicate) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return predicate.apply(input.getTarget());
            }
        };
    }

    public static CallPredicate callTarget() {
        return CallPredicate.target();
    }

    public static DescribedPredicate<JavaCall<?>> callOrigin(final DescribedPredicate<? super JavaCodeUnit> predicate) {
        return new DescribedPredicate<JavaCall<?>>("call origin " + predicate.getDescription()) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return predicate.apply(input.getOrigin());
            }
        };
    }

    public static CallPredicate callOrigin() {
        return CallPredicate.origin();
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> is(DescribedPredicate<T> predicate) {
        return predicate.as("is " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(are(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> are(DescribedPredicate<T> predicate) {
        return predicate.as("are " + predicate.getDescription());
    }

    public static DescribedPredicate<JavaFieldAccess> accessOrigin(final DescribedPredicate<? super JavaCodeUnit> predicate) {
        return new DescribedPredicate<JavaFieldAccess>("access origin " + predicate.getDescription()) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return predicate.apply(input.getOrigin());
            }
        };
    }
}
