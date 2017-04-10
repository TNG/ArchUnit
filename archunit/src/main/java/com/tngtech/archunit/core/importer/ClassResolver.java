package com.tngtech.archunit.core.importer;

import java.net.URI;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.JavaClass;

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
        Optional<JavaClass> tryImport(URI uri);
    }
}
