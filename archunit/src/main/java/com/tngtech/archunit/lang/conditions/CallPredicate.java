package com.tngtech.archunit.lang.conditions;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasOwner;
import com.tngtech.archunit.core.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.properties.HasParameterTypes;

import static com.tngtech.archunit.core.Formatters.formatMethod;
import static com.tngtech.archunit.core.JavaClass.namesOf;
import static com.tngtech.archunit.core.JavaClass.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withNameMatching;
import static com.tngtech.archunit.core.properties.HasParameterTypes.Predicates.withParameterTypes;

public class CallPredicate extends DescribedPredicate<JavaCall<?>> {
    private final CombinedCallPredicate predicate;
    private Modification<?> modification;

    private CallPredicate(CombinedCallPredicate predicate, Modification modification) {
        super(predicate.getDescription());
        this.predicate = predicate;
        this.modification = modification;
    }

    @Override
    public boolean apply(JavaCall<?> input) {
        return predicate.apply(input);
    }

    public CallPredicate hasName(String name) {
        return new CallPredicate(modification.modify(predicate, withNameMatching(name).as("has name '%s'", name)), modification);
    }

    public CallPredicate hasParameterTypes(Class<?>... paramTypes) {
        return hasParameterTypes(ImmutableList.copyOf(paramTypes));
    }

    public CallPredicate hasParameterTypes(final List<Class<?>> paramTypes) {
        DescribedPredicate<HasParameterTypes> hasParameterTypes = withParameterTypes(paramTypes.toArray(new Class[paramTypes.size()]));
        hasParameterTypes = hasParameterTypes
                .as(hasParameterTypes.getDescription().replace("with parameter types", "has parameter types"));
        return new CallPredicate(modification.modify(predicate, hasParameterTypes), modification);
    }

    public CallPredicate isConstructor() {
        return hasName(CONSTRUCTOR_NAME);
    }

    public CallPredicate isDeclaredIn(final DescribedPredicate<? super JavaClass> classIdentifier) {
        return new CallPredicate(ownerIs(classIdentifier), modification);
    }

    public CallPredicate isNotDeclaredIn(Class<?> targetClass) {
        return isDeclaredIn(not(declaredInPredicateFor(targetClass)));
    }

    public CallPredicate isDeclaredIn(Class<?> targetClass) {
        return isDeclaredIn(declaredInPredicateFor(targetClass));
    }

    private DescribedPredicate<JavaClass> declaredInPredicateFor(Class<?> targetClass) {
        return withType(targetClass).as("declared in " + targetClass.getSimpleName());
    }

    public CallPredicate isNotAssignableTo(Class<?> type) {
        return new CallPredicate(ownerIs(not(JavaClass.assignableTo(type))), modification);
    }

    public CallPredicate isAssignableTo(Class<?> type) {
        return new CallPredicate(ownerIs(JavaClass.assignableTo(type)), modification);
    }

    private CombinedCallPredicate ownerIs(DescribedPredicate<? super JavaClass> predicate) {
        return modification.modify(this.predicate, predicate.onResultOf(Get.<JavaClass>owner()));
    }

    public CallPredicate is(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return is(clazz, methodName, ImmutableList.copyOf(paramTypes));
    }

    public <T extends HasOwner<JavaClass> & HasName & HasParameterTypes> CallPredicate is(Class<?> owner, String methodName, List<Class<?>> paramTypes) {
        return matches(owner, methodName, namesOf(paramTypes));
    }

    public <T extends HasOwner<JavaClass> & HasName & HasParameterTypes> CallPredicate matches(Class<?> owner, String methodName, List<String> paramTypeNames) {
        DescribedPredicate<T> isDeclaredIn = declaredInPredicateFor(owner).onResultOf(Get.<JavaClass>owner()).forSubType();
        DescribedPredicate<T> hasName = withNameMatching(methodName).forSubType();
        DescribedPredicate<T> isPredicate = isDeclaredIn.and(hasName).and(withParameterTypes(paramTypeNames))
                .as(formatMethod(owner.getName(), methodName, paramTypeNames));

        // FIXME: It was a bad design decision to combine origin and target inside of this predicate,
        // will be changed when field access predicate is incorporated
        return new CallPredicate(modification.modify(predicate, (DescribedPredicate) isPredicate), modification);
    }

