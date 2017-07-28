package com.tngtech.archunit.junit;

public class ExpectedInheritance implements ExpectedDependency {
    private final Class<?> clazz;
    private final Class<?> superClass;
    private String inheritanceDescription;

    private ExpectedInheritance(Class<?> clazz, String inheritanceDescription, Class<?> superClass) {
        this.clazz = clazz;
        this.superClass = superClass;
        this.inheritanceDescription = inheritanceDescription;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s in (%s.java:0)",
                clazz.getName(), inheritanceDescription, superClass.getName(), clazz.getSimpleName());
    }

    public static Creator inheritanceFrom(Class<?> clazz) {
        return new Creator(clazz);
    }

    public static class Creator {
        private final Class<?> clazz;

        private Creator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedInheritance extending(Class<?> superClass) {
            return new ExpectedInheritance(clazz, "extends", superClass);
        }

        public ExpectedInheritance implementing(Class<?> anInterface) {
            return new ExpectedInheritance(clazz, "implements", anInterface);
        }
    }
}
