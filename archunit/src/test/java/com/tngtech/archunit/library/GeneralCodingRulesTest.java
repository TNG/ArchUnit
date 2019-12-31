package com.tngtech.archunit.library;

import javax.annotation.Resource;
import javax.inject.Inject;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class GeneralCodingRulesTest {

    @Test
    public void noFieldInjectionRule_ClassWithoutFieldInjection() {
        JavaClasses classes = importClasses(ClassWithoutFieldInjection.class);
        assertThat(NO_CLASSES_SHOULD_USE_FIELD_INJECTION).checking(classes).hasNoViolation();
    }

    @Test
    public void noFieldInjectionRule_ClassWithSpringAutowiredFieldInjection() {
        JavaClasses classes = importClasses(ClassWithSpringAutowiredFieldInjection.class);
        assertThat(NO_CLASSES_SHOULD_USE_FIELD_INJECTION).checking(classes).hasViolations(1);
    }

    @Test
    public void noFieldInjectionRule_ClassWithSpringValueFieldInjection() {
        JavaClasses classes = importClasses(ClassWithSpringValueFieldInjection.class);
        assertThat(NO_CLASSES_SHOULD_USE_FIELD_INJECTION).checking(classes).hasViolations(1);
    }

    @Test
    public void noFieldInjectionRule_ClassWithJakartaInjectFieldInjection() {
        JavaClasses classes = importClasses(ClassWithJakartaInjectFieldInjection.class);
        assertThat(NO_CLASSES_SHOULD_USE_FIELD_INJECTION).checking(classes).hasViolations(1);
    }

    @Test
    public void noFieldInjectionRule_ClassWithJakartaResourceFieldInjection() {
        JavaClasses classes = importClasses(ClassWithJakartaResourceFieldInjection.class);
        assertThat(NO_CLASSES_SHOULD_USE_FIELD_INJECTION).checking(classes).hasViolations(1);
    }

    private static class ClassWithoutFieldInjection {
        @SuppressWarnings("unused")
        private String value;
    }

    private static class ClassWithSpringAutowiredFieldInjection {
        @Autowired
        @SuppressWarnings("unused")
        private String value;
    }

    private static class ClassWithSpringValueFieldInjection {
        @Value("${name}")
        @SuppressWarnings("unused")
        private String value;
    }

    private static class ClassWithJakartaInjectFieldInjection {
        @Inject
        @SuppressWarnings("unused")
        private String value;
    }

    private static class ClassWithJakartaResourceFieldInjection {
        @Resource
        @SuppressWarnings("unused")
        private String value;
    }
}
