package com.tngtech.archunit.core.importer.resolvers;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Only resolves classes from classpath, that are beneath the configured {@link #packageRoots}. E.g. useful,
 * if one wants to import com.my.app.foo, but resolve all dependencies to com.my.app, but not to java.util..
 * or similar.
 *
 * @see ClassResolverFromClasspath
 */
public final class SelectedClassResolverFromClasspath implements ClassResolver {
    private final Set<String> packageRoots;
    private final ClassResolverFromClasspath classResolverFromClasspath = new ClassResolverFromClasspath();

    @PublicAPI(usage = ACCESS)
    public SelectedClassResolverFromClasspath(List<String> packageRoots) {
        this.packageRoots = ImmutableSet.copyOf(packageRoots);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void setClassUriImporter(ClassUriImporter classUriImporter) {
        classResolverFromClasspath.setClassUriImporter(classUriImporter);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> tryResolve(String typeName) {
        for (String root : packageRoots) {
            if (typeName.startsWith(root)) {
                return classResolverFromClasspath.tryResolve(typeName);
            }
        }
        return Optional.absent();
    }
}
