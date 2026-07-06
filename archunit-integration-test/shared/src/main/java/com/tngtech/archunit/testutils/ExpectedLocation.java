package com.tngtech.archunit.testutils;

public class ExpectedLocation {
    public static Creator javaClass(Class<?> clazz) {
        return new Creator(clazz);
    }

    public static class Creator {
        private final Class<?> clazz;

        private Creator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedMessage notResidingIn(String packageIdentifier) {
            String expectedMessage = String.format(
                    "Class <%s> does not reside in a package '%s' in (%s.java:0)",
                    clazz.getName(), packageIdentifier, clazz.getSimpleName());
            return new ExpectedMessage(expectedMessage);
        }
    }
}
