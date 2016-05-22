package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Lists;
import com.tngtech.archunit.core.ClassFileImportContext.BaseRawAccessRecord;
import com.tngtech.archunit.core.ClassFileImportContext.ConstructorTargetInfo;
import com.tngtech.archunit.core.ClassFileImportContext.FieldTargetInfo;
import com.tngtech.archunit.core.ClassFileImportContext.MethodTargetInfo;
import com.tngtech.archunit.core.ClassFileImportContext.RawConstructorCallRecord;
import com.tngtech.archunit.core.ClassFileImportContext.RawFieldAccessRecord;
import com.tngtech.archunit.core.ClassFileImportContext.RawMethodCallRecord;
import com.tngtech.archunit.core.ClassFileImportContext.TargetInfo;
import com.tngtech.archunit.core.JavaClass.TypeAnalysisListener;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.ClassFileProcessor.MethodIdentifier.staticInitializerIdentifier;
import static com.tngtech.archunit.core.ClassFileProcessor.MethodLike.staticInitializerOf;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.reflections.ReflectionUtils.getAllSuperTypes;

class ClassFileProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileProcessor.class);

    private final ClassFileImportContext context = new ClassFileImportContext();

    private final ProcessingContext processingContext = new ProcessingContext();

    ClassFileProcessor() {
        super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.info("Analysing class '{}'", name);
        processingContext.init(name);

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!processingContext.canImportCurrentClass) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        LOG.debug("Analysing method {}.{}:{}", processingContext.getCurrentClassName(), name, desc);
        MethodVisitor delegate = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodProcessor(processingContext.getMethod(name, desc), context, delegate);
    }

    @Override
    public void visitEnd() {
        if (processingContext.canImportCurrentClass) {
            LOG.debug("Done analysing {}", processingContext.getCurrentClassName());
            context.add(processingContext.finish());
        }
        super.visitEnd();
    }

    public JavaClasses process(ClassFileSource source) {
        ClassFileProcessor child = new ClassFileProcessor();
        for (InputStream stream : source) {
            try (InputStream s = stream) {
                new ClassReader(s).accept(child, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return child.context.complete();
    }

    private static class ProcessingContext {
        private Class<?> currentClass;
        private MethodRecorder methodRecorder;
        private JavaClass.Builder currentClassBuilder;

        private boolean canImportCurrentClass;

        void init(String classDescriptor) {
            canImportCurrentClass = true;
            try {
                tryInit(classDescriptor);
            } catch (NoClassDefFoundError e) {
                LOG.warn("Can't analyse class '{}' because of missing dependency '{}'",
                        classDescriptor, e.getMessage());
                canImportCurrentClass = false;
            } catch (ReflectionsException e) {
                LOG.warn("Can't analyse class '{}' because of missing dependency. Error was: '{}'",
                        classDescriptor, e.getMessage());
                canImportCurrentClass = false;
            }
        }

        private void tryInit(String classDescriptor) {
            currentClass = classForDescriptor(classDescriptor);
            methodRecorder = new MethodRecorder(currentClass);
            currentClassBuilder = new JavaClass.Builder(methodRecorder).withType(currentClass);
        }

        private Class<?> classForDescriptor(String descriptor) {
            return JavaType.fromDescriptor(descriptor).asClass();
        }

        String getCurrentClassName() {
            return currentClass.getName();
        }

        MethodLike getMethod(String name, String desc) {
            return methodRecorder.get(MethodIdentifier.of(currentClass, name, desc));
        }

        public JavaClass finish() {
            JavaClass javaClass = currentClassBuilder.build();
            currentClass = null;
            methodRecorder = null;
            currentClassBuilder = null;
            return javaClass;
        }
    }

    private static class MethodProcessor extends MethodVisitor {
        private final MethodLike currentMethod;
        private final ClassFileImportContext context;

        private Set<RawFieldAccessRecord> fieldAccessRecordBuilders = new HashSet<>();
        private Set<RawMethodCallRecord> methodCallRecordBuilders = new HashSet<>();
        private Set<RawConstructorCallRecord> constructorCallRecordBuilders = new HashSet<>();
        private int actualLineNumber;

        MethodProcessor(MethodLike currentMethod, ClassFileImportContext context, MethodVisitor mv) {
            super(ASM5, mv);
            this.currentMethod = currentMethod;
            this.context = context;
        }

        @Override
        public void visitCode() {
            actualLineNumber = 0;
            super.visitCode();
        }

        // NOTE: ASM doesn't reliably visit this method, so if this method is skipped, line number 0 is recorded
        @Override
        public void visitLineNumber(int line, Label start) {
            LOG.debug("Examining line number {}", line);
            actualLineNumber = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            AccessType accessType = AccessType.forOpCode(opcode);
            LOG.debug("Found {} access to field {}.{}:{} in line {}", accessType, owner, name, desc, actualLineNumber);
            TargetInfo target = new FieldTargetInfo(owner, name, desc);
            fieldAccessRecordBuilders.add(filled(new RawFieldAccessRecord.Builder(), target)
                    .withAccessType(accessType)
                    .build());

            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            LOG.debug("Found call of method {}.{}:{} in line {}", owner, name, desc, actualLineNumber);
            if (CONSTRUCTOR_NAME.equals(name)) {
                TargetInfo target = new ConstructorTargetInfo(owner, name, desc);
                constructorCallRecordBuilders.add(filled(new RawConstructorCallRecord.Builder(), target).build());
            } else {
                TargetInfo target = new MethodTargetInfo(owner, name, desc);
                methodCallRecordBuilders.add(filled(new RawMethodCallRecord.Builder(), target).build());
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        private <BUILDER extends BaseRawAccessRecord.Builder<BUILDER>> BUILDER filled(BUILDER builder, TargetInfo target) {
            return builder
                    .withCaller(currentMethod)
                    .withTarget(target)
                    .withLineNumber(actualLineNumber);
        }

        @Override
        public void visitEnd() {
            for (RawFieldAccessRecord fieldAccessRecord : fieldAccessRecordBuilders) {
                context.registerFieldAccess(fieldAccessRecord);
            }
            fieldAccessRecordBuilders = new HashSet<>();
            for (RawMethodCallRecord methodCallRecord : methodCallRecordBuilders) {
                context.registerMethodCall(methodCallRecord);
            }
            methodCallRecordBuilders = new HashSet<>();
            for (RawConstructorCallRecord constructorCallRecord : constructorCallRecordBuilders) {
                context.registerConstructorCall(constructorCallRecord);
            }
            constructorCallRecordBuilders = new HashSet<>();

            super.visitEnd();
        }
    }

    private static class MethodRecorder extends ForwardingMap<MethodIdentifier, MethodLike>
            implements TypeAnalysisListener {

        private final Map<MethodIdentifier, MethodLike> typeToMethod = new HashMap<>();

        @SuppressWarnings("unchecked")
        public MethodRecorder(Class<?> type) {
            for (Class<?> clazzInHierarchy : getAllSuperTypes(type)) {
                put(staticInitializerIdentifier(clazzInHierarchy), staticInitializerOf(clazzInHierarchy));
            }
        }

        @Override
        public MethodLike get(Object key) {
            return checkNotNull(super.get(key),
                    "No Method for %s was encountered, you probably have ambiguous classes on your classpath", key);
        }

        @Override
        public void onMethodFound(Method method) {
            typeToMethod.put(MethodIdentifier.of(method), MethodLike.of(method));
        }

        @Override
        public void onConstructorFound(Constructor<?> constructor) {
            typeToMethod.put(MethodIdentifier.of(constructor), MethodLike.of(constructor));
        }

        @Override
        protected Map<MethodIdentifier, MethodLike> delegate() {
            return typeToMethod;
        }
    }

    static class MethodIdentifier {
        private final Class<?> declaringClass;
        private final String name;
        private final Type type;

        public MethodIdentifier(Class<?> declaringClass, String name, Type type) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaringClass, name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodIdentifier other = (MethodIdentifier) obj;
            return Objects.equals(this.declaringClass, other.declaringClass) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.type, other.type);
        }

        @Override
        public String toString() {
            return "{declaringClass=" + declaringClass + ", name='" + name + "', type=" + type + '}';
        }

        static MethodIdentifier of(Method method) {
            return new MethodIdentifier(method.getDeclaringClass(), method.getName(), Type.getType(method));
        }

        public static MethodIdentifier of(Constructor<?> constructor) {
            return new MethodIdentifier(constructor.getDeclaringClass(), CONSTRUCTOR_NAME, Type.getType(constructor));
        }

        public static MethodIdentifier of(Class<?> declaringClass, String name, String desc) {
            return new MethodIdentifier(declaringClass, name, Type.getMethodType(desc));
        }

        public static MethodIdentifier staticInitializerIdentifier(Class<?> declaringClass) {
            return new MethodIdentifier(declaringClass, STATIC_INITIALIZER_NAME, Type.getMethodType("()V"));
        }
    }

    static class MethodLike {
        private final Member member;
        private final String name;
        private final List<Class<?>> parameters;
        private final Class<?> declaringClass;
        private final int hashCode;

        private MethodLike(Member member) {
            this(member,
                    nameOf(member),
                    parametersOf(member),
                    member.getDeclaringClass(),
                    Objects.hash(member));
        }

        private MethodLike(Member object, String name, List<Class<?>> parameters, Class<?> declaringClass, int hashCode) {
            this.member = object;
            this.name = name;
            this.parameters = parameters;
            this.declaringClass = declaringClass;
            this.hashCode = hashCode;
        }

        private static ArrayList<Class<?>> parametersOf(Member member) {
            return member instanceof Constructor ?
                    Lists.newArrayList(((Constructor<?>) member).getParameterTypes()) :
                    Lists.newArrayList(((Method) member).getParameterTypes());
        }

        private static String nameOf(Member member) {
            return member instanceof Constructor ?
                    CONSTRUCTOR_NAME :
                    member.getName();
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        public List<Class<?>> getParameters() {
            return parameters;
        }

        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodLike other = (MethodLike) obj;
            return Objects.equals(this.member, other.member);
        }

        @Override
        public String toString() {
            return "MethodLike{member=" + member.getName() + '}';
        }

        static MethodLike of(Object o) {
            checkArgument(o instanceof Constructor || o instanceof Method);
            return new MethodLike((Member) o);
        }

        static MethodLike staticInitializerOf(final Class<?> clazz) {
            return new StaticInitializer(clazz);
        }

        public boolean is(JavaMethodLike<?, ?> method) {
            return getName().equals(method.getName())
                    && getParameters().equals(method.getParameters())
                    && getDeclaringClass() == method.getOwner().reflect();
        }

        private static class StaticInitializer extends MethodLike {
            private StaticInitializer(Class<?> clazz) {
                super(null, STATIC_INITIALIZER_NAME, Collections.<Class<?>>emptyList(), clazz, Objects.hash(STATIC_INITIALIZER_NAME, clazz));
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                if (!super.equals(obj)) {
                    return false;
                }
                final MethodLike.StaticInitializer other = (MethodLike.StaticInitializer) obj;
                return Objects.equals(getName(), other.getName()) &&
                        Objects.equals(getDeclaringClass(), other.getDeclaringClass()) &&
                        Objects.equals(getParameters(), other.getParameters());
            }

            @Override
            public String toString() {
                return String.format("%s{owner=%s, name=%s}", getClass().getSimpleName(), getDeclaringClass().getName(), getName());
            }
        }
    }
}
