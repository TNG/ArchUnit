package com.tngtech.archunit.core.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.TypeSwitchCheck;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.ChecksInstanceofInMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.V21;

class ClassFileImporterTypeSwitchTest {
    private static final String ORIGIN_NAME = "example.SwitchBootstrapOnly";
    private static final String TARGET_NAME = "example.SampleData";
    private static final Handle TYPE_SWITCH_BOOTSTRAP = new Handle(
            H_INVOKESTATIC,
            "java/lang/runtime/SwitchBootstraps",
            "typeSwitch",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false);

    @TempDir
    Path temporaryFolder;

    @Test
    void imports_outgoing_dependency_from_type_switch_label_without_a_cast() throws IOException {
        JavaClasses classes = importTypeSwitch();
        JavaClass origin = classes.get(ORIGIN_NAME);

        assertOnlyTypeSwitchCanReferenceTheTarget(origin);
        assertThat(origin.getDirectDependenciesFromSelf())
                .extracting(Dependency::getTargetClass)
                .contains(classes.get(TARGET_NAME));
    }

    @Test
    void imports_incoming_dependency_from_type_switch_label_without_a_cast() throws IOException {
        JavaClasses classes = importTypeSwitch();
        JavaClass origin = classes.get(ORIGIN_NAME);

        assertOnlyTypeSwitchCanReferenceTheTarget(origin);
        assertThat(classes.get(TARGET_NAME).getDirectDependenciesToSelf())
                .extracting(Dependency::getOriginClass)
                .contains(origin);
    }

