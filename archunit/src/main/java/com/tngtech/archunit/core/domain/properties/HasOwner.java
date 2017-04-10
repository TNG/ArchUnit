package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

public interface HasOwner<T> {
    T getOwner();

    class Predicates {
        public static class With {
            public static <T> DescribedPredicate<HasOwner<T>> owner(final DescribedPredicate<? super T> predicate) {
                return new DescribedPredicate<HasOwner<T>>("owner " + predicate.getDescription()) {
                    @Override
                    public boolean apply(HasOwner<T> input) {
                        return predicate.apply(input.getOwner());
                    }
                };
            }
        }
    }

    class Functions {
        public static class Get {
            public static <T> ChainableFunction<HasOwner<T>, T> owner() {
                return new ChainableFunction<HasOwner<T>, T>() {
                    @Override
                    public T apply(HasOwner<T> input) {
                        return input.getOwner();
                    }
                };
            }
        }
    }
}
