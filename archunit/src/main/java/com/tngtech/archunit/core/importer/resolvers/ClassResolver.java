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

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.ClassResolverConfigurationException;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * The {@link ClassFileImporter} will use the configured {@link ClassResolver}, to determine how to
 * resolve further dependencies.<br><br>
 * For example, if you import the package <code>com.foo.bar</code>, and some class <code>com.foo.bar.FooBar</code>
 * calls a method of <code>com.other.Dependency</code>, the {@link ClassResolver} will decide how this dependency should
 * be treated, i.e. should the class tried to be located somehow, e.g. on the classpath, or should instead a stub
 * with the respective type name be created.<br><br>
 * Before any call of {@link #tryResolve(String)}, ArchUnit will always call
 * {@link #setClassUriImporter(ClassUriImporter)} and supply a respective {@link ClassUriImporter ClassUriImporter}.
 * Thus the job of {@link ClassResolver} is just, to resolve the correct {@link URI}, where to locate the class.
 * The {@link ClassUriImporter ClassUriImporter} can then import any given {@link URI} as a {@link JavaClass}.
 */
@PublicAPI(usage = INHERITANCE)
public interface ClassResolver {
    /**
     * Always called BEFORE {@link #tryResolve(String)}.
     *
     * @param classUriImporter to import a {@link JavaClass} from any supplied {@link URI}
     */
    void setClassUriImporter(ClassUriImporter classUriImporter);

    /**
     * ArchUnit will call this method, to resolve any missing {@link JavaClass JavaClasses}, needed to
     * build the class graph (i.e. targets of method calls, field accesses, super classes, interfaces, ...)
     *
     * @param typeName The type name to resolve as {@link JavaClass}
     * @return Optional.of(resolvedClass), if the {@link JavaClass} could be successfully imported,
     * otherwise Optional.absent()
     */
    Optional<JavaClass> tryResolve(String typeName);

    /**
     * Provides a way to import a JavaClass from a given {@link URI}.
     *
     * @see #tryImport(URI)
     */
    @PublicAPI(usage = ACCESS)
    interface ClassUriImporter {
        /**
         * Try to import a {@link JavaClass} from the given {@link URI}, i.e. open a stream and use the default
         * core import, to create a {@link JavaClass} from it.<br><br>
         * NOTE: {@link ClassUriImporter ClassUriImporter} has to be resilient against errors during import, e.g.
         * {@link java.io.IOException IOExceptions} or {@link java.net.MalformedURLException MalformedURLExceptions}.
         * Errors while reading from the given {@link URI} will always result in an Optional.absent() return value,
         * no need to catch {@link Exception Exceptions}.
         *
         * @param uri The {@link URI} to import a {@link JavaClass} from
         * @return Optional.of(importedClass), if the {@link JavaClass} could be successfully imported,
         * otherwise Optional.absent()
         */
        @PublicAPI(usage = ACCESS)
        Optional<JavaClass> tryImport(URI uri);
    }

    @Internal
    final class Factory {
        public ClassResolver create() {
            Optional<ClassResolver> resolver = getExplicitlyConfiguredClassResolver();
            if (resolver.isPresent()) {
                return resolver.get();
            }

            boolean resolveFromClasspath = ArchConfiguration.get().resolveMissingDependenciesFromClassPath();
            return resolveFromClasspath ?
                    new ClassResolverFromClasspath() :
                    new NoOpClassResolver();
        }

        private Optional<ClassResolver> getExplicitlyConfiguredClassResolver() {
            Optional<String> resolverClassName = ArchConfiguration.get().getClassResolver();
            if (!resolverClassName.isPresent()) {
                return Optional.absent();
            }

            Class<?> resolverClass = classForName(resolverClassName);
            List<String> args = ArchConfiguration.get().getClassResolverArguments();
            ClassResolverProvider classResolverProvider = createProvider(resolverClass, args);
            return Optional.of(classResolverProvider.get());
        }

        @MayResolveTypesViaReflection(reason = "Loading a ClassResolver implementation is independent of the actual import")
        private Class<?> classForName(Optional<String> resolverClassName) {
            try {
                return Class.forName(resolverClassName.get());
            } catch (ClassNotFoundException e) {
                throw ClassResolverConfigurationException.onLoadingClass(resolverClassName.get(), e);
            }
        }

        private ClassResolverProvider createProvider(final Class<?> resolverClass, final List<String> args) {
            final Optional<Constructor<?>> listConstructor = tryGetListConstructor(resolverClass);
            if (listConstructor.isPresent()) {
                return new ClassResolverProvider(instantiationException(listConstructor.get(), args)) {
                    @Override
                    ClassResolver tryGet() throws Exception {
                        return (ClassResolver) listConstructor.get().newInstance(args);
                    }
                };
            }
            if (!args.isEmpty()) {
                throw ClassResolverConfigurationException.onWrongConstructor(resolverClass, args);
            }
            return tryCreateResolverProviderForDefaultConstructor(resolverClass, args);
        }

        private Function<Exception, ClassResolverConfigurationException> instantiationException(
                final Constructor<?> constructor, final List<String> args) {

            return new Function<Exception, ClassResolverConfigurationException>() {
                @Override
                public ClassResolverConfigurationException apply(Exception cause) {
                    return ClassResolverConfigurationException.onInstantiation(constructor, args, cause);
                }
            };
        }

        private Optional<Constructor<?>> tryGetListConstructor(Class<?> resolverClass) {
            try {
                return Optional.<Constructor<?>>of(resolverClass.getConstructor(List.class));
            } catch (NoSuchMethodException e) {
                return Optional.absent();
            }
        }

        private ClassResolverProvider tryCreateResolverProviderForDefaultConstructor(final Class<?> resolverClass, final List<String> args) {
            final Constructor<?> defaultConstructor;
            try {
                defaultConstructor = resolverClass.getConstructor();
            } catch (NoSuchMethodException e) {
                throw ClassResolverConfigurationException.onWrongArguments(resolverClass, e);
            }

            return new ClassResolverProvider(instantiationException(defaultConstructor, args)) {
                @Override
                ClassResolver tryGet() throws Exception {
                    return (ClassResolver) resolverClass.getConstructor().newInstance();
                }
            };
        }

        static class NoOpClassResolver implements ClassResolver {
            @Override
            public void setClassUriImporter(ClassUriImporter classUriImporter) {
            }

            @Override
            public Optional<JavaClass> tryResolve(String typeName) {
                return Optional.absent();
            }
        }

        private abstract class ClassResolverProvider {
            private final Function<Exception, ClassResolverConfigurationException> onFailure;

            ClassResolverProvider(Function<Exception, ClassResolverConfigurationException> onFailure) {
                this.onFailure = onFailure;
            }

            ClassResolver get() {
                try {
                    return tryGet();
                } catch (Exception e) {
                    throw onFailure.apply(e);
                }
            }

            abstract ClassResolver tryGet() throws Exception;
        }
    }
}