    @Test
    void imports_all_class_labels_and_ignores_constant_value_labels() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, false,
                "constant", 23, Type.getObjectType("java/lang/String"), Type.getObjectType(TARGET_NAME.replace('.', '/')))
                .get(ORIGIN_NAME);
        JavaMethod method = origin.getMethod("classify", Object.class);
        Set<TypeSwitchCheck> checks = method.getTypeSwitchChecks();

        assertThat(checks).extracting(check -> check.getRawType().getName())
                .containsExactlyInAnyOrder(String.class.getName(), TARGET_NAME);
        assertThat(origin.getTypeSwitchChecks()).containsExactlyInAnyOrderElementsOf(checks);
        assertThat(checks).allSatisfy(check -> {
            assertThat(check.getOwner()).isSameAs(method);
            assertThat(check.getType()).isSameAs(check.getRawType());
            assertThat(check.isDeclaredInLambda()).isFalse();
        });
        assertThatThrownBy(checks::clear).isInstanceOf(UnsupportedOperationException.class);
        assertOnlyTypeSwitchCanReferenceTheTarget(origin);
        assertThat(origin.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getSourceCodeLocation().getLineNumber() == 42))
                .extracting(dependency -> dependency.getTargetClass().getName())
                .containsExactlyInAnyOrder(String.class.getName(), TARGET_NAME);
    }

    @Test
    void retains_array_and_self_labels_while_applying_normal_dependency_rules() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, false,
                Type.getType("[[Lexample/SampleData;"), Type.getObjectType("example/SwitchBootstrapOnly"),
                Type.getType("[Lexample/SwitchBootstrapOnly;"), Type.getType("[I")).get(ORIGIN_NAME);

        assertThat(origin.getTypeSwitchChecks()).extracting(check -> check.getRawType().getName())
                .containsExactlyInAnyOrder("[[Lexample.SampleData;", ORIGIN_NAME, "[Lexample.SwitchBootstrapOnly;", "[I");
        assertThat(origin.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getSourceCodeLocation().getLineNumber() == 42))
                .extracting(dependency -> dependency.getTargetClass().getName())
                .containsExactlyInAnyOrder("[[Lexample.SampleData;", "[Lexample.SampleData;", TARGET_NAME,
                        "[Lexample.SwitchBootstrapOnly;", "[I");
        assertOnlyTypeSwitchCanReferenceTheTarget(origin);
    }

    @Test
    void reports_the_instruction_location_and_a_distinct_dependency_description() throws IOException {
        JavaClass origin = importTypeSwitch().get(ORIGIN_NAME);
        JavaMethod method = origin.getMethod("classify", Object.class);
        TypeSwitchCheck check = origin.getTypeSwitchChecks().stream()
                .filter(candidate -> candidate.getRawType().getName().equals(TARGET_NAME)).findFirst().get();
        Dependency dependency = origin.getDirectDependenciesFromSelf().stream()
                .filter(candidate -> candidate.getTargetClass().getName().equals(TARGET_NAME)).findFirst().get();

        assertThat(method.getSourceCodeLocation().getLineNumber()).isEqualTo(7);
        assertThat(check.getLineNumber()).isEqualTo(42);
        assertThat(check.getSourceCodeLocation().getSourceClass()).isSameAs(origin);
        assertThat(dependency.getSourceCodeLocation()).isEqualTo(check.getSourceCodeLocation());
        assertThat(dependency.getDescription()).isEqualTo(
                "Method <example.SwitchBootstrapOnly.classify(java.lang.Object)> checks type in switch <example.SampleData> in (SwitchBootstrapOnly.java:42)");
        assertThat(dependency.convertTo(Dependency.class)).containsExactly(dependency);
        assertThat(dependency.convertTo(JavaAccess.class)).isEmpty();
        assertThat(dependency.convertTo(TypeSwitchCheck.class)).isEmpty();
    }

    @Test
    void reports_line_zero_when_debug_line_information_is_missing() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, null, false,
                Type.getObjectType(TARGET_NAME.replace('.', '/'))).get(ORIGIN_NAME);

        assertThat(origin.getTypeSwitchChecks()).singleElement().satisfies(check -> assertThat(check.getLineNumber()).isZero());
        assertThat(origin.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getTargetClass().getName().equals(TARGET_NAME)))
                .singleElement().satisfies(dependency -> assertThat(dependency.getSourceCodeLocation().getLineNumber()).isZero());
    }

    @Test
    void ignores_other_bootstrap_owners_and_method_names() throws IOException {
        for (Handle otherBootstrap : new Handle[]{
                new Handle(H_INVOKESTATIC, "example/OtherBootstrap", "typeSwitch", TYPE_SWITCH_BOOTSTRAP.getDesc(), false),
                new Handle(H_INVOKESTATIC, TYPE_SWITCH_BOOTSTRAP.getOwner(), "enumSwitch", TYPE_SWITCH_BOOTSTRAP.getDesc(), false)
        }) {
            JavaClass origin = importTypeSwitch(otherBootstrap, 42, false,
                    Type.getObjectType(TARGET_NAME.replace('.', '/'))).get(ORIGIN_NAME);

            assertThat(origin.getTypeSwitchChecks()).isEmpty();
            assertThat(origin.getDirectDependenciesFromSelf()).extracting(dependency -> dependency.getTargetClass().getName())
                    .doesNotContain(TARGET_NAME);
        }
    }

    @Test
    void does_not_treat_method_type_constants_as_class_labels() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, false,
                Type.getMethodType("(Lexample/MethodTypeOnly;)V"), Type.getObjectType(TARGET_NAME.replace('.', '/')))
                .get(ORIGIN_NAME);

        assertThat(origin.getTypeSwitchChecks()).extracting(check -> check.getRawType().getName()).containsExactly(TARGET_NAME);
    }

    @Test
    void resolves_class_labels_from_the_classpath() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, false,
                Type.getType(ChecksInstanceofInMethod.class)).get(ORIGIN_NAME);

        assertThat(origin.getTypeSwitchChecks()).singleElement().satisfies(check ->
                assertThat(check.getRawType().getMethod("method", Object.class).getRawReturnType().getName()).isEqualTo("boolean"));
    }

    @Test
    void associates_type_switch_labels_in_a_lambda_with_the_enclosing_method() throws IOException {
        JavaClass origin = importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, true,
                Type.getObjectType(TARGET_NAME.replace('.', '/'))).get(ORIGIN_NAME);
        JavaMethod method = origin.getMethod("classify");

        assertThat(method.getTypeSwitchChecks()).singleElement().satisfies(check -> {
            assertThat(check.getOwner()).isSameAs(method);
            assertThat(check.isDeclaredInLambda()).isTrue();
            assertThat(check.getLineNumber()).isEqualTo(42);
        });
        assertThat(origin.getTypeSwitchChecks()).containsExactlyInAnyOrderElementsOf(method.getTypeSwitchChecks());
        assertThat(origin.getDirectDependenciesFromSelf()).extracting(dependency -> dependency.getTargetClass().getName())
                .contains(TARGET_NAME);
        assertOnlyTypeSwitchCanReferenceTheTarget(origin);
    }

    private JavaClasses importTypeSwitch() throws IOException {
        return importTypeSwitch(TYPE_SWITCH_BOOTSTRAP, 42, false,
                Type.getObjectType("java/lang/String"), Type.getObjectType(TARGET_NAME.replace('.', '/')));
    }

    private JavaClasses importTypeSwitch(Handle bootstrap, Integer lineNumber, boolean inLambda, Object... labels) throws IOException {
        ClassWriter target = new ClassWriter(0);
        target.visit(V21, ACC_PUBLIC, TARGET_NAME.replace('.', '/'), null, "java/lang/Object", null);
        target.visitEnd();
        writeClass(TARGET_NAME, target.toByteArray());

        ClassWriter origin = new ClassWriter(0);
        origin.visit(V21, ACC_PUBLIC, ORIGIN_NAME.replace('.', '/'), null, "java/lang/Object", null);
        origin.visitSource("SwitchBootstrapOnly.java", null);
        if (inLambda) {
            MethodVisitor enclosing = origin.visitMethod(ACC_PUBLIC | ACC_STATIC, "classify", "()Ljava/util/function/ToIntFunction;", null, null);
            enclosing.visitCode();
            enclosing.visitInvokeDynamicInsn("applyAsInt", "()Ljava/util/function/ToIntFunction;", new Handle(
                            H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                            false),
                    Type.getMethodType("(Ljava/lang/Object;)I"),
                    new Handle(H_INVOKESTATIC, ORIGIN_NAME.replace('.', '/'), "lambda$classify$0", "(Ljava/lang/Object;)I", false),
                    Type.getMethodType("(Ljava/lang/Object;)I"));
            enclosing.visitInsn(ARETURN);
            enclosing.visitMaxs(1, 0);
            enclosing.visitEnd();
        }
        MethodVisitor method = origin.visitMethod(inLambda ? ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC : ACC_PUBLIC | ACC_STATIC,
                inLambda ? "lambda$classify$0" : "classify", "(Ljava/lang/Object;)I", null, null);
        method.visitCode();
        Label start = new Label();
        method.visitLabel(start);
        if (lineNumber != null) {
            method.visitLineNumber(7, start);
        }
        method.visitVarInsn(ALOAD, 0);
        Label instruction = new Label();
        method.visitLabel(instruction);
        if (lineNumber != null) {
            method.visitLineNumber(lineNumber, instruction);
        }
        method.visitInsn(ICONST_0);
        method.visitInvokeDynamicInsn("typeSwitch", "(Ljava/lang/Object;I)I", bootstrap, labels);
        method.visitInsn(IRETURN);
        method.visitMaxs(2, 1);
        method.visitEnd();
        origin.visitEnd();
        writeClass(ORIGIN_NAME, origin.toByteArray());

        return new ClassFileImporter().importPath(temporaryFolder);
    }

    private void writeClass(String name, byte[] bytecode) throws IOException {
        Path classFile = temporaryFolder.resolve(name.replace('.', '/') + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, bytecode);
    }

    private static void assertOnlyTypeSwitchCanReferenceTheTarget(JavaClass origin) {
        assertThat(origin.getAccessesFromSelf()).isEmpty();
        assertThat(origin.getReferencedClassObjects()).isEmpty();
        assertThat(origin.getInstanceofChecks()).isEmpty();
    }
}
