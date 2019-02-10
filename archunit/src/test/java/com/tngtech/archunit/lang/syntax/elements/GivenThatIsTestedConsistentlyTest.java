package com.tngtech.archunit.lang.syntax.elements;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import org.junit.Assert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static org.assertj.core.api.Assertions.assertThat;

public class GivenThatIsTestedConsistentlyTest {

    @Test
    public void classes_that_and_members_declared_in_classes_that_test_the_same() {
        Set<String> names = getAllTestMethodNames(GivenMembersDeclaredInClassesThatTest.class);
        assertThat(names).as("Tests relevant for " + ClassesThat.class.getSimpleName())
                .isEqualTo(getAllTestMethodNames(GivenClassesThatTest.class));
    }

    @Test
    public void classes_that_tests_all_relevant_methods() {
        JavaClasses classes = importClassesWithContext(GivenClassesThatTest.class, ClassesThat.class);
        JavaClass test = classes.get(GivenClassesThatTest.class);
        for (JavaMethod method : classes.get(ClassesThat.class).getMethods()) {
            assertAccessFrom(test, method);
        }
    }

    private void assertAccessFrom(JavaClass test, JavaMethod method) {
        for (JavaMethodCall call : method.getAccessesToSelf()) {
            if (call.getOriginOwner().equals(test)) {
                return;
            }
        }
        Assert.fail("Method " + method.getFullName() + " is not called by " + test.getName());
    }

    private Set<String> getAllTestMethodNames(Class<?> testClass) {
        Set<Method> methods = new HashSet<>();
        for (Method method : testClass.getMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                methods.add(method);
            }
        }
        return getNames(methods);
    }

    private Set<String> getNames(Set<Method> methods) {
        Set<String> result = new HashSet<>();
        for (Method method : methods) {
            result.add(method.getName());
        }
        return result;
    }
}
