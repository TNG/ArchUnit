package com.tngtech.archunit.base;

import java.util.Iterator;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface DescribedIterable<T> extends Iterable<T>, HasDescription {
    final class From {
        private From() {
        }

        @PublicAPI(usage = ACCESS)
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
