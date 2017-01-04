package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.DescribedIterable;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.Guava;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.properties.HasDescription;

public abstract class ClassesTransformer<T> implements HasDescription {
    private String description;

    protected ClassesTransformer(String description) {
        this.description = description;
    }

    public final DescribedIterable<T> transform(JavaClasses collection) {
        return DescribedIterable.From.iterable(doTransform(collection), description);
    }

    public abstract Iterable<T> doTransform(JavaClasses collection);

    public ClassesTransformer<T> that(final DescribedPredicate<? super T> predicate) {
        return new ClassesTransformer<T>(description + " that " + predicate.getDescription()) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                Iterable<T> transformed = ClassesTransformer.this.doTransform(collection);
                return Guava.Iterables.filter(transformed, predicate);
            }
        };
    }

    @Override
    public String getDescription() {
        return description;
    }

    public ClassesTransformer<T> as(String description) {
        return new ClassesTransformer<T>(description) {
            @Override
            public Iterable<T> doTransform(JavaClasses collection) {
                return ClassesTransformer.this.doTransform(collection);
            }
        };
    }
}
