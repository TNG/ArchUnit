/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer.resolvers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.tngtech.archunit.base.ArchUnitException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;

/**
 * A {@link ClassResolver} that tries to locate missing dependencies on the classpath.
 * I.e. uses {@link Class#getResource(String)} to find the {@link URI} of the classfile for the missing
 * type, then uses the supplied {@link ClassResolver.ClassUriImporter} to import the type.
 */
@MayResolveTypesViaReflection(reason = "This is a dedicated option to resolve further dependencies from the classpath")
public final class ClassResolverFromClasspath implements ClassResolver {
    private ClassUriImporter classUriImporter;

    @Override
    public void setClassUriImporter(ClassUriImporter classUriImporter) {
        this.classUriImporter = checkNotNull(classUriImporter,
                "%s may not be null", ClassUriImporter.class.getSimpleName());
    }

    @Override
    public Optional<JavaClass> tryResolve(String typeName) {
        String typeFile = typeName.replace(".", "/") + ".class";

        Optional<URI> uri = tryGetUriOf(typeFile);

        return uri.isPresent() ? classUriImporter.tryImport(uri.get()) : Optional.<JavaClass>absent();
    }

    private Optional<URI> tryGetUriOf(String typeFile) {
        URL resource = getCurrentClassLoader(getClass()).getResource(typeFile);
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
