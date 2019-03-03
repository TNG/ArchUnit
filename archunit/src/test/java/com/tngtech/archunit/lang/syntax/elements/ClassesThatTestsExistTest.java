package com.tngtech.archunit.lang.syntax.elements;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a simple sanity check, since we have different contexts that need to support {@link ClassesThat},
 * so for each of those contexts there should exist a test covering each of the syntax elements
 * {@link ClassesThat} provides.
 */
public class ClassesThatTestsExistTest {
    private static final Set<Class<?>> CLASSES_SHOULD_THAT_CONTEXT = ImmutableSet.of(
            GivenClassesThatTest.class, ShouldClassesThatTest.class, ShouldOnlyByClassesThatTest.class
    );

    @Test
    public void for_each_context_all_syntax_elements_are_tested() {
        for (Multiset.Entry<String> entry : getSyntaxElements().entrySet()) {
            for (Class<?> testClass : CLASSES_SHOULD_THAT_CONTEXT) {
                Set<Method> testMethods = getMethodsStartingWith(entry.getElement(), testClass);
                assertThat(testMethods.size())
                        .as("Number of tests in %s to cover %s.%s(..)", testClass.getSimpleName(),
                                ClassesThat.class.getSimpleName(), entry.getElement())
                        .isGreaterThanOrEqualTo(entry.getCount());
            }
        }
    }

    private Set<Method> getMethodsStartingWith(String prefix, Class<?> testClass) {
        Set<Method> result = new HashSet<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getAnnotation(Test.class) != null && method.getName().startsWith(prefix)) {
                result.add(method);
            }
        }
        return result;
    }

    private Multiset<String> getSyntaxElements() {
        HashMultiset<String> result = HashMultiset.create();
        for (Method method : ClassesThat.class.getMethods()) {
            result.add(method.getName());
        }
        return result;
    }
}
