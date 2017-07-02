package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaFieldAccess;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static java.util.Collections.singleton;

abstract class ExpectedMember {
    private final Class<?> clazz;
    private final String memberName;
    private final List<String> params = new ArrayList<>();

    ExpectedMember(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
        this.clazz = clazz;
        this.memberName = memberName;
        for (Class<?> paramType : paramTypes) {
            params.add(paramType.getName());
        }
    }

    String lineMessage(int number) {
        return String.format("(%s.java:%d)", clazz.getSimpleName(), number);
    }

    List<String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return String.format("%s.%s", clazz.getName(), memberName);
    }

    static class ExpectedOrigin extends ExpectedMember {
        ExpectedOrigin(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            super(clazz, methodName, paramTypes);
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(getParams()));
        }
    }

    abstract static class ExpectedTarget extends ExpectedMember {
        private ExpectedTarget(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            super(clazz, memberName, paramTypes);
        }

        String messageFor(ExpectedAccess access) {
            return String.format(template(),
                    access.getOrigin(), access.getTarget(), access.getOrigin().lineMessage(access.getLineNumber()));
        }

        abstract String template();
    }

    static class ExpectedFieldTarget extends ExpectedTarget {
        private final Map<Set<JavaFieldAccess.AccessType>, String> accessDescription = ImmutableMap.of(
                singleton(JavaFieldAccess.AccessType.GET), "gets",
                singleton(JavaFieldAccess.AccessType.SET), "sets",
                EnumSet.of(JavaFieldAccess.AccessType.GET, JavaFieldAccess.AccessType.SET), "accesses"
        );

        private final String accesses;

        ExpectedFieldTarget(Class<?> clazz, String memberName, ImmutableSet<JavaFieldAccess.AccessType> accessTypes) {
            super(clazz, memberName, new Class<?>[0]);
            this.accesses = accessDescription.get(accessTypes);
        }

        @Override
        String template() {
            return "Method <%s> " + accesses + " field <%s> in %s";
        }
    }

    static class ExpectedMethodTarget extends ExpectedTarget {
        ExpectedMethodTarget(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            super(clazz, memberName, paramTypes);
        }

        @Override
        String template() {
            return "Method <%s> calls method <%s> in %s";
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(getParams()));
        }
    }

    static class ExpectedConstructorTarget extends ExpectedMethodTarget {
        ExpectedConstructorTarget(Class<?> clazz, Class<?>[] paramTypes) {
            super(clazz, CONSTRUCTOR_NAME, paramTypes);
        }

        @Override
        String template() {
            return "Method <%s> calls constructor <%s> in %s";
        }
    }
}
