package com.tngtech.archunit.testutils;

import com.tngtech.archunit.core.domain.JavaModifier;

import static java.lang.String.format;

public class ExpectedField {

    public static ExpectedField.Creator of(Class<?> owner, String fieldName) {
        return new ExpectedField.Creator(owner, fieldName);
    }

    public static class Creator {
        private final Class<?> clazz;
        private final String fieldName;

        private Creator(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        public ExpectedMessage doesNotHaveModifier(JavaModifier modifier) {
            return field("does not have modifier " + modifier);
        }

        private ExpectedMessage field(String message) {
            String fieldDescription = format("Field <%s.%s>", clazz.getName(), fieldName);
            String sourceCodeLocation = format("(%s.java:0)", clazz.getSimpleName());
            return new ExpectedMessage(format("%s %s in %s", fieldDescription, message, sourceCodeLocation));
        }
    }
}
