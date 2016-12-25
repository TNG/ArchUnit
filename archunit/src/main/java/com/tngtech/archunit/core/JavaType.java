package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

class JavaType {
    private static final Map<Type, Class<?>> PRIMITIVE_CLASSES = ImmutableMap.<Type, Class<?>>builder()
            .put(VOID_TYPE, void.class)
            .put(BOOLEAN_TYPE, boolean.class)
            .put(CHAR_TYPE, char.class)
            .put(BYTE_TYPE, byte.class)
            .put(SHORT_TYPE, short.class)
            .put(INT_TYPE, int.class)
            .put(FLOAT_TYPE, float.class)
            .put(LONG_TYPE, long.class)
            .put(DOUBLE_TYPE, double.class)
            .build();

    private final String typeName;

    private JavaType(String typeName) {
        this.typeName = typeName;
    }

    static JavaType fromDescriptor(String descriptor) {
        Type type = Type.getObjectType(descriptor);
        if (PRIMITIVE_CLASSES.containsKey(type)) {
            return new JavaType(PRIMITIVE_CLASSES.get(type).getName());
        }
        if (type.getSort() == ARRAY) {
            return new JavaType(type.getClassName());
        }
        return new JavaType(type.getClassName());
    }

    Class<?> asClass() {
        return classForName(typeName);
    }

    public String getName() {
        return typeName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
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
        return Objects.equals(this.typeName, other.typeName);
    }

    @Override
    public String toString() {
        return "JavaType{typeName='" + typeName + "'}";
    }
}