    public CallPredicate is(DescribedPredicate<? super JavaCodeUnit> predicate) {
        return new CallPredicate(modification.modify(this.predicate, (DescribedPredicate) predicate), modification);
    }

    @Override
    public CallPredicate as(String description, Object... params) {
        return new CallPredicate(predicate.as(description, params), modification);
    }

    public static CallPredicate target() {
        return new CallPredicate(new CombinedCallPredicate(), Modification.target());
    }

    public static CallPredicate origin() {
        return new CallPredicate(new CombinedCallPredicate(), Modification.origin());
    }

    private static <T> DescribedPredicate<T> predicateFrom(
            Optional<DescribedPredicate<T>> predicate) {
        return predicate.or(DescribedPredicate.<T>alwaysTrue());
    }

    private static abstract class Modification<T extends HasName & HasOwner<JavaClass> & HasParameterTypes> {
        abstract CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super T> addition);

        private static Modification<CodeUnitCallTarget> target() {
            return new Modification<CodeUnitCallTarget>() {
                @Override
                CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super CodeUnitCallTarget> addition) {
                    return predicate.andTarget(addition);
                }
            };
        }

        public static Modification<JavaCodeUnit> origin() {
            return new Modification<JavaCodeUnit>() {
                @Override
                CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super JavaCodeUnit> addition) {
                    return predicate.andOrigin(addition);
                }
            };
        }
    }

    private static class CombinedCallPredicate extends DescribedPredicate<JavaCall<?>> {
        private final Optional<DescribedPredicate<JavaCodeUnit>> originPredicate;
        private final Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate;
        private final DescribedPredicate<JavaCall<?>> combined;

        private CombinedCallPredicate() {
            this(Optional.<DescribedPredicate<JavaCodeUnit>>absent(),
                    Optional.<DescribedPredicate<CodeUnitCallTarget>>absent());
        }

        private CombinedCallPredicate(Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                      Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            this(combine(originPredicate, targetPredicate), originPredicate, targetPredicate);
        }

        private CombinedCallPredicate(String description,
                                      Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                      Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            this(description, combine(originPredicate, targetPredicate), originPredicate, targetPredicate);
        }

        private CombinedCallPredicate(DescribedPredicate<JavaCall<?>> combined,
                                      Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                      Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            this(combined.getDescription(), combined, originPredicate, targetPredicate);
        }

        private CombinedCallPredicate(String description, DescribedPredicate<JavaCall<?>> combined,
                                      Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                      Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            super(description);
            this.originPredicate = originPredicate;
            this.targetPredicate = targetPredicate;
            this.combined = combined;
        }

        @Override
        public boolean apply(JavaCall<?> input) {
            return combined.apply(input);
        }

        @Override
        public CombinedCallPredicate as(String description, Object... params) {
            return new CombinedCallPredicate(String.format(description, params), originPredicate, targetPredicate);
        }

        private static DescribedPredicate<JavaCall<?>> combine(final Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                                               final Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            return new DescribedPredicate<JavaCall<?>>(describe(originPredicate, targetPredicate)) {
                @Override
                public boolean apply(JavaCall<?> input) {
                    return predicateFrom(originPredicate).apply(input.getOrigin()) &&
                            predicateFrom(targetPredicate).apply(input.getTarget());
                }
            };
        }

        private static String describe(Optional<DescribedPredicate<JavaCodeUnit>> originPredicate,
                                       Optional<DescribedPredicate<CodeUnitCallTarget>> targetPredicate) {
            String originDescription = originPredicate.isPresent() ?
                    "origin is " + originPredicate.get().getDescription() : null;
            String targetDescription = targetPredicate.isPresent() ?
                    "target is " + targetPredicate.get().getDescription() : null;
            return Joiner.on(" and ").skipNulls().join(originDescription, targetDescription);
        }

        private CombinedCallPredicate andTarget(DescribedPredicate<? super CodeUnitCallTarget> predicate) {
            return new CombinedCallPredicate(originPredicate, Optional.of(and(targetPredicate, predicate)));
        }

        private CombinedCallPredicate andOrigin(DescribedPredicate<? super JavaCodeUnit> predicate) {
            return new CombinedCallPredicate(Optional.of(and(originPredicate, predicate)), targetPredicate);
        }

        private <T> DescribedPredicate<T> and(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> second) {
            return first.isPresent() ? first.get().and(second) : second.<T>forSubType();
        }
    }
}
