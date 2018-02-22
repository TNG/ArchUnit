package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;

@Retention(RetentionPolicy.RUNTIME)
@interface LocationOfClass {
    Class<?> value();

    class Provider implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Locations.ofClass(testClass.getAnnotation(LocationOfClass.class).value());
        }
    }
}
