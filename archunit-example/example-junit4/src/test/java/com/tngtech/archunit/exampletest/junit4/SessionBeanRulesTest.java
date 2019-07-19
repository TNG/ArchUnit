package com.tngtech.archunit.exampletest.junit4;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.originOwnerEqualsTargetOwner;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaCodeUnit.Predicates.constructor;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class SessionBeanRulesTest {

    @ArchTest
    static final ArchRule stateless_session_beans_should_not_have_state =
            noClasses().should()
                    .setFieldWhere(targetIsStatelessSessionBean().and(accessOriginIsOutsideOfConstruction()))
                    .as("No Stateless Session Bean should have state");

    @ArchTest
    static final ArchRule business_interface_implementations_should_be_unique =
            classes().that(are(businessInterfaces())).should(haveAUniqueImplementation());

    private static DescribedPredicate<JavaFieldAccess> targetIsStatelessSessionBean() {
        return Get.<JavaFieldAccess, FieldAccessTarget>target()
                .then(HasOwner.Functions.Get.<JavaClass>owner())
                .is(annotatedWith(Stateless.class));
    }

    private static DescribedPredicate<JavaAccess<?>> accessOriginIsOutsideOfConstruction() {
        return not(originOwnerEqualsTargetOwner()).or(originNeitherConstructorNorPostConstruct());
    }

    private static DescribedPredicate<JavaAccess<?>> originNeitherConstructorNorPostConstruct() {
        return Get.origin().is(not(constructor()).and(not(annotatedWith("javax.annotation.PostConstruct"))));
    }

    private static DescribedPredicate<JavaClass> haveLocalBeanSubclass() {
        return new DescribedPredicate<JavaClass>("have subclass that is a local bean") {
            @Override
            public boolean apply(JavaClass input) {
                for (JavaClass subClass : input.getAllSubClasses()) {
                    if (isLocalBeanImplementation(subClass, input)) {
                        return true;
                    }
                }
                return false;
            }

            // NOTE: We assume that in this project by convention @Local is always used as @Local(type) with exactly
            //       one type, otherwise this would need to be more sophisticated
            private boolean isLocalBeanImplementation(JavaClass bean, JavaClass businessInterfaceType) {
                return bean.isAnnotatedWith(Local.class) &&
                        bean.getAnnotationOfType(Local.class).value()[0].getName()
                                .equals(businessInterfaceType.getName());
            }
        };
    }

    private static DescribedPredicate<JavaClass> businessInterfaces() {
        return INTERFACES.and(haveLocalBeanSubclass()).as("business interfaces");
    }

    private static ArchCondition<JavaClass> haveAUniqueImplementation() {
        return new ArchCondition<JavaClass>("have a unique implementation") {
            @Override
            public void check(JavaClass businessInterface, ConditionEvents events) {
                events.add(new SimpleConditionEvent(businessInterface,
                        businessInterface.getAllSubClasses().size() <= 1,
                        describe(businessInterface)));
            }

            private String describe(JavaClass businessInterface) {
                return String.format("%s is implemented by %s",
                        businessInterface.getSimpleName(), joinNamesOf(businessInterface.getAllSubClasses()));
            }

            private String joinNamesOf(Set<JavaClass> implementations) {
                if (implementations.isEmpty()) {
                    return "";
                }

                Deque<JavaClass> toJoin = new LinkedList<>(implementations);
                StringBuilder sb = new StringBuilder(toJoin.pollFirst().getSimpleName());
                for (JavaClass javaClass : toJoin) {
                    sb.append(", ").append(javaClass.getSimpleName());
                }
                return sb.toString();
            }
        };
    }
}
