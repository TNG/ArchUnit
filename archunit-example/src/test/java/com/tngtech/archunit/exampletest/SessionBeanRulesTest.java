package com.tngtech.archunit.exampletest;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.DescribedPredicate.are;
import static com.tngtech.archunit.core.JavaClass.INTERFACES;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;

public class SessionBeanRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingSessionBeanRules.class);
    }

    @Ignore
    @Test
    public void stateless_session_beans_should_not_have_state() {
        all(classes.that(are(ANNOTATED_WITH_STATELESS)).as("Stateless Session Beans"))
                .should(NOT_SET_FIELDS_AFTER_CONSTRUCTION.as("not have state"));
    }

    @Ignore
    @Test
    public void business_interface_implementations_should_be_unique() {
        all(classes.that(are(BUSINESS_INTERFACES)).as("Business Interfaces"))
                .should(HAVE_AN_UNIQUE_IMPLEMENTATION);
    }

    private static final DescribedPredicate<JavaClass> ANNOTATED_WITH_STATELESS =
            new DescribedPredicate<JavaClass>("annotated with @" + Stateless.class.getSimpleName()) {
                @Override
                public boolean apply(JavaClass input) {
                    return input.reflect().getAnnotation(Stateless.class) != null;
                }
            };

    private static final DescribedPredicate<JavaFieldAccess> ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION =
            new DescribedPredicate<JavaFieldAccess>("access origin is outside of construction") {
                @Override
                public boolean apply(JavaFieldAccess input) {
                    return !input.getOrigin().isConstructor() &&
                            !input.getOrigin().tryGetAnnotationOfType(PostConstruct.class).isPresent();
                }
            };

    private static final ArchCondition<JavaClass> NOT_SET_FIELDS_AFTER_CONSTRUCTION =
            never(setFieldWhere(ACCESS_ORIGIN_IS_OUTSIDE_OF_CONSTRUCTION));

    private static final DescribedPredicate<JavaClass> BUSINESS_INTERFACES = INTERFACES.
            and(new DescribedPredicate<JavaClass>("has subclass that is local bean") {
                @Override
                public boolean apply(JavaClass input) {
                    for (JavaClass subClass : input.getAllSubClasses()) {
                        if (isLocalBeanImplementation(subClass, input)) {
                            return true;
                        }
                    }
                    return false;
                }

                private boolean isLocalBeanImplementation(JavaClass bean, JavaClass businessInterfaceType) {
                    return bean.isAnnotationPresent(Local.class)
                            && (bean.reflect().getAnnotation(Local.class).value()[0] == businessInterfaceType.reflect());
                }
            });

    private static final ArchCondition<JavaClass> HAVE_AN_UNIQUE_IMPLEMENTATION =
            new ArchCondition<JavaClass>("have an unique implementation") {
                @Override
                public void check(JavaClass businessInterface, ConditionEvents events) {
                    events.add(new ConditionEvent(businessInterface.getAllSubClasses().size() <= 1, "%s is implemented by %s",
                            businessInterface.getSimpleName(), joinNamesOf(businessInterface.getAllSubClasses())));
                }

                private String joinNamesOf(Set<JavaClass> implementations) {
                    return FluentIterable.from(implementations).transform(new Function<JavaClass, String>() {
                        @Override
                        public String apply(JavaClass input) {
                            return input.getSimpleName();
                        }
                    }).join(Joiner.on(", "));
                }
            };
}
