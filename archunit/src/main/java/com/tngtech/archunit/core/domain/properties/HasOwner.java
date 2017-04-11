package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasOwner<T> {
    @PublicAPI(usage = ACCESS)
    T getOwner();

    @PublicAPI(usage = ACCESS)
    final class Predicates {
        private Predicates() {
        }

        public static final class With {
            private With() {
            }

            @PublicAPI(usage = ACCESS)
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

    @PublicAPI(usage = ACCESS)
    final class Functions {
        private Functions() {
        }

        public static final class Get {
            private Get() {
            }

            @PublicAPI(usage = ACCESS)
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
