package com.tngtech.archunit.junit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import org.reflections.util.ClasspathHelper;

import static com.tngtech.archunit.core.ReflectionUtils.newInstanceOf;

class ClassCache {
    private final ConcurrentHashMap<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UrlKey, LazyJavaClasses> cachedByUrls = new ConcurrentHashMap<>();

    private ClassFileImporter classFileImporter = new ClassFileImporter();

    public JavaClasses getClassesToAnalyseFor(Class<?> testClass) {
        checkArgument(testClass);

        if (cachedByTest.containsKey(testClass)) {
            return cachedByTest.get(testClass);
        }

        Collection<URL> urls = urlsToImport(testClass);
        UrlKey key = new UrlKey(urls);
        cachedByUrls.putIfAbsent(key, new LazyJavaClasses(urls));
        cachedByTest.put(testClass, cachedByUrls.get(key).get());
        return cachedByUrls.get(key).get();
    }

    private Collection<URL> urlsToImport(Class<?> testClass) {
        AnalyseClasses analyseClasses = testClass.getAnnotation(AnalyseClasses.class);
        Urls<?> packageUrls = packageUrls(analyseClasses.packages());
        Urls<?> classUrls = urlsOfClasses(analyseClasses);
        Collection<URL> result = calculateUrls(packageUrls, classUrls);
        return filter(result, analyseClasses.importOption());
    }

    private Urls<?> packageUrls(String[] packages) {
        Set<URL> urls = new HashSet<>();
        for (String pkg : packages) {
            urls.addAll(createUrlsFrom(pkg));
        }
        return new Urls<>(packages, urls);
    }

    private Collection<URL> createUrlsFrom(String pkg) {
        Set<URL> result = new HashSet<>();
        Collection<URL> rootUrls = ClasspathHelper.forPackage(pkg);
        for (URL rootUrl : rootUrls) {
            result.add(newUrl(pkg, rootUrl));
        }
        return result;
    }

    private Urls<?> urlsOfClasses(AnalyseClasses analyseClasses) {
        Set<URL> urls = new HashSet<>();
        for (Class clazz : analyseClasses.locationsOf()) {
            URL rootUrl = ClasspathHelper.forClass(clazz);
            if (analyseClasses.packages().length == 0) {
                urls.add(rootUrl);
            }
            for (String pkg : analyseClasses.packages()) {
                urls.add(newUrl(pkg, rootUrl));
            }
        }
        return new Urls<>(analyseClasses.locationsOf(), urls);
    }

    private URL newUrl(String pkg, URL rootUrl) {
        try {
            return new URL(rootUrl, pkg.replace('.', '/'));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<URL> calculateUrls(Urls<?> packageUrls, Urls<?> classUrls) {
        Collection<URL> result;
        if (packageUrls.areNotSpecified() && classUrls.areNotSpecified()) {
            result = ClasspathHelper.forClassLoader();
        } else if (packageUrls.areNotSpecified() || classUrls.areNotSpecified()) {
            result = packageUrls.and(classUrls);
        } else {
            result = packageUrls.intersectionWith(classUrls);
        }
        return result;
    }

    // Would be great, if we could just pass the import option on to the ClassFileImporter, but this would be
    // problematic with respect to caching classes for certain URL combinations
    private Collection<URL> filter(Collection<URL> urls, Class<? extends ClassFileImporter.ImportOption> importOption) {
        ClassFileImporter.ImportOption option = newInstanceOf(importOption);
        Set<URL> result = new HashSet<>();
        for (URL url : urls) {
            if (option.includes(url)) {
                result.add(url);
            }
        }
        return result;
    }

    private void checkArgument(Class<?> testClass) {
        if (testClass.getAnnotation(AnalyseClasses.class) == null) {
            throw new IllegalArgumentException(String.format("Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyseClasses.class.getSimpleName()));
        }
    }

    private class LazyJavaClasses {
        private final Collection<URL> urls;
        private volatile JavaClasses javaClasses;

        private LazyJavaClasses(Collection<URL> urls) {
            this.urls = urls;
        }

        public JavaClasses get() {
            if (javaClasses == null) {
                initialize();
            }
            return javaClasses;
        }

        private synchronized void initialize() {
            if (javaClasses == null) {
                javaClasses = classFileImporter.importUrls(urls);
            }
        }
    }

    private static class UrlKey {
        private Set<String> urls = new HashSet<>();

        public UrlKey(Collection<URL> urls) {
            for (URL url : urls) {
                this.urls.add(url.toExternalForm());
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(urls);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final UrlKey other = (UrlKey) obj;
            return Objects.equals(this.urls, other.urls);
        }

        @Override
        public String toString() {
            return "Key{urls=" + urls + '}';
        }
    }

    private static class Urls<DERIVED_FROM> {
        private final DERIVED_FROM[] derivedFrom;
        private final Set<URL> urls;

        private Urls(DERIVED_FROM[] derivedFrom, Set<URL> urls) {
            this.derivedFrom = derivedFrom;
            this.urls = urls;
        }

        public boolean areNotSpecified() {
            return derivedFrom.length == 0;
        }

        public Collection<URL> and(Urls<?> other) {
            return Sets.union(urls, other.urls);
        }

        public Collection<URL> intersectionWith(Urls<?> other) {
            return Sets.intersection(urls, other.urls);
        }
    }
}
