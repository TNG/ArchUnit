package com.tngtech.archunit.lang;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.Guava;
import com.tngtech.archunit.core.JavaClasses;

public abstract class AbstractClassesTransformer<T> implements ClassesTransformer<T> {
    private String description;

    protected AbstractClassesTransformer(String description) {
        this.description = description;
    }

    @Override
    public final DescribedIterable<T> transform(JavaClasses collection) {
        return DescribedIterable.From.iterable(doTransform(collection), description);
    }

    public abstract Iterable<T> doTransform(JavaClasses collection);

    @Override
    public ClassesTransformer<T> that(final DescribedPredicate<? super T> predicate) {
        return new AbstractClassesTransformer<T>(description + " that " + predicate.getDescription()) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                Iterable<T> transformed = AbstractClassesTransformer.this.doTransform(collection);
                return Guava.Iterables.filter(transformed, predicate);
            }
        };
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ClassesTransformer<T> as(String description) {
        return new AbstractClassesTransformer<T>(description) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                return AbstractClassesTransformer.this.doTransform(collection);
            }
        };
    }
}
