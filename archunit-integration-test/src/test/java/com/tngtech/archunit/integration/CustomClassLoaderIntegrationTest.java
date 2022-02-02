package com.tngtech.archunit.integration;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.importer.UrlSourceTestUtils;
import com.tngtech.archunit.exampletest.junit5.CodingRulesTest;
import com.tngtech.archunit.testutils.ContextClassLoaderExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.junit.FieldSelector.selectField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

@ExtendWith(ContextClassLoaderExtension.class)
class CustomClassLoaderIntegrationTest {

    @Test
    void should_load_test_classes_with_context_ClassLoader() throws Exception {
        // This is a simplified version of the setup that e.g. Quarkus continuous testing is using.
        // The ClassLoader that loads the test infrastructure does not know the concrete test classes, only the context ClassLoader does

        CustomTestClassLoader customTestClassLoader = new CustomTestClassLoader();
        Thread.currentThread().setContextClassLoader(customTestClassLoader.testClassLoader);

        JUnitPlatformLauncherFromOtherClassLoader launcher = new JUnitPlatformLauncherFromOtherClassLoader(customTestClassLoader);

        List<String> testFailureMessage = launcher.run(CodingRulesTest.class, "no_access_to_standard_streams");

        assertThat(getOnlyElement(testFailureMessage)).contains("Rule 'no classes should access standard streams' was violated");
    }

    private static class CustomTestClassLoader {
        private final URLClassLoader testInfrastructureClassLoader;
        private final URLClassLoader testClassLoader;

        public CustomTestClassLoader() {
            URL[] fullClasspath = ImmutableList.copyOf(UrlSourceTestUtils.getClasspath()).toArray(new URL[0]);
            URL[] testInfrastructureClasspath = Arrays.stream(fullClasspath)
                    .filter(url -> !url.toString().contains("example"))
                    .toArray(URL[]::new);
            testInfrastructureClassLoader = new URLClassLoader(testInfrastructureClasspath, null);
            testClassLoader = new URLClassLoader(fullClasspath, testInfrastructureClassLoader);
        }

        Class<?> loadTestInfrastructure(Class<?> clazz) throws ClassNotFoundException {
            return testInfrastructureClassLoader.loadClass(clazz.getName());
        }

        Class<?> loadTestClass(Class<?> clazz) throws ClassNotFoundException {
            return testClassLoader.loadClass(clazz.getName());
        }
    }

    private static class JUnitPlatformLauncherFromOtherClassLoader {
        private final CustomTestClassLoader customTestClassLoader;

        JUnitPlatformLauncherFromOtherClassLoader(CustomTestClassLoader customTestClassLoader) {
            this.customTestClassLoader = customTestClassLoader;
        }

        public List<String> run(Class<?> testClass, String ruleName) throws Exception {
            Class<?> launcherExecutorClassFromOtherClassLoader = customTestClassLoader.loadTestInfrastructure(JUnitPlatformLauncherExecutor.class);
            Class<?> testClassFromOtherClassLoader = customTestClassLoader.loadTestClass(testClass);
            Object launcherExecutor = accessible(launcherExecutorClassFromOtherClassLoader.getDeclaredConstructor()).newInstance();

            return invoke(launcherExecutor, "run", testClassFromOtherClassLoader, ruleName);
        }

        @SuppressWarnings({"unchecked", "SameParameterValue"}) // Access through Reflection can never be typesafe, the caller needs to decide
        private <T> T invoke(Object owner, String methodName, Object... params) throws Exception {
            Class<?>[] parameterTypes = Arrays.stream(params).map(Object::getClass).toArray(Class[]::new);
            Method runMethod = owner.getClass().getDeclaredMethod(methodName, parameterTypes);
            return (T) accessible(runMethod).invoke(owner, params);
        }

        private <T extends AccessibleObject> T accessible(T member) {
            member.setAccessible(true);
            return member;
        }
    }

    private static class JUnitPlatformLauncherExecutor {
        @SuppressWarnings("unused") // accessed via Reflection
        private List<String> run(Class<?> testClass, String ruleName) {
            Launcher launcher = LauncherFactory.create();
            List<String> violations = new ArrayList<>();
            launcher.registerTestExecutionListeners(new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                    result.getThrowable().ifPresent(t -> violations.add(t.getMessage()));
                }
            });
            // reference test class by class name to make sure FieldSelector can create the class
            launcher.execute(request().selectors(selectField(testClass.getName(), ruleName)).build());
            return violations;
        }
    }
}
