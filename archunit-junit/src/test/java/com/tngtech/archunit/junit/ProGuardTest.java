package com.tngtech.archunit.junit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.isEmpty;
import static com.tngtech.archunit.core.domain.SourceTest.urlOf;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

public class ProGuardTest {
    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule()
            .resolveAdditionalDependenciesFromClassPath(false);

    private static final boolean PRINT_EXPECTED_RESULT = false;

    /**
     * We have to make sure, that all Guava targets used by archunit-junit are left alone by ProGuard,
     * so no necessary methods, etc., are removed from the byte code while optimizing the relocated
     * classes within archunit.
     */
    @Test
    public void all_guava_targets_are_configured() throws Exception {
        URL urlOfArchUnitJUnit = rootOf(ArchUnitRunner.class);
        URL urlOfGuava = baseJarUrlOf(ImmutableSet.class);
        Set<Location> locations = Locations.of(ImmutableList.of(urlOfArchUnitJUnit, urlOfGuava));
        JavaClasses archunitJunitClasses = new ClassFileImporter().importLocations(locations);

        Set<String> guavaDependencies = findGuavaDependencyNamesIn(archunitJunitClasses);

        if (PRINT_EXPECTED_RESULT) {
            System.out.println(Joiner.on(lineSeparator()).join(guavaDependencies));
        }

        List<String> keptTargets = read("proguard-keepclassmembers.txt");

        assertThat(guavaDependencies).as("Guava dependencies of archunit-junit").containsOnlyElementsOf(keptTargets);
    }

    private Set<String> findGuavaDependencyNamesIn(Iterable<JavaClass> archunitJunitClasses) {
        Set<JavaClass> guavaDependencies = findGuavaDependenciesIn(Collections.<JavaClass>emptySet(), archunitJunitClasses);
        Set<String> result = new TreeSet<>();
        for (JavaClass guavaClass : guavaDependencies) {
            result.add(guavaClass.getName().replaceAll("\\$.*", ""));
        }
        return result;
    }

    private Set<JavaClass> findGuavaDependenciesIn(Set<JavaClass> alreadyPresent, Iterable<JavaClass> classes) {
        if (isEmpty(classes)) {
            return alreadyPresent;
        }

        Set<JavaClass> guavaDependencies = new HashSet<>();
        for (JavaClass javaClass : classes) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (!alreadyPresent.contains(target) && target.getPackage().startsWith("com.google")) {
                    guavaDependencies.add(target);
                }
            }
        }
        return findGuavaDependenciesIn(Sets.union(alreadyPresent, guavaDependencies), guavaDependencies);
    }

    private List<String> read(String fileName) throws IOException {
        return Files.readLines(BuildStepsDir.get().find(fileName), StandardCharsets.UTF_8);
    }

    private URL rootOf(Class<?> clazz) throws MalformedURLException {
        return newUrlWithReplace(clazz, "/com/tngtech/archunit/.*", "/com/tngtech/archunit");
    }

    private URL baseJarUrlOf(Class<?> guavaClass) throws MalformedURLException {
        return newUrlWithReplace(guavaClass, "!/.*", "!/");
    }

    private URL newUrlWithReplace(Class<?> clazz, String regex, String replacement) throws MalformedURLException {
        return new URL(urlOf(clazz).toString().replaceAll(regex, replacement));
    }

    private static class BuildStepsDir {
        private static final String DIR_NAME = "build-steps";
        private static final BuildStepsDir INSTANCE = new BuildStepsDir();

        static BuildStepsDir get() {
            return INSTANCE;
        }

        private final Path buildStepsDir;

        private BuildStepsDir() {
            File currentAttempt = newCanonicalFileInRelativePath(DIR_NAME);
            while (!currentAttempt.exists() && currentAttempt.getParentFile() != null) {
                currentAttempt = new File(currentAttempt.getParentFile().getParentFile(), DIR_NAME);
            }
            checkState(currentAttempt.exists(), "Couldn't find %s in any parent dir", DIR_NAME);
            buildStepsDir = currentAttempt.toPath();
        }

        private File newCanonicalFileInRelativePath(String fileName) {
            try {
                return new File(new File("."), fileName).getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File find(final String fileName) {
            try {
                final File[] result = new File[1];
                java.nio.file.Files.walkFileTree(buildStepsDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toFile().getName().equals(fileName)) {
                            result[0] = file.toFile();
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                return checkNotNull(result[0], "Couldn't find %s within %s", fileName, buildStepsDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
