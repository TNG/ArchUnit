/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder.ValueBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.JavaCodeUnitSignatureImporter.JavaCodeUnitSignature;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isAsmMethodHandle;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isLambdaMetafactory;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isLambdaMethod;
import static com.tngtech.archunit.core.importer.RawInstanceofCheck.from;

class JavaClassProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassProcessor.class);

    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private DomainBuilders.JavaClassBuilder javaClassBuilder;
    private final Set<JavaAnnotationBuilder> annotations = new HashSet<>();
    private final SourceDescriptor sourceDescriptor;
    private final DeclarationHandler declarationHandler;
    private final AccessHandler accessHandler;
    private String className;

    JavaClassProcessor(SourceDescriptor sourceDescriptor, DeclarationHandler declarationHandler) {
        this(sourceDescriptor, declarationHandler, NO_OP);
    }

    JavaClassProcessor(SourceDescriptor sourceDescriptor, DeclarationHandler declarationHandler, AccessHandler accessHandler) {
        super(ASM_API_VERSION);
        this.sourceDescriptor = sourceDescriptor;
        this.declarationHandler = declarationHandler;
        this.accessHandler = accessHandler;
    }

    Optional<JavaClass> createJavaClass() {
        return javaClassBuilder != null ? Optional.of(javaClassBuilder.build()) : Optional.<JavaClass>empty();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.debug("Processing class '{}'", name);
        JavaClassDescriptor descriptor = JavaClassDescriptorImporter.createFromAsmObjectTypeName(name);
        if (alreadyImported(descriptor)) {
            return;
        }

        List<String> interfaceNames = createInterfaceNames(interfaces);
        LOG.trace("Found interfaces {} on class '{}'", interfaceNames, name);
        boolean opCodeForInterfaceIsPresent = (access & Opcodes.ACC_INTERFACE) != 0;
        boolean opCodeForEnumIsPresent = (access & Opcodes.ACC_ENUM) != 0;
        boolean opCodeForAnnotationIsPresent = (access & Opcodes.ACC_ANNOTATION) != 0;
        Optional<String> superclassName = getSuperclassName(superName, opCodeForInterfaceIsPresent);
        LOG.trace("Found superclass {} on class '{}'", superclassName.orElse(null), name);

        javaClassBuilder = new DomainBuilders.JavaClassBuilder()
                .withSourceDescriptor(sourceDescriptor)
                .withDescriptor(descriptor)
                .withInterface(opCodeForInterfaceIsPresent)
                .withEnum(opCodeForEnumIsPresent)
                .withAnnotation(opCodeForAnnotationIsPresent)
                .withModifiers(JavaModifier.getModifiersForClass(access));

        className = descriptor.getFullyQualifiedClassName();
        declarationHandler.onNewClass(className, superclassName, interfaceNames);
        JavaClassSignatureImporter.parseAsmTypeSignature(signature, declarationHandler);
    }

    private boolean alreadyImported(JavaClassDescriptor descriptor) {
        return !declarationHandler.isNew(descriptor.getFullyQualifiedClassName());
    }

    // NOTE: For some reason ASM claims superName == java/lang/Object for Interfaces???
    //       This is inconsistent with the behavior of Class.getSuperclass()
    private Optional<String> getSuperclassName(String superName, boolean isInterface) {
        return superName != null && !isInterface ?
                Optional.of(createTypeName(superName)) :
                Optional.<String>empty();
    }

    private boolean importAborted() {
        return javaClassBuilder == null;
    }

    @Override
    public void visitSource(String source, String debug) {
        if (!importAborted() && source != null) {
            javaClassBuilder.withSourceFileName(source);
        }
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        javaClassBuilder.withRecord(true);

        // Records are implicitly static and final (compare JLS 8.10 Record Declarations)
        // Thus we ensure that those modifiers are always present (the access flag in visit(..) does not contain STATIC)
        ImmutableSet<JavaModifier> recordModifiers = ImmutableSet.<JavaModifier>builder()
                .addAll(javaClassBuilder.getModifiers())
                .add(JavaModifier.STATIC, JavaModifier.FINAL)
                .build();
        javaClassBuilder.withModifiers(recordModifiers);

        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (importAborted()) {
            return;
        }

        String innerTypeName = createTypeName(name);
        if (!visitingCurrentClass(innerTypeName)) {
            return;
        }

        javaClassBuilder.withSimpleName(nullToEmpty(innerName));

        // Javadoc for innerName: "May be null for anonymous inner classes."
        boolean isAnonymousClass = innerName == null;
        javaClassBuilder.withAnonymousClass(isAnonymousClass);

        // Javadoc for outerName: "May be null for not member classes."
        boolean isMemberClass = outerName != null;
        javaClassBuilder.withMemberClass(isMemberClass);

        if (isMemberClass) {
            javaClassBuilder.withModifiers(JavaModifier.getModifiersForClass(access));
            declarationHandler.registerEnclosingClass(innerTypeName, createTypeName(outerName));
        }
    }

    // visitInnerClass is called for named inner classes, even if we are currently importing
    // this class itself (i.e. visit(..) and visitInnerClass(..) are called with the same class name.
    // visitInnerClass offers some more properties like correct modifiers.
    // Modifier handling is somewhat counter intuitive for nested named classes, even though we 'visit' the nested class
    // like any outer class in visit(..) before, the modifiers like 'PUBLIC' or 'PRIVATE'
    // are found in the access flags of visitInnerClass(..)
    private boolean visitingCurrentClass(String innerTypeName) {
        return innerTypeName.equals(className);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        if (importAborted()) {
            return;
        }

        declarationHandler.registerEnclosingClass(className, createTypeName(owner));

        if (name != null && desc != null) {
            JavaClassDescriptor ownerType = JavaClassDescriptorImporter.createFromAsmObjectTypeName(owner);
            CodeUnit codeUnit = new CodeUnit(name, desc, ownerType.getFullyQualifiedClassName());
            declarationHandler.registerEnclosingCodeUnit(className, codeUnit);
        }
    }

    private List<String> createInterfaceNames(String[] interfaces) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (String i : interfaces) {
            result.add(createTypeName(i));
        }
        return result.build();
    }

    private String createTypeName(String name) {
        return name.replace("/", ".");
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (importAborted()) {
            return super.visitField(access, name, desc, signature, value);
        }

        JavaClassDescriptor rawType = JavaClassDescriptorImporter.importAsmTypeFromDescriptor(desc);
        Optional<JavaTypeCreationProcess<JavaField>> genericType = JavaFieldTypeSignatureImporter.parseAsmFieldTypeSignature(signature);
        DomainBuilders.JavaFieldBuilder fieldBuilder = new DomainBuilders.JavaFieldBuilder()
                .withName(name)
                .withType(genericType, rawType)
                .withModifiers(JavaModifier.getModifiersForField(access))
                .withDescriptor(desc);
        declarationHandler.onDeclaredField(fieldBuilder, rawType.getFullyQualifiedClassName());
        return new FieldProcessor(fieldBuilder, declarationHandler);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (importAborted()) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        LOG.trace("Analyzing method {}.{}:{}", className, name, desc);
        CodeUnit codeUnit = new CodeUnit(name, desc, className);
        accessHandler.setContext(codeUnit);

        JavaClassDescriptor rawReturnType = JavaClassDescriptorImporter.importAsmMethodReturnType(desc);
        DomainBuilders.JavaCodeUnitBuilder<?, ?> codeUnitBuilder = addCodeUnitBuilder(name, codeUnit.getRawParameterTypeNames(), rawReturnType.getFullyQualifiedClassName());
        JavaCodeUnitSignature codeUnitSignature = JavaCodeUnitSignatureImporter.parseAsmMethodSignature(signature);
        List<JavaClassDescriptor> throwsDeclarations = typesFrom(exceptions);
        codeUnitBuilder
                .withName(name)
                .withModifiers(JavaModifier.getModifiersForMethod(access))
                .withTypeParameters(codeUnitSignature.getTypeParameterBuilders())
                .withParameterTypes(codeUnitSignature.getParameterTypes(), codeUnit.getRawParameterTypes())
                .withReturnType(codeUnitSignature.getReturnType(), rawReturnType)
                .withDescriptor(desc)
                .withThrowsClause(throwsDeclarations);
        declarationHandler.onDeclaredThrowsClause(fullyQualifiedClassNamesOf(throwsDeclarations));

        return new MethodProcessor(className, accessHandler, codeUnitBuilder, declarationHandler);
    }

    private Collection<String> fullyQualifiedClassNamesOf(List<JavaClassDescriptor> classDescriptors) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (JavaClassDescriptor classDescriptor : classDescriptors) {
            result.add(classDescriptor.getFullyQualifiedClassName());
        }
        return result.build();
    }

    private List<JavaClassDescriptor> typesFrom(String[] throwsDeclarations) {
        List<JavaClassDescriptor> result = new ArrayList<>();
        if (throwsDeclarations != null) {
            for (String throwsDeclaration : throwsDeclarations) {
                result.add(JavaClassDescriptorImporter.createFromAsmObjectTypeName(throwsDeclaration));
            }
        }
        return result;
    }

    private DomainBuilders.JavaCodeUnitBuilder<?, ?> addCodeUnitBuilder(String name, Collection<String> rawParameterTypeNames, String rawReturnTypeName) {
        if (CONSTRUCTOR_NAME.equals(name)) {
            DomainBuilders.JavaConstructorBuilder builder = new DomainBuilders.JavaConstructorBuilder();
            declarationHandler.onDeclaredConstructor(builder, rawParameterTypeNames);
            return builder;
        } else if (STATIC_INITIALIZER_NAME.equals(name)) {
            DomainBuilders.JavaStaticInitializerBuilder builder = new DomainBuilders.JavaStaticInitializerBuilder();
            declarationHandler.onDeclaredStaticInitializer(builder);
            return builder;
        } else {
            DomainBuilders.JavaMethodBuilder builder = new DomainBuilders.JavaMethodBuilder();
            declarationHandler.onDeclaredMethod(builder, rawParameterTypeNames, rawReturnTypeName);
            return builder;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (importAborted()) {
            return super.visitAnnotation(desc, visible);
        }

        return new AnnotationProcessor(addAnnotationTo(annotations), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
    }

    @Override
    public void visitEnd() {
        if (importAborted()) {
            return;
        }

        declarationHandler.onDeclaredClassAnnotations(annotations);
        LOG.trace("Done analyzing {}", className);
    }

    private static class MethodProcessor extends MethodVisitor {
        private final String declaringClassName;
        private final AccessHandler accessHandler;
        private final DomainBuilders.JavaCodeUnitBuilder<?, ?> codeUnitBuilder;
        private final DeclarationHandler declarationHandler;
        private final Set<JavaAnnotationBuilder> annotations = new HashSet<>();
        private final SetMultimap<Integer, JavaAnnotationBuilder> parameterAnnotationsByIndex = HashMultimap.create();
        private int actualLineNumber;

        MethodProcessor(String declaringClassName, AccessHandler accessHandler, DomainBuilders.JavaCodeUnitBuilder<?, ?> codeUnitBuilder, DeclarationHandler declarationHandler) {
            super(ASM_API_VERSION);
            this.declaringClassName = declaringClassName;
            this.accessHandler = accessHandler;
            this.codeUnitBuilder = codeUnitBuilder;
            this.declarationHandler = declarationHandler;
            codeUnitBuilder.withParameterAnnotations(parameterAnnotationsByIndex);
        }

        @Override
        public void visitCode() {
            actualLineNumber = 0;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationAtIndex(parameterAnnotationsByIndex, parameter), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
        }

        // NOTE: ASM does not reliably visit this method, so if this method is skipped, line number 0 is recorded
        @Override
        public void visitLineNumber(int line, Label start) {
            LOG.trace("Examining line number {}", line);
            codeUnitBuilder.recordLineNumber(line);
            actualLineNumber = line;
            accessHandler.setLineNumber(actualLineNumber);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (JavaClassDescriptorImporter.isAsmType(value)) {
                JavaClassDescriptor type = JavaClassDescriptorImporter.importAsmType(value);
                codeUnitBuilder.addReferencedClassObject(RawReferencedClassObject.from(type, actualLineNumber));
                declarationHandler.onDeclaredClassObject(type.getFullyQualifiedClassName());
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            accessHandler.handleFieldInstruction(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            accessHandler.handleMethodInstruction(owner, name, desc);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.INSTANCEOF) {
                JavaClassDescriptor instanceOfCheckType = JavaClassDescriptorImporter.createFromAsmObjectTypeName(type);
                codeUnitBuilder.addInstanceOfCheck(from(instanceOfCheckType, actualLineNumber));
                declarationHandler.onDeclaredInstanceofCheck(instanceOfCheckType.getFullyQualifiedClassName());
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationDefaultProcessor(declaringClassName, codeUnitBuilder, declarationHandler);
        }

        @Override
        public void visitInvokeDynamicInsn(
                final String name,
                final String descriptor,
                final Handle bootstrapMethodHandle,
                final Object... bootstrapMethodArguments
        ) {
            if (isLambdaMetafactory(bootstrapMethodHandle.getOwner())) {
                Object methodHandleCandidate = bootstrapMethodArguments[1];
                if (isAsmMethodHandle(methodHandleCandidate)) {
                    processLambdaMetafactoryMethodHandleArgument((Handle) methodHandleCandidate);
                }
            }
        }

        private void processLambdaMetafactoryMethodHandleArgument(Handle methodHandle) {
            if (!isLambdaMethod(methodHandle)) {
                accessHandler.handleMethodReferenceInstruction(methodHandle.getOwner(), methodHandle.getName(), methodHandle.getDesc());
            }
        }

        @Override
        public void visitEnd() {
            declarationHandler.onDeclaredMemberAnnotations(codeUnitBuilder.getName(), codeUnitBuilder.getDescriptor(), annotations);
        }

        private static class AnnotationDefaultProcessor extends ClassAndPrimitiveDistinguishingAnnotationVisitor {
            private final String annotationTypeName;
            private final DeclarationHandler declarationHandler;
            private final DomainBuilders.JavaMethodBuilder methodBuilder;

            AnnotationDefaultProcessor(String annotationTypeName, DomainBuilders.JavaCodeUnitBuilder<?, ?> codeUnitBuilder, DeclarationHandler declarationHandler) {
                this.annotationTypeName = annotationTypeName;
                this.declarationHandler = declarationHandler;
                checkArgument(codeUnitBuilder instanceof DomainBuilders.JavaMethodBuilder,
                        "tried to import annotation defaults for code unit '%s' that is not a method " +
                                "(as any annotation.property() is assumed to be), " +
                                "this is likely a bug", codeUnitBuilder.getName());

                methodBuilder = (DomainBuilders.JavaMethodBuilder) codeUnitBuilder;
            }

            @Override
            void visitClass(String name, JavaClassDescriptor clazz) {
                declarationHandler.onDeclaredAnnotationDefaultValue(methodBuilder.getName(), methodBuilder.getDescriptor(), handleAnnotationClassProperty(clazz, declarationHandler));
                declarationHandler.onDeclaredAnnotationValueType(clazz.getFullyQualifiedClassName());
            }

            @Override
            void visitPrimitive(String name, Object value) {
                declarationHandler.onDeclaredAnnotationDefaultValue(methodBuilder.getName(), methodBuilder.getDescriptor(), ValueBuilder.fromPrimitiveProperty(value));
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                declarationHandler.onDeclaredAnnotationDefaultValue(methodBuilder.getName(), methodBuilder.getDescriptor(), handleAnnotationEnumProperty(desc, value, declarationHandler));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String desc) {
                return new AnnotationProcessor(new SetAsAnnotationDefault(annotationTypeName, methodBuilder, declarationHandler), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return new AnnotationArrayProcessor(new SetAsAnnotationDefault(annotationTypeName, methodBuilder, declarationHandler), declarationHandler);
            }
        }
    }

    private static class SetAsAnnotationDefault implements TakesAnnotationBuilder, AnnotationArrayContext {
        private final String annotationTypeName;
        private final DomainBuilders.JavaMethodBuilder methodBuilder;
        private final DeclarationHandler declarationHandler;

        private SetAsAnnotationDefault(String annotationTypeName, DomainBuilders.JavaMethodBuilder methodBuilder, DeclarationHandler declarationHandler) {
            this.annotationTypeName = annotationTypeName;
            this.methodBuilder = methodBuilder;
            this.declarationHandler = declarationHandler;
        }

        @Override
        public void add(JavaAnnotationBuilder annotation) {
            setArrayResult(ValueBuilder.fromAnnotationProperty(annotation));
        }

        @Override
        public String getDeclaringAnnotationTypeName() {
            return annotationTypeName;
        }

        @Override
        public String getDeclaringAnnotationMemberName() {
            return methodBuilder.getName();
        }

        @Override
        public void setArrayResult(ValueBuilder valueBuilder) {
            declarationHandler.onDeclaredAnnotationDefaultValue(methodBuilder.getName(), methodBuilder.getDescriptor(), valueBuilder);
        }
    }

    interface DeclarationHandler {
        boolean isNew(String className);

        void onNewClass(String className, Optional<String> superclassName, List<String> interfaceNames);

        void onDeclaredTypeParameters(DomainBuilders.JavaClassTypeParametersBuilder typeParametersBuilder);

        void onGenericSuperclass(DomainBuilders.JavaParameterizedTypeBuilder<JavaClass> genericSuperclassBuilder);

        void onGenericInterfaces(List<DomainBuilders.JavaParameterizedTypeBuilder<JavaClass>> genericInterfaceBuilders);

        void onDeclaredField(DomainBuilders.JavaFieldBuilder fieldBuilder, String fieldTypeName);

        void onDeclaredConstructor(DomainBuilders.JavaConstructorBuilder constructorBuilder, Collection<String> rawParameterTypeNames);

        void onDeclaredMethod(DomainBuilders.JavaMethodBuilder methodBuilder, Collection<String> rawParameterTypeNames, String rawReturnTypeName);

        void onDeclaredStaticInitializer(DomainBuilders.JavaStaticInitializerBuilder staticInitializerBuilder);

        void onDeclaredClassAnnotations(Set<JavaAnnotationBuilder> annotationBuilders);

        void onDeclaredMemberAnnotations(String memberName, String descriptor, Set<JavaAnnotationBuilder> annotations);

        void onDeclaredAnnotationValueType(String valueTypeName);

        void onDeclaredAnnotationDefaultValue(String methodName, String methodDescriptor, ValueBuilder valueBuilder);

        void registerEnclosingClass(String ownerName, String enclosingClassName);

        void registerEnclosingCodeUnit(String ownerName, CodeUnit enclosingCodeUnit);

        void onDeclaredClassObject(String typeName);

        void onDeclaredInstanceofCheck(String typeName);

        void onDeclaredThrowsClause(Collection<String> exceptionTypeNames);
    }

    interface AccessHandler {
        void handleFieldInstruction(int opcode, String owner, String name, String desc);

        void setContext(CodeUnit codeUnit);

        void setLineNumber(int lineNumber);

        void handleMethodInstruction(String owner, String name, String desc);

        void handleMethodReferenceInstruction(String owner, String name, String desc);

        @Internal
        class NoOp implements AccessHandler {
            @Override
            public void handleFieldInstruction(int opcode, String owner, String name, String desc) {
            }

            @Override
            public void setContext(CodeUnit codeUnit) {
            }

            @Override
            public void setLineNumber(int lineNumber) {
            }

            @Override
            public void handleMethodInstruction(String owner, String name, String desc) {
            }

            @Override
            public void handleMethodReferenceInstruction(String owner, String name, String desc) {
            }
        }
    }

    private static class FieldProcessor extends FieldVisitor {
        private final DomainBuilders.JavaFieldBuilder fieldBuilder;
        private final DeclarationHandler declarationHandler;
        private final Set<JavaAnnotationBuilder> annotations = new HashSet<>();

        private FieldProcessor(DomainBuilders.JavaFieldBuilder fieldBuilder, DeclarationHandler declarationHandler) {
            super(ASM_API_VERSION);

            this.fieldBuilder = fieldBuilder;
            this.declarationHandler = declarationHandler;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
        }

        @Override
        public void visitEnd() {
            declarationHandler.onDeclaredMemberAnnotations(fieldBuilder.getName(), fieldBuilder.getDescriptor(), annotations);
        }
    }

    private static class AnnotationProcessor extends ClassAndPrimitiveDistinguishingAnnotationVisitor {
        private final TakesAnnotationBuilder takesAnnotationBuilder;
        private final DeclarationHandler declarationHandler;
        private final JavaAnnotationBuilder annotationBuilder;

        private AnnotationProcessor(TakesAnnotationBuilder takesAnnotationBuilder, DeclarationHandler declarationHandler, JavaAnnotationBuilder annotationBuilder) {
            this.takesAnnotationBuilder = takesAnnotationBuilder;
            this.declarationHandler = declarationHandler;
            this.annotationBuilder = annotationBuilder;
        }

        @Override
        void visitClass(String name, JavaClassDescriptor clazz) {
            annotationBuilder.addProperty(name, handleAnnotationClassProperty(clazz, declarationHandler));
        }

        @Override
        void visitPrimitive(String name, Object value) {
            annotationBuilder.addProperty(name, ValueBuilder.fromPrimitiveProperty(value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            annotationBuilder.addProperty(name, handleAnnotationEnumProperty(desc, value, declarationHandler));
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String name, String desc) {
            return new AnnotationProcessor(addAnnotationAsProperty(name, this.annotationBuilder), declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
            return new AnnotationArrayProcessor(new AnnotationArrayContext() {
                @Override
                public String getDeclaringAnnotationTypeName() {
                    return annotationBuilder.getFullyQualifiedClassName();
                }

                @Override
                public String getDeclaringAnnotationMemberName() {
                    return name;
                }

                @Override
                public void setArrayResult(ValueBuilder valueBuilder) {
                    annotationBuilder.addProperty(name, valueBuilder);
                }
            }, declarationHandler);
        }

        @Override
        public void visitEnd() {
            takesAnnotationBuilder.add(annotationBuilder);
        }
    }

    private static TakesAnnotationBuilder addAnnotationTo(final Collection<? super JavaAnnotationBuilder> collection) {
        return new TakesAnnotationBuilder() {
            @Override
            public void add(JavaAnnotationBuilder annotation) {
                collection.add(annotation);
            }
        };
    }

    private static TakesAnnotationBuilder addAnnotationAtIndex(final SetMultimap<Integer, JavaAnnotationBuilder> annotations, final int index) {
        return new TakesAnnotationBuilder() {
            @Override
            public void add(JavaAnnotationBuilder annotation) {
                annotations.put(index, annotation);
            }
        };
    }

    private static TakesAnnotationBuilder addAnnotationAsProperty(final String name, final JavaAnnotationBuilder annotationBuilder) {
        return new TakesAnnotationBuilder() {
            @Override
            public void add(JavaAnnotationBuilder builder) {
                annotationBuilder.addProperty(name, ValueBuilder.fromAnnotationProperty(builder));
            }
        };
    }

    private interface TakesAnnotationBuilder {
        void add(JavaAnnotationBuilder annotation);
    }

    private static class AnnotationArrayProcessor extends ClassAndPrimitiveDistinguishingAnnotationVisitor {
        private final AnnotationArrayContext annotationArrayContext;
        private final DeclarationHandler declarationHandler;
        private Class<?> derivedComponentType;
        private final List<ValueBuilder> values = new ArrayList<>();

        private AnnotationArrayProcessor(AnnotationArrayContext annotationArrayContext, DeclarationHandler declarationHandler) {
            this.annotationArrayContext = annotationArrayContext;
            this.declarationHandler = declarationHandler;
        }

        @Override
        void visitClass(String name, JavaClassDescriptor clazz) {
            setDerivedComponentType(JavaClass.class);
            values.add(handleAnnotationClassProperty(clazz, declarationHandler));
        }

        @Override
        void visitPrimitive(String name, Object value) {
            setDerivedComponentType(value.getClass());
            values.add(ValueBuilder.fromPrimitiveProperty(value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            setDerivedComponentType(JavaAnnotation.class);
            return new AnnotationProcessor(new TakesAnnotationBuilder() {
                @Override
                public void add(JavaAnnotationBuilder annotationBuilder) {
                    values.add(ValueBuilder.fromAnnotationProperty(annotationBuilder));
                }
            }, declarationHandler, handleAnnotationAnnotationProperty(desc, declarationHandler));
        }

        @Override
        public void visitEnum(String name, final String desc, final String value) {
            setDerivedComponentType(JavaEnumConstant.class);
            values.add(handleAnnotationEnumProperty(desc, value, declarationHandler));
        }

        // NOTE: If the declared annotation is not imported itself, we can still use this heuristic,
        //       to determine additional information about the respective array.
        //       (If the annotation is imported itself, we could easily determine this from the respective
        //       JavaClass methods)
        private void setDerivedComponentType(Class<?> type) {
            checkState(derivedComponentType == null || derivedComponentType.equals(type),
                    "Found mixed component types while importing array, this is most likely a bug");

            derivedComponentType = type;
        }

        @Override
        public void visitEnd() {
            annotationArrayContext.setArrayResult(new ArrayValueBuilder());
        }

        private class ArrayValueBuilder extends ValueBuilder {
            @Override
            public <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses importContext) {
                Optional<Class<?>> componentType = determineComponentType(importContext);
                if (!componentType.isPresent()) {
                    return Optional.empty();
                }

                return Optional.of(toArray(componentType.get(), buildValues(owner, importContext)));
            }

            @SuppressWarnings({"unchecked", "rawtypes"}) // NOTE: We assume the component type matches the list
            private Object toArray(Class<?> componentType, List<Object> values) {
                if (componentType == boolean.class) {
                    return Booleans.toArray((Collection) values);
                } else if (componentType == byte.class) {
                    return Bytes.toArray((Collection) values);
                } else if (componentType == short.class) {
                    return Shorts.toArray((Collection) values);
                } else if (componentType == int.class) {
                    return Ints.toArray((Collection) values);
                } else if (componentType == long.class) {
                    return Longs.toArray((Collection) values);
                } else if (componentType == float.class) {
                    return Floats.toArray((Collection) values);
                } else if (componentType == double.class) {
                    return Doubles.toArray((Collection) values);
                } else if (componentType == char.class) {
                    return Chars.toArray((Collection) values);
                }
                return values.toArray((Object[]) Array.newInstance(componentType, values.size()));
            }

            private <T extends HasDescription> List<Object> buildValues(T owner, ImportedClasses importContext) {
                List<Object> result = new ArrayList<>();
                for (ValueBuilder value : values) {
                    result.addAll(value.build(owner, importContext).asSet());
                }
                return result;
            }

            private Optional<Class<?>> determineComponentType(ImportedClasses importContext) {
                if (derivedComponentType != null) {
                    return Optional.<Class<?>>of(derivedComponentType);
                }

                Optional<JavaClass> returnType = importContext.getMethodReturnType(
                        annotationArrayContext.getDeclaringAnnotationTypeName(),
                        annotationArrayContext.getDeclaringAnnotationMemberName());

                return returnType.isPresent() ?
                        determineComponentTypeFromReturnValue(returnType.get()) :
                        Optional.<Class<?>>empty();
            }

            private Optional<Class<?>> determineComponentTypeFromReturnValue(JavaClass returnType) {
                if (returnType.isEquivalentTo(Class[].class)) {
                    return Optional.<Class<?>>of(JavaClass.class);
                }

                return resolveComponentTypeFrom(returnType.getName());
            }

            @MayResolveTypesViaReflection(reason = "Resolving primitives does not really use reflection")
            private Optional<Class<?>> resolveComponentTypeFrom(String arrayTypeName) {
                JavaClassDescriptor descriptor = JavaClassDescriptor.From.name(arrayTypeName);
                JavaClassDescriptor componentType = getComponentType(descriptor);

                if (componentType.isPrimitive()) {
                    return Optional.<Class<?>>of(componentType.resolveClass());
                }
                if (String.class.getName().equals(componentType.getFullyQualifiedClassName())) {
                    return Optional.<Class<?>>of(String.class);
                }

                // if we couldn't determine the type up to now, it must be an empty enum or annotation array,
                // it's not completely consistent, but we'll just treat this as Object array for now and see
                // if this will ever make a problem, since annotation proxy would to the conversion backwards
                // and if one has to handle get(property): Object, this to be an empty Object[]
                // instead of a JavaEnumConstant[] or JavaAnnotation<?>[] should hardly cause any real problems
                return Optional.<Class<?>>of(Object.class);
            }

            private JavaClassDescriptor getComponentType(JavaClassDescriptor descriptor) {
                Optional<JavaClassDescriptor> result = descriptor.tryGetComponentType();
                checkState(result.isPresent(), "Couldn't determine component type of array return type %s, " +
                        "this is most likely a bug", descriptor.getFullyQualifiedClassName());

                return result.get();
            }
        }
    }

    private interface AnnotationArrayContext {
        String getDeclaringAnnotationTypeName();

        String getDeclaringAnnotationMemberName();

        void setArrayResult(ValueBuilder valueBuilder);
    }

    private static ValueBuilder handleAnnotationClassProperty(JavaClassDescriptor clazz, DeclarationHandler declarationHandler) {
        declarationHandler.onDeclaredAnnotationValueType(clazz.getFullyQualifiedClassName());
        return ValueBuilder.fromClassProperty(clazz);
    }

    private static ValueBuilder handleAnnotationEnumProperty(String desc, String value, DeclarationHandler declarationHandler) {
        JavaClassDescriptor enumType = JavaClassDescriptorImporter.importAsmTypeFromDescriptor(desc);
        declarationHandler.onDeclaredAnnotationValueType(enumType.getFullyQualifiedClassName());
        return ValueBuilder.fromEnumProperty(enumType, value);
    }

    private static JavaAnnotationBuilder handleAnnotationAnnotationProperty(String desc, DeclarationHandler declarationHandler) {
        JavaAnnotationBuilder subAnnotationBuilder = new JavaAnnotationBuilder().withType(JavaClassDescriptorImporter.importAsmTypeFromDescriptor(desc));
        declarationHandler.onDeclaredAnnotationValueType(subAnnotationBuilder.getFullyQualifiedClassName());
        return subAnnotationBuilder;
    }

    private abstract static class ClassAndPrimitiveDistinguishingAnnotationVisitor extends AnnotationVisitor {
        ClassAndPrimitiveDistinguishingAnnotationVisitor() {
            super(ASM_API_VERSION);
        }

        @Override
        public final void visit(String name, Object input) {
            final Object value = JavaClassDescriptorImporter.importAsmTypeIfPossible(input);
            if (value instanceof JavaClassDescriptor) {
                visitClass(name, (JavaClassDescriptor) value);
            } else {
                visitPrimitive(name, value);
            }
        }

        abstract void visitClass(String name, JavaClassDescriptor clazz);

        abstract void visitPrimitive(String name, Object value);
    }
}
