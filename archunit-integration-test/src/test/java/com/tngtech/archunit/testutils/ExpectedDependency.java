package com.tngtech.archunit.testutils;

import com.tngtech.archunit.core.domain.Dependency;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static java.util.regex.Pattern.quote;

public class ExpectedDependency implements ExpectedRelation {
    private final Class<?> origin;
    private final Class<?> target;
    private String dependencyPattern;

    private ExpectedDependency(Class<?> origin, Class<?> target, String dependencyPattern) {
        this.origin = origin;
        this.target = target;
        this.dependencyPattern = dependencyPattern;
    }

    @Override
    public String toString() {
        return "Matches: " + dependencyPattern;
    }

    public static InheritanceCreator inheritanceFrom(Class<?> clazz) {
        return new InheritanceCreator(clazz);
    }

    public static AnnotationDependencyCreator annotatedClass(Class<?> clazz) {
        return new AnnotationDependencyCreator(clazz);
    }

    public static AccessCreator accessFrom(Class<?> clazz) {
        return new AccessCreator(clazz);
    }

    public static MemberDependencyCreator field(Class<?> owner, String fieldName) {
        return new MemberDependencyCreator(owner, fieldName);
    }

    public static MemberDependencyCreator method(Class<?> owner, String methodName) {
        return new MemberDependencyCreator(owner, methodName);
    }

    public static MemberDependencyCreator constructor(Class<?> owner) {
        return new MemberDependencyCreator(owner, CONSTRUCTOR_NAME);
    }

    @Override
    public void associateLines(LineAssociation association) {
        association.associateIfPatternMatches(dependencyPattern);
    }

    @Override
    public boolean correspondsTo(Object object) {
        if (!(object instanceof Dependency)) {
            return false;
        }

        Dependency dependency = (Dependency) object;
        boolean originMatches = dependency.getOriginClass().isEquivalentTo(origin);
        boolean targetMatches = dependency.getTargetClass().isEquivalentTo(target);
        boolean descriptionMatches = dependency.getDescription().matches(dependencyPattern);
        return originMatches && targetMatches && descriptionMatches;
    }

    private static String getDependencyPattern(String originName, String dependencyTypePattern, String targetName, int lineNumber) {
        return String.format(".*%s.*%s.*%s.*\\.java:%d.*", quote(originName), dependencyTypePattern, quote(targetName), lineNumber);
    }

    public static class InheritanceCreator {
        private final Class<?> clazz;

        private InheritanceCreator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedDependency extending(Class<?> superClass) {
            return new ExpectedDependency(clazz, superClass,
                    getDependencyPattern(clazz.getName(), "extends", superClass.getName(), 0));
        }

        public ExpectedDependency implementing(Class<?> anInterface) {
            return new ExpectedDependency(clazz, anInterface,
                    getDependencyPattern(clazz.getName(), "implements", anInterface.getName(), 0));
        }
    }

    public static class AccessCreator {
        private final Class<?> originClass;

        private AccessCreator(Class<?> originClass) {
            this.originClass = originClass;
        }

        public Step2 toFieldDeclaredIn(Class<?> clazz) {
            return new Step2(clazz, "(accesses|gets|sets)");
        }

        public Step2 toCodeUnitDeclaredIn(Class<?> clazz) {
            return new Step2(clazz, "calls");
        }

        public class Step2 {
            private final Class<?> targetClass;
            private final String accessPattern;

            Step2(Class<?> targetClass, String accessPattern) {
                this.targetClass = targetClass;
                this.accessPattern = accessPattern;
            }

            public ExpectedDependency inLineNumber(int lineNumber) {
                String dependencyPattern = getDependencyPattern(originClass.getName(), accessPattern, targetClass.getName(), lineNumber);
                return new ExpectedDependency(originClass, targetClass, dependencyPattern);
            }
        }
    }

    public static class MemberDependencyCreator {
        private final Class<?> owner;
        private final String memberName;

        MemberDependencyCreator(Class<?> owner, String memberName) {
            this.owner = owner;
            this.memberName = memberName;
        }

        public ExpectedDependency ofType(Class<?> type) {
            return new ExpectedDependency(owner, type, getDependencyPattern(getOriginName(), "has type", type.getName(), 0));
        }

        public ExpectedDependency withParameter(Class<?> targetParameter) {
            String dependencyPattern = getDependencyPattern(getOriginName(), "parameter of type", targetParameter.getName(), 0);
            return new ExpectedDependency(owner, targetParameter, dependencyPattern);
        }

        public ExpectedDependency withReturnType(Class<?> returnType) {
            String dependencyPattern = getDependencyPattern(getOriginName(), "return type", returnType.getName(), 0);
            return new ExpectedDependency(owner, returnType, dependencyPattern);
        }

        public ExpectedDependency withAnnotationType(Class<?> annotationType) {
            return new ExpectedDependency(owner, annotationType, getDependencyPattern(getOriginName(), "is annotated with", annotationType.getName(), 0));
        }

        private String getOriginName() {
            return owner.getName() + "." + memberName;
        }
    }

    public static class AnnotationDependencyCreator {
        private final Class<?> owner;

        AnnotationDependencyCreator(Class<?> owner) {
            this.owner = owner;
        }

        public ExpectedDependency annotatedWith(Class<?> annotationType) {
            String dependencyPattern = getDependencyPattern(owner.getName(), "is annotated with", annotationType.getName(), 0);
            return new ExpectedDependency(owner, annotationType, dependencyPattern);
        }

        public ExpectedDependency withAnnotationParameterType(Class<?> type) {
            String dependencyPattern = getDependencyPattern(owner.getName(), "has annotation member of type", type.getName(), 0);
            return new ExpectedDependency(owner, type, dependencyPattern);
        }
    }
}
