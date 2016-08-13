package com.tngtech.archunit.lang.conditions;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.Optional;

import static com.tngtech.archunit.core.Formatters.formatMethod;
import static com.tngtech.archunit.core.JavaClass.REFLECT;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaMember.GET_OWNER;

public class CallPredicate extends DescribedPredicate<JavaCall<?>> {
    private final CombinedCallPredicate predicate;
    private Modification modification;

    CallPredicate(CombinedCallPredicate predicate, Modification modification) {
        super(predicate.getDescription());
        this.predicate = predicate;
        this.modification = modification;
    }

    @Override
    public boolean apply(JavaCall<?> input) {
        return predicate.apply(input);
    }

    public CallPredicate hasName(String name) {
        return new CallPredicate(modification.modify(predicate, JavaCodeUnit.hasName(name)), modification);
    }

    public CallPredicate hasParameters(Class<?>... paramTypes) {
        return hasParameters(ImmutableList.copyOf(paramTypes));
    }

    public CallPredicate hasParameters(final List<Class<?>> paramTypes) {
        return new CallPredicate(modification.modify(predicate, JavaCodeUnit.hasParameters(paramTypes)), modification);
    }

    public CallPredicate isConstructor() {
        return hasName(CONSTRUCTOR_NAME);
    }

    public CallPredicate isDeclaredIn(final DescribedPredicate<JavaClass> classIdentifier) {
        return new CallPredicate(ownerIs(classIdentifier), modification);
    }

    public CallPredicate isNotDeclaredIn(Class<?> targetClass) {
        return isDeclaredIn(not(declaredInPredicateFor(targetClass)));
    }

    public CallPredicate isDeclaredIn(Class<?> targetClass) {
        return isDeclaredIn(declaredInPredicateFor(targetClass));
    }

    private DescribedPredicate<JavaClass> declaredInPredicateFor(Class<?> targetClass) {
        return DescribedPredicate.<Class<?>>equalTo(targetClass).onResultOf(REFLECT)
                .as("declared in " + targetClass.getSimpleName());
    }

    public CallPredicate isNotAssignableTo(Class<?> type) {
        return new CallPredicate(ownerIs(not(JavaClass.assignableTo(type))), modification);
    }

    public CallPredicate isAssignableTo(Class<?> type) {
        return new CallPredicate(ownerIs(JavaClass.assignableTo(type)), modification);
    }

    private CombinedCallPredicate ownerIs(DescribedPredicate<JavaClass> predicate) {
        return modification.modify(this.predicate, predicate.onResultOf(GET_OWNER));
    }

    public CallPredicate is(Class<?> clazz, String methodName, List<Class<?>> paramTypes) {
        return isDeclaredIn(clazz).hasName(methodName).hasParameters(paramTypes)
                .as("is %s", formatMethod(clazz.getName(), methodName, paramTypes));
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

    private static DescribedPredicate<JavaCodeUnit<?, ?>> predicateFrom(
            Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> predicate) {
        return predicate.or(DescribedPredicate.<JavaCodeUnit<?, ?>>alwaysTrue());
    }

    private static abstract class Modification {
        abstract CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super JavaCodeUnit<?, ?>> addition);

        private static Modification target() {
            return new Modification() {
                @Override
                CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super JavaCodeUnit<?, ?>> addition) {
                    return predicate.andTarget(addition);
                }
            };
        }

        public static Modification origin() {
            return new Modification() {
                @Override
                CombinedCallPredicate modify(CombinedCallPredicate predicate, DescribedPredicate<? super JavaCodeUnit<?, ?>> addition) {
                    return predicate.andOrigin(addition);
                }
            };
        }
    }

    private static class CombinedCallPredicate extends DescribedPredicate<JavaCall<?>> {
        private final Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate;
        private final Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate;
        private final DescribedPredicate<JavaCall<?>> combined;

        public CombinedCallPredicate() {
            this(Optional.<DescribedPredicate<JavaCodeUnit<?, ?>>>absent(),
                    Optional.<DescribedPredicate<JavaCodeUnit<?, ?>>>absent());
        }

        public CombinedCallPredicate(Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                     Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
            this(combine(originPredicate, targetPredicate), originPredicate, targetPredicate);
        }

        public CombinedCallPredicate(String description,
                                     Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                     Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
            this(description, combine(originPredicate, targetPredicate), originPredicate, targetPredicate);
        }

        private CombinedCallPredicate(DescribedPredicate<JavaCall<?>> combined,
                                      Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                      Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
            this(combined.getDescription(), combined, originPredicate, targetPredicate);
        }

        private CombinedCallPredicate(String description, DescribedPredicate<JavaCall<?>> combined,
                                      Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                      Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
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

        private static DescribedPredicate<JavaCall<?>> combine(final Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                                               final Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
            return new DescribedPredicate<JavaCall<?>>(describe(originPredicate, targetPredicate)) {
                @Override
                public boolean apply(JavaCall<?> input) {
                    return predicateFrom(originPredicate).apply(input.getOrigin()) &&
                            predicateFrom(targetPredicate).apply(input.getTarget());
                }
            };
        }

        private static String describe(Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> originPredicate,
                                       Optional<DescribedPredicate<JavaCodeUnit<?, ?>>> targetPredicate) {
            String originDescription = originPredicate.isPresent() ?
                    "origin is " + originPredicate.get().getDescription() : null;
            String targetDescription = targetPredicate.isPresent() ?
                    "target is " + targetPredicate.get().getDescription() : null;
            return Joiner.on(" and ").skipNulls().join(originDescription, targetDescription);
        }

        public CombinedCallPredicate andTarget(DescribedPredicate<? super JavaCodeUnit<?, ?>> predicate) {
            return new CombinedCallPredicate(originPredicate, Optional.of(predicateFrom(targetPredicate).and(predicate)));
        }

        public CombinedCallPredicate andOrigin(DescribedPredicate<? super JavaCodeUnit<?, ?>> predicate) {
            return new CombinedCallPredicate(Optional.of(predicateFrom(originPredicate).and(predicate)), targetPredicate);
        }
    }
}
