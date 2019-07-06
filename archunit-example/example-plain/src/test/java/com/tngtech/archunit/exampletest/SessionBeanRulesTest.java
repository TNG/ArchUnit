package com.tngtech.archunit.exampletest;

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
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.originOwnerEqualsTargetOwner;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaCodeUnit.Predicates.constructor;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
public class SessionBeanRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingSessionBeanRules.class);

    @Test
    public void stateless_session_beans_should_not_have_state() {
        noClasses().should()
                .setFieldWhere(TARGET_IS_STATELESS_SESSION_BEAN.and(ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION))
                .as("No Stateless Session Bean should have state").check(classes);
    }

    @Test
    public void business_interface_implementations_should_be_unique() {
        classes().that(are(BUSINESS_INTERFACES)).should(HAVE_A_UNIQUE_IMPLEMENTATION).check(classes);
    }

    private static final DescribedPredicate<JavaFieldAccess> TARGET_IS_STATELESS_SESSION_BEAN =
            Get.<JavaFieldAccess, FieldAccessTarget>target()
                    .then(HasOwner.Functions.Get.<JavaClass>owner())
                    .is(annotatedWith(Stateless.class));

    private static final DescribedPredicate<JavaAccess<?>> ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION =
            not(originOwnerEqualsTargetOwner()).or(originNeitherConstructorNorPostConstruct());

    private static DescribedPredicate<JavaAccess<?>> originNeitherConstructorNorPostConstruct() {
        return Get.origin().is(not(constructor()).and(not(annotatedWith("javax.annotation.PostConstruct"))));
    }

    private static final DescribedPredicate<JavaClass> HAVE_LOCAL_BEAN_SUBCLASS =
            new DescribedPredicate<JavaClass>("have subclass that is a local bean") {
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

    private static final DescribedPredicate<JavaClass> BUSINESS_INTERFACES =
            INTERFACES.and(HAVE_LOCAL_BEAN_SUBCLASS).as("business interfaces");

    private static final ArchCondition<JavaClass> HAVE_A_UNIQUE_IMPLEMENTATION =
            new ArchCondition<JavaClass>("have a unique implementation") {
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
