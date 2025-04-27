package com.tngtech.archunit.core.importer;

import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccess;
import static java.util.stream.Collectors.toSet;

public class ClassFileImporterLambdaAccessesNewerJavaVersionTest {
    /**
     * This is a special case: For local constructors the Java compiler actually adds a lambda calling the constructor
     * for a constructor reference. Since we do not distinguish between calls from within a lambda and outside it,
     * this will lead to such a constructor reference being reported as a constructor call.
     *
     * Note that this actually does not compile with JDK 8
     */
    @Test
    public void imports_constructor_reference_to_local_class_from_lambda_without_parameter_as_direct_call() {
        class Target {
        }

        @SuppressWarnings("unused")
        class Caller {
            Supplier<Supplier<Target>> call() {
                return () -> Target::new;
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Target.class, Caller.class);
        JavaConstructorCall call = getOnlyElement(
                filterOriginByName(classes.get(Caller.class).getConstructorCallsFromSelf(), "call"));

        assertThatAccess(call).isFrom("call").isTo(Target.class, CONSTRUCTOR_NAME, getClass());
    }

    private <ACCESS extends JavaAccess<?>> Set<ACCESS> filterOriginByName(Set<ACCESS> calls, String methodName) {
        return calls.stream()
                .filter(call -> call.getOrigin().getName().equals(methodName))
                .collect(toSet());
    }
}
