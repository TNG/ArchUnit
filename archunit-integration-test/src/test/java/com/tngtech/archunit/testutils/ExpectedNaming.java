package com.tngtech.archunit.testutils;

public class ExpectedNaming {
    public static Creator simpleNameOf(Class<?> clazz) {
        return new Creator(clazz.getName(), clazz.getSimpleName());
    }

    public static Creator simpleNameOfAnonymousClassOf(Class<?> clazz) {
        return new Creator(clazz.getName() + "$1", clazz.getSimpleName());
    }

    public static class Creator {
        private final String className;
        private final String simpleName;

        private Creator(String className, String simpleName) {
            this.className = className;
            this.simpleName = simpleName;
        }

        public ExpectedMessage notStartingWith(String prefix) {
            return expectedClassViolation(String.format("does not have simple name starting with '%s'", prefix));
        }

        public ExpectedMessage notEndingWith(String suffix) {
            return expectedClassViolation(String.format("does not have simple name ending with '%s'", suffix));
        }

        public ExpectedMessage containing(String infix) {
            return expectedClassViolation(String.format("has simple name containing '%s'", infix));
        }

        private ExpectedMessage expectedClassViolation(String description) {
            return new ExpectedMessage(String.format("Class <%s> %s in (%s.java:0)",
                    className, description, simpleName));
        }
    }
}
