package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.DescribedIterable;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.Guava;
import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClasses;

public abstract class InputTransformer<T> implements HasDescription {
    private String description;

    protected InputTransformer(String description) {
        this.description = description;
    }

    public final DescribedIterable<T> transform(JavaClasses collection) {
        return DescribedIterable.From.iterable(doTransform(collection), description);
    }

    public abstract Iterable<T> doTransform(JavaClasses collection);

    public InputTransformer<T> that(final DescribedPredicate<? super T> predicate) {
        return new InputTransformer<T>(description + " that " + predicate.getDescription()) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                Iterable<T> transformed = InputTransformer.this.doTransform(collection);
                return Guava.Iterables.filter(transformed, predicate);
            }
        };
    }

    @Override
    public String getDescription() {
        return description;
    }

    public InputTransformer<T> as(String description) {
        return new InputTransformer<T>(description) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                return InputTransformer.this.doTransform(collection);
            }
        };
    }
}
