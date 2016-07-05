package com.tngtech.archunit.exampletest;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.FluentPredicate;
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
import static com.tngtech.archunit.lang.conditions.ArchConditions.classSetsFieldWith;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;

public class SessionBeanRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingSessionBeanRules.class);
    }

    @Ignore
    @Test
    public void stateless_session_beans_should_not_have_state() {
        all(classes.that(are(STATELESS_SESSION_BEANS))).should("not have state")
                .assertedBy(NO_FIELDS_ARE_SET_AFTER_CONSTRUCTION);
    }

    @Ignore
    @Test
    public void business_interface_implementations_should_be_unique() {
        all(classes.that(are(INTERFACES).as("Business Interfaces"))).should("have an unique implementation")
                .assertedBy(BUSINESS_INTERFACE_IMPLEMENTATION_IS_UNIQUE);
    }

    private static final DescribedPredicate<JavaClass> STATELESS_SESSION_BEANS = new DescribedPredicate<JavaClass>() {
        @Override
        public boolean apply(JavaClass input) {
            return input.reflect().getAnnotation(Stateless.class) != null;
        }
    }.as("Stateless Session Beans");

    private static final FluentPredicate<JavaFieldAccess> ACCESS_ORIGIN_OUTSIDE_OF_CONSTRUCTION = new FluentPredicate<JavaFieldAccess>() {
        @Override
        public boolean apply(JavaFieldAccess input) {
            return !input.getOrigin().isConstructor() &&
                    !input.getOrigin().tryGetAnnotationOfType(PostConstruct.class).isPresent();
        }
    };

    private static final ArchCondition<JavaClass> NO_FIELDS_ARE_SET_AFTER_CONSTRUCTION =
            never(classSetsFieldWith(ACCESS_ORIGIN_OUTSIDE_OF_CONSTRUCTION));

    public static final ArchCondition<JavaClass> BUSINESS_INTERFACE_IMPLEMENTATION_IS_UNIQUE = new ArchCondition<JavaClass>() {
        @Override
        public void check(JavaClass businessInterface, ConditionEvents events) {
            Set<JavaClass> implementations = new HashSet<>();
            for (JavaClass subClass : businessInterface.getAllSubClasses()) {
                if (isLocalBeanImplementation(subClass, businessInterface)) {
                    implementations.add(subClass);
                }
            }
            events.add(new ConditionEvent(implementations.size() <= 1, "%s is implemented by %s",
                    businessInterface.getSimpleName(), joinNamesOf(implementations)));
        }

        private boolean isLocalBeanImplementation(JavaClass bean, JavaClass businessInterfaceType) {
            return bean.isAnnotationPresent(Local.class)
                    && (bean.reflect().getAnnotation(Local.class).value()[0] == businessInterfaceType.reflect());
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
