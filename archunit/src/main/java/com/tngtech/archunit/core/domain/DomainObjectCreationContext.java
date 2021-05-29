/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.core.importer.DomainBuilders.ConstructorCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaEnumConstantBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaStaticInitializerBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.MethodCallTargetBuilder;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Together with {@link DomainBuilders}, this class is the link to create domain objects from the import
 * context. To make the API clear, we try to keep only those methods public, which are really meant to be used.
 * Constructors of domain objects however, are not to be used under any circumstances, only ArchUnit may construct
 * domain objects. To keep <code>..domain..</code> and <code>..importer..</code> in reasonably grouped packages, we unfortunately
 * need to have some public link, which is created by supplying {@link DomainBuilders}, which can only be
 * instantiated within package <code>..importer..</code> to {@link DomainObjectCreationContext}, which is the only place
 * to create domain objects.<br><br>
 * To make up for the public visibility, the JLS forces upon us,
 * {@link DomainObjectCreationContext} is declared {@link Internal @Internal}, to emphasize that it is not meant
 * for ArchUnit users, to be accessed in any way.
 */
@Internal
public class DomainObjectCreationContext {
    public static JavaClasses createJavaClasses(
            Map<String, JavaClass> selectedClasses, Collection<JavaClass> allClasses, ImportContext importContext) {

        return JavaClasses.of(selectedClasses, allClasses, importContext);
    }

    public static JavaClass createJavaClass(JavaClassBuilder builder) {
        return new JavaClass(builder);
    }

    public static void completeClassHierarchy(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeClassHierarchyFrom(importContext);
    }

    public static void completeEnclosingClass(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeEnclosingClassFrom(importContext);
    }

    public static void completeTypeParameters(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeTypeParametersFrom(importContext);
    }

    public static void completeGenericSuperclass(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeGenericSuperclassFrom(importContext);
    }

    public static void completeGenericInterfaces(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeGenericInterfacesFrom(importContext);
    }

    public static void completeMembers(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeMembers(importContext);
    }

    public static void completeAnnotations(JavaClass javaClass, ImportContext importContext) {
        javaClass.completeAnnotations(importContext);
    }

    public static <T extends HasDescription> JavaAnnotation<T> createJavaAnnotation(T owner, JavaAnnotationBuilder builder) {
        return new JavaAnnotation<>(owner, builder);
    }

    public static JavaClassList createJavaClassList(List<JavaClass> elements) {
        return new JavaClassList(elements);
    }

    public static JavaField createJavaField(JavaFieldBuilder builder) {
        return new JavaField(builder);
    }

    public static JavaFieldAccess createJavaFieldAccess(JavaFieldAccessBuilder builder) {
        return new JavaFieldAccess(builder);
    }

    public static AccessTarget.FieldAccessTarget createFieldAccessTarget(FieldAccessTargetBuilder builder) {
        return new AccessTarget.FieldAccessTarget(builder);
    }

    public static JavaConstructor createJavaConstructor(JavaConstructorBuilder builder) {
        return new JavaConstructor(builder);
    }

    public static JavaConstructorCall createJavaConstructorCall(JavaConstructorCallBuilder builder) {
        return new JavaConstructorCall(builder);
    }

    public static AccessTarget.ConstructorCallTarget createConstructorCallTarget(ConstructorCallTargetBuilder builder) {
        return new AccessTarget.ConstructorCallTarget(builder);
    }

    public static JavaMethod createJavaMethod(JavaMethodBuilder builder, Function<JavaMethod, Optional<Object>> createAnnotationDefaultValue) {
        return new JavaMethod(builder, createAnnotationDefaultValue);
    }

    public static JavaMethodCall createJavaMethodCall(JavaMethodCallBuilder builder) {
        return new JavaMethodCall(builder);
    }

    public static AccessTarget.MethodCallTarget createMethodCallTarget(MethodCallTargetBuilder builder) {
        return new AccessTarget.MethodCallTarget(builder);
    }

    public static JavaStaticInitializer createJavaStaticInitializer(JavaStaticInitializerBuilder builder) {
        return new JavaStaticInitializer(builder);
    }

    public static JavaEnumConstant createJavaEnumConstant(JavaEnumConstantBuilder builder) {
        return new JavaEnumConstant(builder);
    }

    public static Source createSource(URI uri, Optional<String> sourceFileName, boolean md5InClassSourcesEnabled) {
        return new Source(uri, sourceFileName, md5InClassSourcesEnabled);
    }

    public static ReferencedClassObject createReferencedClassObject(JavaCodeUnit codeUnit, JavaClass javaClass, int lineNumber) {
        return ReferencedClassObject.from(codeUnit, javaClass, lineNumber);
    }

    public static <CODE_UNIT extends JavaCodeUnit> ThrowsClause<CODE_UNIT> createThrowsClause(CODE_UNIT codeUnit, List<JavaClass> types) {
        return ThrowsClause.from(codeUnit, types);
    }

    public static InstanceofCheck createInstanceofCheck(JavaCodeUnit codeUnit, JavaClass target, int lineNumber) {
        return InstanceofCheck.from(codeUnit, target, lineNumber);
    }

    public static <OWNER extends HasDescription> JavaTypeVariable<OWNER> createTypeVariable(String name, OWNER owner, JavaClass erasure) {
        return new JavaTypeVariable<>(name, owner, erasure);
    }

    public static void completeTypeVariable(JavaTypeVariable<?> variable, List<JavaType> upperBounds) {
        variable.setUpperBounds(upperBounds);
    }

    public static JavaGenericArrayType createGenericArrayType(JavaType componentType, JavaClass erasure) {
        checkArgument(
                componentType instanceof JavaTypeVariable
                        || componentType instanceof JavaGenericArrayType
                        || componentType instanceof JavaParameterizedType,
                "Component type of a generic array type can only be a type variable, a generic array type or a parameterized type, but was %s. "
                        + "This is most likely a bug.", componentType);

        return new JavaGenericArrayType(componentType.getName() + "[]", componentType, erasure);
    }

    public static JavaWildcardType createWildcardType(JavaWildcardTypeBuilder<?> builder) {
        return new JavaWildcardType(builder);
    }
}
