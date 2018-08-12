import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.thirdparty.com.google.common.collect.ImmutableSet;
import org.joox.Match;
import org.junit.Test;
import org.xml.sax.SAXException;

import static com.tngtech.archunit.thirdparty.com.google.common.base.Preconditions.checkState;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;

public class TestResultTest {
    private static final File rootFolder = findRootFolder();

    @Test
    public void test_result_matches() throws Exception {
        GivenTestClasses givenTestClasses = GivenTestClasses.findAll();

        ImportedTestResults importedTestResults = ImportedTestResults.importAll();

        importedTestResults.assertMatchWith(givenTestClasses);
    }

    private static File findRootFolder() {
        File file = fileFromResource("/");
        while (!new File(file, "pom.xml").exists()) {
            file = file.getParentFile();
        }
        return file;
    }

    private static File fileFromResource(String resourceName) {
        try {
            return new File(TestResultTest.class.getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static class GivenTestClasses {
        private final SingleTestProvider singleTestProvider = createSingleTestProvider();
        private final Set<SingleTest> tests = new HashSet<>();

        GivenTestClasses(Set<Class<?>> testClasses) {
            for (Class<?> testClass : testClasses) {
                tests.addAll(getTestsIn(testClass));
            }
            checkState(!tests.isEmpty(), "No given tests found");
        }

        private Set<SingleTest> getTestsIn(Class<?> clazz) {
            Set<SingleTest> result = new HashSet<>();
            result.addAll(singleTestProvider.getTestMethods(ImmutableSet.copyOf(clazz.getDeclaredMethods())));
            result.addAll(singleTestProvider.getTestFields(ImmutableSet.copyOf(clazz.getDeclaredFields())));
            return result;
        }

        static GivenTestClasses findAll() throws IOException {
            final Set<Class<?>> classes = new HashSet<>();
            final Path root = fileFromResource("/").toPath();
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith("Test.class") && !fileName.contains(TestResultTest.class.getSimpleName())) {
                        String resolvedClassFile = root.relativize(file).toString();
                        classes.add(resolveClassFromFileName(resolvedClassFile));
                    }
                    return FileVisitResult.CONTINUE;
                }

                private Class<?> resolveClassFromFileName(String resolvedClassFile) {
                    try {
                        String className = resolvedClassFile
                                .replace(File.separatorChar, '.')
                                .replace(".class", "");
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return new GivenTestClasses(classes);
        }
    }

    private static class ImportedTestResults {
        private final Set<SingleTest> failedArchitectureTests = new HashSet<>();

        ImportedTestResults(List<File> testReports) throws IOException, SAXException {
            for (File report : testReports) {
                Match document = $($(report).document());

                failedArchitectureTests.addAll(getFailedTests(document.find("testcase")));
            }
            markProcessed(testReports);
        }

        Set<SingleTest> getFailedTests(Match testCaseTags) {
            validateAllArchitectureFailures(testCaseTags);

            List<String> classNames = testCaseTags.attrs("classname");
            List<String> testNames = testCaseTags.attrs("name");
            if (classNames.size() != testNames.size()) {
                throw new RuntimeException("Unexpected attrs mismatch, expected the same size: " + classNames + " <-> " + testNames);
            }

            Set<SingleTest> result = new HashSet<>();
            for (int i = 0; i < classNames.size(); i++) {
                result.add(new SingleTest(classNames.get(i), testNames.get(i)));
            }
            return result;
        }

        private void validateAllArchitectureFailures(Match testCaseTags) {
            assertThat(testCaseTags.find("error")).as("tests in error").isEmpty();

            List<String> messages = testCaseTags.find("failure").attrs("message");
            assertThat(testCaseTags).as("tests").hasSize(messages.size());
            for (String message : messages) {
                assertThat(message).startsWith("Architecture Violation");
            }
        }

        void assertMatchWith(GivenTestClasses givenTestClasses) {
            Set<SingleTest> onlyGiven = difference(givenTestClasses.tests, failedArchitectureTests);
            assertThat(onlyGiven).as("Tests that were expected to fail, but didn't").isEmpty();
            Set<SingleTest> onlyFailed = difference(failedArchitectureTests, givenTestClasses.tests);
            assertThat(onlyFailed).as("Tests that unexpectedly failed").isEmpty();
        }

        private <T> Set<T> difference(Set<T> set, Set<T> toSubtract) {
            Set<T> result = new HashSet<>();
            for (T elem : set) {
                if (!toSubtract.contains(elem)) {
                    result.add(elem);
                }
            }
            return result;
        }

        void markProcessed(List<File> testReports) {
            for (File report : testReports) {
                if (!report.renameTo(new File(report.getParentFile(), report.getName() + ".processed"))) {
                    throw new IllegalStateException("Couldn't mark " + report.getAbsolutePath() + " as processed");
                }
            }
        }

        @Override
        public String toString() {
            return "ImportedTestResult{failedArchitectureTests=" + failedArchitectureTests + '}';
        }

        static ImportedTestResults importAll() throws Exception {
            List<File> files = new ArrayList<>();
            for (File file : new File(rootFolder, "target/surefire-reports").listFiles()) {
                if (isRelevantTestReport(file.getName())) {
                    files.add(file);
                }
            }
            return new ImportedTestResults(files);
        }

        private static boolean isRelevantTestReport(String fileName) {
            return fileName.startsWith("TEST-") &&
                    fileName.endsWith(".xml") &&
                    !fileName.equals(String.format("TEST-%s.xml", TestResultTest.class.getName()));
        }
    }

    private static class SingleTest {
        private final String owner;
        private final String testName;

        private SingleTest(String owner, String testName) {
            this.owner = owner;
            this.testName = testName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, testName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SingleTest other = (SingleTest) obj;
            return Objects.equals(this.owner, other.owner)
                    && Objects.equals(this.testName, other.testName);
        }

        @Override
        public String toString() {
            return "SingleTest{" +
                    "owner='" + owner + '\'' +
                    ", testName='" + testName + '\'' +
                    '}';
        }
    }

    private static SingleTestProvider createSingleTestProvider() {
        try {
            Class.forName("com.tngtech.archunit.junit.ArchTest");
            return new JUnitSingleTestProvider();
        } catch (ClassNotFoundException e) {
            return new PlainSingleTestProvider();
        }
    }

    private interface SingleTestProvider {
        Set<SingleTest> getTestMethods(Collection<Method> methods);

        Set<SingleTest> getTestFields(Collection<Field> fields);
    }

    private static class PlainSingleTestProvider implements SingleTestProvider {
        @Override
        public Set<SingleTest> getTestMethods(Collection<Method> methods) {
            Set<SingleTest> result = new HashSet<>();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Test.class)) {
                    result.add(new SingleTest(method.getDeclaringClass().getName(), method.getName()));
                }
            }
            return result;
        }

        @Override
        public Set<SingleTest> getTestFields(Collection<Field> fields) {
            return Collections.emptySet();
        }
    }

