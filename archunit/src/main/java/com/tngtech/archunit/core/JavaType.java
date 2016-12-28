package com.tngtech.archunit.core;

import java.util.Objects;

import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.core.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.ReflectionUtils.ensureCorrectArrayTypeName;

public interface JavaType {
    String getName();

    String getSimpleName();

    String getPackage();

    class From {
        static JavaType name(String typeName) {
            // NOTE: ASM uses the canonical name for arrays (i.e. java.lang.Object[]), but we want the class name,
            //       i.e. [Ljava.lang.Object;
            typeName = ensureCorrectArrayTypeName(typeName);
            return new AbstractType(typeName);
        }

        /**
         * Takes an 'internal' ASM object type name, i.e. the class name but with slashes instead of periods,
         * i.e. java/lang/Object (note that this is not a descriptor like Ljava/lang/Object;)
         */
        static JavaType fromAsmObjectTypeName(String objectTypeName) {
            return new AbstractType(objectTypeName.replace("/", "."));
        }

        static JavaType asmType(Type type) {
            return name(type.getClassName());
        }

        private static class AbstractType implements JavaType {
            private final String name;
            private final String simpleName;
            private final String javaPackage;

            AbstractType(String fullName) {
                checkArgument(fullName.matches("(\\[+L)?(\\w+\\.)*(\\w|\\$)+;?$") || fullName.matches("\\[+\\w"), "Full name %s is invalid", fullName);

                this.name = fullName;
                this.simpleName = createSimpleName(fullName);
                this.javaPackage = isArray(fullName) ? "" : fullName.replaceAll("\\.?[^.]*$", "");
            }

            private String createSimpleName(String fullName) {
                fullName = !fullName.startsWith("L") && !isArray(fullName) ? "L" + fullName + ";" : fullName;
                return ensureSimpleName(Type.getType(fullName).getClassName());
            }

            private boolean isArray(String fullName) {
                return fullName.startsWith("[");
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSimpleName() {
                return simpleName;
            }

            @Override
            public String getPackage() {
                return javaPackage;
            }

            @Override
            public int hashCode() {
                return Objects.hash(name);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                final JavaType other = (JavaType) obj;
                return Objects.equals(this.getName(), other.getName());
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "{" + getName() + "}";
            }
        }
    }
}
