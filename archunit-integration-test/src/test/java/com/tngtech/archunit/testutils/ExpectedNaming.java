package com.tngtech.archunit.testutils;

public class ExpectedNaming {
    public static Creator simpleNameOf(Class<?> clazz) {
        return new Creator(clazz);
    }

    public static class Creator {
        private final Class<?> clazz;

        private Creator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedMessage notStartingWith(String prefix) {
            return expectedSimpleName(String.format("doesn't start with '%s'", prefix));
        }

        public ExpectedMessage notEndingWith(String suffix) {
            return expectedSimpleName(String.format("doesn't end with '%s'", suffix));
        }

        public ExpectedMessage containing(String infix) {
            return expectedSimpleName(String.format("contains '%s'", infix));
        }

        private ExpectedMessage expectedSimpleName(String suffix) {
            return new ExpectedMessage(String.format("simple name of %s %s in (%s.java:0)",
                    clazz.getName(), suffix, clazz.getSimpleName()));
        }
    }
}
