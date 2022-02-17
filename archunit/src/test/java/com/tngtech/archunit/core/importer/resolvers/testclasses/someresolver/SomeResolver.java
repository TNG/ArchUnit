package com.tngtech.archunit.core.importer.resolvers.testclasses.someresolver;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

@SuppressWarnings("unused") // Only targeted via Reflection
public class SomeResolver implements ClassResolver {
    @Override
    public void setClassUriImporter(ClassUriImporter classUriImporter) {
    }

    @Override
    public Optional<JavaClass> tryResolve(String typeName) {
        return null;
    }
}