    private static class JUnitSingleTestProvider implements SingleTestProvider {
        private final Class<? extends Annotation> archTestAnnotation;
        private final Class<?> archRulesClass;

        @SuppressWarnings("unchecked")
        private JUnitSingleTestProvider() {
            try {
                this.archTestAnnotation = (Class<? extends Annotation>) Class.forName("com.tngtech.archunit.junit.ArchTest");
                this.archRulesClass = Class.forName("com.tngtech.archunit.junit.ArchRules");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Set<SingleTest> getTestMethods(Collection<Method> methods) {
            Set<SingleTest> result = new HashSet<>();
            for (Method method : methods) {
                if (method.getAnnotation(archTestAnnotation) != null || method.getAnnotation(Test.class) != null) {
                    result.add(new SingleTest(method.getDeclaringClass().getName(), method.getName()));
                }
            }
            return result;
        }

        @Override
        public Set<SingleTest> getTestFields(Collection<Field> fields) {
            Set<SingleTest> result = new HashSet<>();
            for (Field field : fields) {
                if (field.getAnnotation(archTestAnnotation) != null) {
                    result.addAll(getTestFields(field));
                }
            }
            return result;
        }

        private Set<SingleTest> getTestFields(Field field) {
            if (ArchRule.class.isAssignableFrom(field.getType())) {
                return Collections.singleton(new SingleTest(field.getDeclaringClass().getName(), field.getName()));
            }
            if (archRulesClass.isAssignableFrom(field.getType())) {
                return getTestsFrom(getValue(field, null));
            }
            throw new IllegalStateException("Unknown @ArchTest: " + field);
        }

        private Set<SingleTest> getTestsFrom(Object archRules) {
            Set<SingleTest> result = new HashSet<>();
            Class<?> definitionLocation = getValue("definitionLocation", archRules);
            result.addAll(getTestFields(asList(definitionLocation.getDeclaredFields())));
            result.addAll(getTestMethods(asList(definitionLocation.getDeclaredMethods())));
            return result;
        }

        @SuppressWarnings("unchecked")
        private <T> T getValue(String fieldName, Object owner) {
            try {
                return getValue(owner.getClass().getDeclaredField(fieldName), owner);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T getValue(Field field, Object owner) {
            try {
                field.setAccessible(true);
                owner = owner == null && !isStatic(field.getModifiers()) ? newInstanceOf(field.getDeclaringClass()) : owner;
                return (T) field.get(owner);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static Object newInstanceOf(Class<?> clazz) {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
