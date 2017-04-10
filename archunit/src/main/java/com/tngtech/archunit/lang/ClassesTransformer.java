package com.tngtech.archunit.lang;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasDescription;

public interface ClassesTransformer<T> extends HasDescription {
    /**
     * Defines how to transform imported {@link JavaClasses} to the respective objects to test.
     *
     * @param collection Imported {@link JavaClasses}
     * @return A {@link DescribedIterable} holding the transformed objects
     * @see com.tngtech.archunit.library.dependencies.Slices.Transformer
     */
    DescribedIterable<T> transform(JavaClasses collection);

    /**
     * Can be used to further filter the transformation result.
     *
     * @param predicate Predicate to filter the collection of transformed objects
     * @return A transformer that additionally filters the transformed result
     */
    ClassesTransformer<T> that(DescribedPredicate<? super T> predicate);

    /**
     * @param description A new description for this transformer
     * @return A transformer for the same transformation with an adjusted description
     */
    ClassesTransformer<T> as(String description);
}
