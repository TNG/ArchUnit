package com.tngtech.archunit.core;

import java.util.Iterator;

public interface DescribedIterable<T> extends Iterable<T>, HasDescription {
    class From {
        public static <T> DescribedIterable<T> iterable(final Iterable<T> iterable, final String description) {
            return new DescribedIterable<T>() {
                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public Iterator<T> iterator() {
                    return iterable.iterator();
                }
            };
        }
    }
}
