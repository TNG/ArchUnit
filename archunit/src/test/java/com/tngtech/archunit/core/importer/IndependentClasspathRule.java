package com.tngtech.archunit.core.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

class IndependentClasspathRule extends ExternalResource {
    private Setup setup;
    private ClassLoader storedContextClassLoader;
    private final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Override
    protected void before() throws Throwable {
        systemPropertiesRule.before();
    }

    void configureClasspath() {
        storedContextClassLoader = Thread.currentThread().getContextClassLoader();
        setup = Setup.create();
        Thread.currentThread().setContextClassLoader(setup.classLoader);
        String pathEntry = new File(URI.create(getOnlyUrl().getFile().replaceAll("!/.*", ""))).getAbsolutePath();
        System.setProperty("java.class.path", pathEntry);
    }

    @Override
    protected void after() {
        if (setup != null) {
            Thread.currentThread().setContextClassLoader(storedContextClassLoader);
        }
        storedContextClassLoader = null;
        setup = null;
        systemPropertiesRule.after();
    }

    String getNameOfSomeContainedClass() {
        return Setup.INDEPENDENT_CLASS_IN_JAR;
    }

    String getIndependentTopLevelPackage() {
        return Setup.PACKAGE_OF_INDEPENDENT_CLASS_IN_JAR;
    }

    URL getOnlyUrl() {
        return getOnlyElement(ImmutableSet.copyOf(setup.classLoader.getURLs()));
    }

    Set<String> getNamesOfClasses() {
        return Setup.NAMES_OF_CLASSES_IN_JAR;
    }

    Set<String> getPackagesOfClasses() {
        return Setup.PACKAGES_OF_CLASSES_IN_JAR;
    }

    private static class Setup {
        static final String PACKAGE_OF_INDEPENDENT_CLASS_IN_JAR =
                "com.tngtech.archunit.core.importer.independent_testexamples";
        static final String INDEPENDENT_CLASS_IN_JAR =
                PACKAGE_OF_INDEPENDENT_CLASS_IN_JAR + ".IndependentClassOne";
        static final Set<String> NAMES_OF_CLASSES_IN_JAR = ImmutableSet.of(
                "com.tngtech.archunit.core.importer.independent_testexamples.IndependentClassOne",
                "com.tngtech.archunit.core.importer.independent_testexamples.IndependentClassTwo",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_with_entry.IndependentSubClassWithEntryOne",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_with_entry.IndependentSubClassWithEntryTwo",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_without_entry.IndependentSubClassWithoutEntryOne",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_without_entry.IndependentSubClassWithoutEntryTwo");
        static final Set<String> PACKAGES_OF_CLASSES_IN_JAR = ImmutableSet.of(
                "com.tngtech.archunit.core.importer.independent_testexamples",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_with_entry",
                "com.tngtech.archunit.core.importer.independent_testexamples.subpackage_without_entry");

        private final URLClassLoader classLoader;

        private Setup(URLClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        static Setup create() {
            try {
                return tryCreate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Setup tryCreate() throws MalformedURLException, ClassNotFoundException {
            URL resource = Setup.class.getResource("testexamples/independent-test-example.jar");
            URL url = Location.of(resource).asURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
            verifySetupAsExpected(classLoader, resource);
            return new Setup(classLoader);
        }

        private static void verifySetupAsExpected(URLClassLoader classLoader, URL resource) throws ClassNotFoundException {
            try {
                Class.forName(INDEPENDENT_CLASS_IN_JAR);
                Assert.fail(String.format(
                        "Class %s shouldn't be loadable by default ClassLoader", INDEPENDENT_CLASS_IN_JAR));
            } catch (ClassNotFoundException expected) {
                // This is the expected behavior, since the class is not on the classpath
            }

            checkState(classLoader.loadClass(INDEPENDENT_CLASS_IN_JAR) != null,
                    "Couldn't load expected class %s in Jar %s",
                    INDEPENDENT_CLASS_IN_JAR, resource.getFile());
        }
    }
}
