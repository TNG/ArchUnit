package com.tngtech.archunit.core.importer.resolvers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.tngtech.archunit.base.ArchUnitException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaClass;

@MayResolveTypesViaReflection(reason = "This is a dedicated option to resolve further dependencies from the classpath")
public final class ClassResolverFromClassPath implements ClassResolver {
    private ClassUriImporter classUriImporter;

    @Override
    public void setClassUriImporter(ClassUriImporter classUriImporter) {
        this.classUriImporter = classUriImporter;
    }

    @Override
    public Optional<JavaClass> tryResolve(String typeName) {
        String typeFile = "/" + typeName.replace(".", "/") + ".class";

        Optional<URI> uri = tryGetUriOf(typeFile);

        return uri.isPresent() ? classUriImporter.tryImport(uri.get()) : Optional.<JavaClass>absent();
    }

    private Optional<URI> tryGetUriOf(String typeFile) {
        URL resource = getClass().getResource(typeFile);
        if (resource == null) {
            return Optional.absent();
        }
        try {
            return Optional.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new ArchUnitException.LocationException(e);
        }
    }
}
