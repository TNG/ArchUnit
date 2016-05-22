package com.tngtech.archunit.exampletest;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.lang.ArchCondition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.DescribedPredicate.are;
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

    private static final DescribedPredicate<JavaClass> STATELESS_SESSION_BEANS = new DescribedPredicate<JavaClass>() {
        @Override
        public Optional<String> getDescription() {
            return Optional.of("Stateless Session Beans");
        }

        @Override
        public boolean apply(JavaClass input) {
            return input.reflect().getAnnotation(Stateless.class) != null;
        }
    };

    private static final Predicate<JavaFieldAccess> ACCESS_ORIGIN_OUTSIDE_OF_CONSTRUCTION = new Predicate<JavaFieldAccess>() {
        @Override
        public boolean apply(JavaFieldAccess input) {
            return !input.getOrigin().isConstructor() &&
                    !input.getOrigin().tryGetAnnotationOfType(PostConstruct.class).isPresent();
        }
    };

    private static final ArchCondition<JavaClass> NO_FIELDS_ARE_SET_AFTER_CONSTRUCTION =
            never(classSetsFieldWith(ACCESS_ORIGIN_OUTSIDE_OF_CONSTRUCTION));
}
