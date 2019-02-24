package com.tngtech.archunit.exampletest;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.anticorruption.WrappedResult;
import com.tngtech.archunit.example.security.Secured;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.Formatters.formatLocation;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;

@Category(Example.class)
public class MethodsTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

    @Test
    public void all_public_methods_in_the_controller_layer_should_return_API_response_wrappers() {
        methods()
                .that(areDefinedInAPackage("..anticorruption..")).and(arePublic())
                .should(returnType(WrappedResult.class))
                .because("we don't want to couple the client code directly to the return types of the encapsulated module")
                .check(classes);
    }

    @Test
    public void code_units_in_DAO_layer_should_not_be_Secured() {
        noCodeUnits()
                .that(areDefinedInAPackage("..persistence.."))
                .should(beAnnotatedWith(Secured.class))
                .check(classes);
    }

    private static DescribedPredicate<? super JavaMember> areDefinedInAPackage(final String packageIdentifier) {
        return Get.<JavaClass>owner().is(resideInAPackage(packageIdentifier));
    }

    private static DescribedPredicate<HasModifiers> arePublic() {
        return modifier(PUBLIC).as("are public");
    }

    private static ArchCondition<JavaMethod> returnType(final Class<?> type) {
        return new ArchCondition<JavaMethod>("return type " + type.getName()) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean typeMatches = method.getRawReturnType().isAssignableTo(type);
                String message = String.format("%s returns %s in %s",
                        method.getFullName(), method.getRawReturnType().getName(),
                        formatLocation(method.getOwner(), 0));
                events.add(new SimpleConditionEvent(method, typeMatches, message));
            }
        };
    }

    private static ArchCondition<JavaCodeUnit> beAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return new ArchCondition<JavaCodeUnit>("be annotated with @" + annotationType.getSimpleName()) {
            @Override
            public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
                boolean satisfied = codeUnit.isAnnotatedWith(annotationType);
                String message = String.format("%s is%s annotated with @%s",
                        codeUnit.getFullName(), satisfied ? "" : " not", annotationType.getSimpleName());
                events.add(new SimpleConditionEvent(codeUnit, satisfied, message));
            }
        };
    }
}