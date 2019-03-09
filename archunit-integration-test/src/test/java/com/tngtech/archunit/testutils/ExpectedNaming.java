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
            return expectedSimpleName(String.format("does not start with '%s'", prefix));
        }

        public ExpectedMessage notEndingWith(String suffix) {
            return expectedSimpleName(String.format("does not end with '%s'", suffix));
        }

        public ExpectedMessage containing(String infix) {
            return expectedSimpleName(String.format("contains '%s'", infix));
        }

        private ExpectedMessage expectedSimpleName(String suffix) {
            return new ExpectedMessage(String.format("simple name of %s %s in (%s.java:0)",
                    className, suffix, simpleName));
        }
    }
}
