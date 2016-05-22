package com.tngtech.archunit.lang;

import java.io.Serializable;

import com.google.common.base.Predicate;
import org.junit.Test;

import static com.tngtech.archunit.lang.SimpleArchCondition.violationIf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class SimpleArchConditionTest {
    private static final Predicate<Object> INSTANCE_OF_SERIALIZABLE = new Predicate<Object>() {
        @Override
        public boolean apply(Object input) {
            return input instanceof Serializable;
        }
    };
    private static final SimpleArchCondition.Message<Object> ITEM_IS_SERIALIZABLE_MESSAGE = new SimpleArchCondition.Message<Object>() {
        @Override
        public String createFor(Object item) {
            return String.format("%s is instance of Serializable", item);
        }
    };

    @Test
    public void simple_violation() {
        SimpleArchCondition<Object> condition = violationIf(INSTANCE_OF_SERIALIZABLE).withMessage(ITEM_IS_SERIALIZABLE_MESSAGE);

        ConditionEvents events = new ConditionEvents();
        condition.check(new Object(), events);
        assertThat(events).containNoViolation();

        events = new ConditionEvents();
        Serializable evil = new Serializable() {
        };
        condition.check(evil, events);
        assertThat(events.getViolating()).hasSize(1);
        assertThat(events).containViolations(evil + " is instance of Serializable");
    }
}