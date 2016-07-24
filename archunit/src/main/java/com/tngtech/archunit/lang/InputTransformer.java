package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClasses;

public abstract class InputTransformer<T> implements HasDescription {
    private String description;

    protected InputTransformer(String description) {
        this.description = description;
    }

    public abstract <OUTPUT extends Iterable<T> & HasDescription> OUTPUT transform(JavaClasses collection);

    public InputTransformer<T> that(DescribedPredicate<T> predicate) {
        return new InputTransformer<T>(description + " that " + predicate.getDescription()) {
            @Override
            public <OUTPUT extends Iterable<T> & HasDescription> OUTPUT transform(JavaClasses collection) {
                return InputTransformer.this.transform(collection);
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
            public <OUTPUT extends Iterable<T> & HasDescription> OUTPUT transform(JavaClasses collection) {
                return InputTransformer.this.transform(collection);
            }
        };
    }
}
