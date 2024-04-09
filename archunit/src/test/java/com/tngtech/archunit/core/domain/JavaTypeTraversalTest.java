package com.tngtech.archunit.core.domain;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.base.ForwardingList;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaTypeTraversalTest {

    @Test
    public void traverses_simple_class() {
        class SimpleClass {
        }

        JavaClass clazz = new ClassFileImporter().importClass(SimpleClass.class);

        List<JavaType> traversedClasses = new ArrayList<>();
        clazz.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitClass(JavaClass type) {
                traversedClasses.add(type);
                return Result.STOP;
            }
        });

        assertThatTypes(traversedClasses).matchExactly(SimpleClass.class);
    }

    @Test
    public void traverses_array_type() {
        class SimpleClass {
            @SuppressWarnings("unused")
            private SimpleClass[][] field;
        }

        JavaType arrayType = new ClassFileImporter().importClass(SimpleClass.class).getField("field").getType();

        List<JavaType> traversedClasses = new ArrayList<>();
        arrayType.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitClass(JavaClass type) {
                traversedClasses.add(type);
                return Result.STOP;
            }
        });

        assertThatTypes(traversedClasses).matchExactly(SimpleClass[][].class);
    }

    @Test
    public void traverses_type_variable() {
        @SuppressWarnings("unused")
        class SomeClass<T> {
            T field;
        }
        JavaTypeVariable<?> typeVariable = (JavaTypeVariable<?>) new ClassFileImporter()
                .importClass(SomeClass.class)
                .getField("field")
                .getType();

        List<JavaType> traversedTypes = new ArrayList<>();
        typeVariable.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitTypeVariable(JavaTypeVariable<?> type) {
                traversedTypes.add(type);
                return Result.STOP;
            }
        });

        assertThat(traversedTypes).containsOnly(typeVariable);
    }

    @Test
    public void traverses_parameterized_type() {
        @SuppressWarnings("unused")
        class SomeClass {
            List<String> field;
        }
        JavaParameterizedType parameterizedType = (JavaParameterizedType) new ClassFileImporter()
                .importClass(SomeClass.class)
                .getField("field")
                .getType();

        List<JavaType> traversedTypes = new ArrayList<>();
        parameterizedType.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitParameterizedType(JavaParameterizedType type) {
                traversedTypes.add(type);
                return Result.STOP;
            }
        });

        assertThat(traversedTypes).containsOnly(parameterizedType);
    }

    @Test
    public void traverses_wildcard_type() {
        @SuppressWarnings("unused")
        class SomeClass {
            List<?> field;
        }
        JavaParameterizedType parameterizedType = (JavaParameterizedType) new ClassFileImporter()
                .importClass(SomeClass.class)
                .getField("field")
                .getType();

        JavaWildcardType wildcardType = (JavaWildcardType) getOnlyElement(parameterizedType.getActualTypeArguments());

        List<JavaType> traversedTypes = new ArrayList<>();
        wildcardType.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitWildcardType(JavaWildcardType type) {
                traversedTypes.add(type);
                return Result.STOP;
            }
        });

        assertThat(traversedTypes).containsOnly(wildcardType);
    }

    @Test
    public void traverses_generic_array_type() {
        @SuppressWarnings("unused")
        class SomeClass<T> {
            T[] field;
        }
        JavaGenericArrayType genericArrayType = (JavaGenericArrayType) new ClassFileImporter()
                .importClass(SomeClass.class)
                .getField("field")
                .getType();

        List<JavaType> traversedTypes = new ArrayList<>();
        genericArrayType.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitGenericArrayType(JavaGenericArrayType type) {
                traversedTypes.add(type);
                return Result.STOP;
            }
        });

        assertThat(traversedTypes).containsOnly(genericArrayType);
    }

    @Test
    public void traverses_complex_signature_of_JavaClass() {
        @SuppressWarnings("unused")
        class SomeClass<T extends Map<? extends T[][], List<?>>, U extends List<? super int[][]>, V extends File & Set<?>> {
        }

        JavaClass classWithComplexSignature = new ClassFileImporter().importClass(SomeClass.class);

        List<JavaTypeVariable<JavaClass>> typeParameters = classWithComplexSignature.getTypeParameters();

        ExpectedTypes types = new ExpectedTypes();
        types.expect(classWithComplexSignature);
        // Type variable T
        JavaTypeVariable<JavaClass> typeVariableT = types.expect(typeParameters.get(0));
        JavaParameterizedType mapType = types.expect(typeVariableT.getUpperBounds().get(0));
        JavaWildcardType mapKeyWildCard = types.expect(mapType.getActualTypeArguments().get(0));
        JavaGenericArrayType genericArrayType2DimT = types.expect(mapKeyWildCard.getUpperBounds().get(0));
        types.expect(genericArrayType2DimT.getComponentType());
        JavaParameterizedType mapValueParameterizedTypeList = types.expect(mapType.getActualTypeArguments().get(1));
        types.expect(mapValueParameterizedTypeList.getActualTypeArguments().get(0));
        // Type variable U
        JavaTypeVariable<JavaClass> typeVariableU = types.expect(typeParameters.get(1));
        JavaParameterizedType listType = types.expect(typeVariableU.getUpperBounds().get(0));
        JavaWildcardType listWildCard = types.expect(listType.getActualTypeArguments().get(0));
        types.expect(listWildCard.getLowerBounds().get(0));
        // Type variable V
        JavaTypeVariable<JavaClass> typeVariableV = types.expect(typeParameters.get(2));
        types.expect(typeVariableV.getUpperBounds().get(0));
        JavaParameterizedType setType = types.expect(typeVariableV.getUpperBounds().get(1));
        types.expect(setType.getActualTypeArguments().get(0));
        // End expected types

        List<JavaType> traversedTypes = new ArrayList<>();
        classWithComplexSignature.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void traverses_complex_signature_of_JavaTypeVariable() {
        @SuppressWarnings("unused")
        class SomeClass<T extends Map<? extends T[][], List<?>>> {
        }

        JavaType typeVariable = new ClassFileImporter().importClass(SomeClass.class)
                .getTypeParameters().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaTypeVariable<JavaClass> typeVariableT = types.expect(typeVariable);
        JavaParameterizedType mapType = types.expect(typeVariableT.getUpperBounds().get(0));
        JavaWildcardType mapKeyWildCard = types.expect(mapType.getActualTypeArguments().get(0));
        JavaGenericArrayType genericArrayType2DimT = types.expect(mapKeyWildCard.getUpperBounds().get(0));
        types.expect(genericArrayType2DimT.getComponentType());
        JavaParameterizedType mapValueParameterizedTypeList = types.expect(mapType.getActualTypeArguments().get(1));
        types.expect(mapValueParameterizedTypeList.getActualTypeArguments().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        typeVariable.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void traverses_complex_signature_of_JavaParameterizedType() {
        @SuppressWarnings("unused")
        class SomeClass<T extends Map<? extends T[][], List<?>>> {
        }

        JavaType parameterizedType = new ClassFileImporter().importClass(SomeClass.class)
                .getTypeParameters().get(0).getUpperBounds().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaParameterizedType mapType = types.expect(parameterizedType);
        JavaWildcardType mapKeyWildCard = types.expect(mapType.getActualTypeArguments().get(0));
        JavaGenericArrayType genericArrayType2DimT = types.expect(mapKeyWildCard.getUpperBounds().get(0));
        JavaGenericArrayType genericArrayType1DimT = types.expect(genericArrayType2DimT.getComponentType());
        types.expect(genericArrayType1DimT.getComponentType());
        JavaParameterizedType mapValueParameterizedTypeList = types.expect(mapType.getActualTypeArguments().get(1));
        types.expect(mapValueParameterizedTypeList.getActualTypeArguments().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        parameterizedType.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void traverses_complex_signature_of_generic_array_type_from_type_variable() {
        @SuppressWarnings("unused")
        class SomeClass<T extends File> {
            T[][] field;
        }

        JavaType arrayType = new ClassFileImporter().importClass(SomeClass.class)
                .getField("field").getType();

        ExpectedTypes types = new ExpectedTypes();
        JavaGenericArrayType tArray2Dim = types.expect(arrayType);
        JavaGenericArrayType tArray1Dim = types.expect(tArray2Dim.getComponentType());
        JavaTypeVariable<?> typeVariableT = types.expect(tArray1Dim.getComponentType());
        types.expect(typeVariableT.getUpperBounds().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        arrayType.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void traverses_complex_signature_of_JavaWildcardType() {
        @SuppressWarnings("unused")
        class SomeClass<T extends Map<? extends T[][], List<?>>> {
        }

        JavaParameterizedType mapType = (JavaParameterizedType) new ClassFileImporter().importClass(SomeClass.class)
                .getTypeParameters().get(0).getUpperBounds().get(0);
        JavaType mapKeyWildcard = mapType.getActualTypeArguments().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaWildcardType mapKeyWildCard = types.expect(mapKeyWildcard);
        JavaGenericArrayType genericArrayType2DimT = types.expect(mapKeyWildCard.getUpperBounds().get(0));
        JavaGenericArrayType genericArrayType1DimT = types.expect(genericArrayType2DimT.getComponentType());
        types.expect(genericArrayType1DimT.getComponentType());
        // from here on we recurse through type variable in the signature
        types.expect(mapType);
        JavaParameterizedType mapValueParameterizedTypeList = types.expect(mapType.getActualTypeArguments().get(1));
        types.expect(mapValueParameterizedTypeList.getActualTypeArguments().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        mapKeyWildcard.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void does_not_traverse_into_type_variables_of_raw_type_in_signature() {
        @SuppressWarnings({"unused", "rawtypes"})
        class SomeClass<T extends Map> {
        }

        JavaType type = new ClassFileImporter().importClasses(SomeClass.class, Map.class)
                .get(SomeClass.class).getTypeParameters().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaTypeVariable<?> typeVariable = types.expect(type);
        types.expect(typeVariable.getUpperBounds().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        type.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void traverses_into_type_variables_of_method_signature_type() {
        @SuppressWarnings("unused")
        class SomeClass {
            <T extends List<?>> void method(Set<? extends T> param) {
            }
        }

        JavaType type = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", Set.class).getParameterTypes().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaParameterizedType setType = types.expect(type);
        JavaWildcardType wildcardType = types.expect(setType.getActualTypeArguments().get(0));
        JavaTypeVariable<?> typeVariableT = types.expect(wildcardType.getUpperBounds().get(0));
        JavaParameterizedType listType = types.expect(typeVariableT.getUpperBounds().get(0));
        types.expect(listType.getActualTypeArguments().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        type.traverseSignature(newTrackingVisitor(traversedTypes));

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    @Test
    public void stops_depth_first_traversal_once_STOP_is_received() {
        @SuppressWarnings("unused")
        class SomeClass {
            <T extends Map<String, File> & Serializable> void method(T param) {
            }
        }

        JavaType type = new ClassFileImporter().importClass(SomeClass.class)
                .getMethod("method", Map.class).getParameterTypes().get(0);

        ExpectedTypes types = new ExpectedTypes();
        JavaTypeVariable<?> typeVariableT = types.expect(type);
        types.expect(typeVariableT.getUpperBounds().get(0));

        List<JavaType> traversedTypes = new ArrayList<>();
        type.traverseSignature(new AllRejectingSignatureVisitor() {
            @Override
            public Result visitTypeVariable(JavaTypeVariable<?> type) {
                traversedTypes.add(type);
                return Result.CONTINUE;
            }

            @Override
            public Result visitParameterizedType(JavaParameterizedType type) {
                traversedTypes.add(type);
                return Result.STOP;
            }
        });

        assertThat(traversedTypes).containsExactlyElementsOf(types);
    }

    private static JavaType.SignatureVisitor newTrackingVisitor(List<JavaType> traversedTypes) {
        return new JavaType.SignatureVisitor() {
            @Override
            public Result visitClass(JavaClass type) {
                traversedTypes.add(type);
                return JavaType.SignatureVisitor.super.visitClass(type);
            }

            @Override
            public Result visitParameterizedType(JavaParameterizedType type) {
                traversedTypes.add(type);
                return JavaType.SignatureVisitor.super.visitParameterizedType(type);
            }

            @Override
            public Result visitTypeVariable(JavaTypeVariable<?> type) {
                traversedTypes.add(type);
                return JavaType.SignatureVisitor.super.visitTypeVariable(type);
            }

            @Override
            public Result visitGenericArrayType(JavaGenericArrayType type) {
                traversedTypes.add(type);
                return JavaType.SignatureVisitor.super.visitGenericArrayType(type);
            }

            @Override
            public Result visitWildcardType(JavaWildcardType type) {
                traversedTypes.add(type);
                return JavaType.SignatureVisitor.super.visitWildcardType(type);
            }
        };
    }

    private static class ExpectedTypes extends ForwardingList<JavaType> {
        private final List<JavaType> delegate = new ArrayList<>();

        @Override
        protected List<JavaType> delegate() {
            return delegate;
        }

        // some inherently unsafe syntactic sugar, trust the caller
        @SuppressWarnings("unchecked")
        <T extends JavaType> T expect(JavaType javaType) {
            add(javaType);
            return (T) javaType;
        }
    }

    private static class AllRejectingSignatureVisitor implements JavaType.SignatureVisitor {
        @Override
        public Result visitClass(JavaClass type) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public Result visitParameterizedType(JavaParameterizedType type) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public Result visitTypeVariable(JavaTypeVariable<?> type) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public Result visitGenericArrayType(JavaGenericArrayType type) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public Result visitWildcardType(JavaWildcardType type) {
            throw new UnsupportedOperationException("should not be called");
        }
    }
}
