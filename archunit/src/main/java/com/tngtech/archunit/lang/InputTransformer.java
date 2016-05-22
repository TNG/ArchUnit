package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;

public interface InputTransformer<T> {
    <OUTPUT extends Iterable<T> & HasDescription> OUTPUT transform(JavaClasses collection);

    class Of {
        public static InputTransformer<JavaClass> predicate(final DescribedPredicate<JavaClass> predicate) {
            return new InputTransformer<JavaClass>() {
                @Override
                @SuppressWarnings("unchecked")
                public JavaClasses transform(JavaClasses collection) {
                    return collection.that(predicate);
                }
            };
        }
    }

}
