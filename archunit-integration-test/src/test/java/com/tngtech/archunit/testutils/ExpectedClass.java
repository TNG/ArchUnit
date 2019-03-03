package com.tngtech.archunit.testutils;

public class ExpectedClass {
    public static Creator javaClass(Class<?> clazz) {
        return new Creator(clazz);
    }

    public static class Creator {
        private final Class<?> clazz;

        private Creator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedMessage notBeing(Class<?> desiredClass) {
            String expectedMessage = String.format(
                    "Class <%s> is not %s in (%s.java:0)",
                    clazz.getName(), desiredClass.getName(), clazz.getSimpleName());
            return new ExpectedMessage(expectedMessage);
        }

        public ExpectedMessage being(Class<?> desiredClass) {
            String expectedMessage = String.format(
                    "Class <%s> is %s in (%s.java:0)",
                    clazz.getName(), desiredClass.getName(), clazz.getSimpleName());
            return new ExpectedMessage(expectedMessage);
        }
    }
}
