package com.tngtech.archunit.exampletest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.properties.HasOwner;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.JavaAccess.Predicates.originOwnerEqualsTargetOwner;
import static com.tngtech.archunit.core.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.JavaCodeUnit.Predicates.constructor;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.allClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class SessionBeanRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingSessionBeanRules.class);
    }

    @Ignore
    @Test
    public void stateless_session_beans_should_not_have_state() {
        noClasses().should()
                .setFieldWhere(TARGET_IS_STATELESS_SESSION_BEAN.and(ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION))
                .as("No Stateless Session Bean should have state").check(classes);
    }

    @Ignore
    @Test
    public void business_interface_implementations_should_be_unique() {
        allClasses().that(are(BUSINESS_INTERFACES)).should(HAVE_AN_UNIQUE_IMPLEMENTATION).check(classes);
    }

    private static final DescribedPredicate<JavaFieldAccess> TARGET_IS_STATELESS_SESSION_BEAN =
            Get.<JavaFieldAccess, FieldAccessTarget>target()
                    .then(HasOwner.Functions.Get.<JavaClass>owner())
                    .is(annotatedWith(Stateless.class));

    private static final DescribedPredicate<JavaAccess<?>> ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION =
            not(originOwnerEqualsTargetOwner()).or(originNeitherConstructorNorPostConstruct());

    private static DescribedPredicate<JavaAccess<?>> originNeitherConstructorNorPostConstruct() {
        return Get.origin().is(not(constructor()).and(not(annotatedWith(PostConstruct.class))));
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
                            bean.getReflectionAnnotation(Local.class).value()[0].getName()
                                    .equals(businessInterfaceType.getName());
                }
            };

    private static final DescribedPredicate<JavaClass> BUSINESS_INTERFACES =
            INTERFACES.and(HAVE_LOCAL_BEAN_SUBCLASS).as("business interfaces");

    private static final ArchCondition<JavaClass> HAVE_AN_UNIQUE_IMPLEMENTATION =
            new ArchCondition<JavaClass>("have an unique implementation") {
                @Override
                public void check(JavaClass businessInterface, ConditionEvents events) {
                    events.add(new SimpleConditionEvent(businessInterface.getAllSubClasses().size() <= 1,
                            "%s is implemented by %s",
                            businessInterface.getSimpleName(), joinNamesOf(businessInterface.getAllSubClasses())));
                }

                private String joinNamesOf(Set<JavaClass> implementations) {
                    List<String> simpleNames = new ArrayList<>();
                    for (JavaClass impl : implementations) {
                        simpleNames.add(impl.getSimpleName());
                    }
                    return Joiner.on(", ").join(simpleNames);
                }
            };
}
