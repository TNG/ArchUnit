package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static java.util.Collections.emptyList;
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

    String getMemberName() {
        return memberName;
    }

    Class<?> getDeclaringClass() {
        return clazz;
    }

    abstract String getExpectedDescription();

    @Override
    public String toString() {
        return String.format("%s.%s", clazz.getName(), memberName);
    }

    <T extends HasOwner<JavaClass> & HasName> boolean matches(T member) {
        return member.getOwner().isEquivalentTo(clazz) &&
                member.getName().equals(memberName) &&
                getParameters(member).equals(params);
    }

    private List<String> getParameters(Object member) {
        return member instanceof HasParameterTypes ?
                ((HasParameterTypes) member).getRawParameterTypes().getNames() :
                emptyList();
    }

    static class ExpectedOrigin extends ExpectedMember {
        private final String memberDescription;

        ExpectedOrigin(String memberDescription, Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            super(clazz, methodName, paramTypes);
            this.memberDescription = memberDescription;
        }

        @Override
        String getExpectedDescription() {
            return memberDescription + " <" + toString() + ">";
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
        String getExpectedDescription() {
            return accesses + String.format(" field <%s>", toString());
        }
    }

    static class ExpectedMethodTarget extends ExpectedTarget {
        ExpectedMethodTarget(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            super(clazz, memberName, paramTypes);
        }

        @Override
        String getExpectedDescription() {
            return String.format("calls method <%s>", toString());
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(getParams()));
        }
    }

    static class ExpectedConstructorTarget extends ExpectedTarget {
        ExpectedConstructorTarget(Class<?> clazz, Class<?>[] paramTypes) {
            super(clazz, CONSTRUCTOR_NAME, paramTypes);
        }

        @Override
        String getExpectedDescription() {
            return String.format("calls constructor <%s>", toString());
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(getParams()));
        }
    }
}
