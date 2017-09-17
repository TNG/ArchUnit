package com.tngtech.archunit.junit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.SourceTest;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
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
        JavaClasses archunitJunitClasses = new ClassFileImporter().importUrl(rootOf(ArchTest.class));

        Set<String> guavaDependencies = findGuavaDependenciesIn(archunitJunitClasses);

        if (PRINT_EXPECTED_RESULT) {
            System.out.println(Joiner.on(lineSeparator()).join(guavaDependencies));
        }

        List<String> keptTargets = read("proguard-keepclassmembers.txt");

        assertThat(guavaDependencies).as("Guava dependencies of archunit-junit").containsOnlyElementsOf(keptTargets);
    }

    private Set<String> findGuavaDependenciesIn(JavaClasses archunitJunitClasses) {
        Set<String> guavaDependencies = new TreeSet<>();
        for (JavaClass javaClass : archunitJunitClasses) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                if (dependency.getTargetClass().getPackage().startsWith("com.google")) {
                    guavaDependencies.add(dependency.getTargetClass().getName().replaceAll("\\$.*", ""));
                }
            }
        }
        return guavaDependencies;
    }

    private List<String> read(String fileName) throws IOException {
        File currentAttempt = new File(new File("."), fileName).getCanonicalFile();
        while (!currentAttempt.exists() && currentAttempt.getParentFile() != null) {
            currentAttempt = new File(currentAttempt.getParentFile().getParentFile(), fileName);
        }
        checkState(currentAttempt.exists(), "Couldn't find %s in any parent dir", fileName);
        return Files.readLines(currentAttempt, StandardCharsets.UTF_8);
    }

    private URL rootOf(Class<?> clazz) throws MalformedURLException {
        URL url = SourceTest.urlOf(clazz);
        String root = url.toString().replaceAll("/com/tngtech/archunit/.*", "/com/tngtech/archunit");
        return new URL(root);
    }
}
