package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.importer.testexamples.OtherClass;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassAccessingOneDimensionalArray;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassAccessingTwoDimensionalArray;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassUsedInArray;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.BaseClass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.CollectionInterface;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.GrandParentInterface;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.OtherInterface;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.OtherSubclass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.ParentInterface;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.SomeCollection;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.SubSubSubSubclass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.SubSubSubclass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.SubSubclass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.Subclass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.Subinterface;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.YetAnotherInterface;
import com.tngtech.archunit.core.importer.testexamples.innerclassimport.CalledClass;
import com.tngtech.archunit.core.importer.testexamples.innerclassimport.ClassWithInnerClass;
import com.tngtech.archunit.core.importer.testexamples.nestedimport.ClassWithNestedClass;
import com.tngtech.archunit.core.importer.testexamples.pathone.Class11;
import com.tngtech.archunit.core.importer.testexamples.pathone.Class12;
import com.tngtech.archunit.core.importer.testexamples.pathtwo.Class21;
import com.tngtech.archunit.core.importer.testexamples.pathtwo.Class22;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.AnnotationParameter;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.AnnotationToImport;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.ClassToImportOne;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.ClassToImportTwo;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.EnumToImport;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.InterfaceToImport;
import com.tngtech.archunit.core.importer.testexamples.simplenames.SimpleNameExamples;
import com.tngtech.archunit.core.importer.testexamples.syntheticimport.ClassWithSynthetics;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.archunit.testutil.LogTestRule;
import com.tngtech.archunit.testutil.OutsideOfClassPathRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.logging.log4j.Level;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.BRIDGE;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.SourceTest.bytesAt;
import static com.tngtech.archunit.core.domain.SourceTest.urlOf;
import static com.tngtech.archunit.core.domain.TestUtils.MD5_SUM_DISABLED;
import static com.tngtech.archunit.core.domain.TestUtils.md5sumOf;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.findAnyByName;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.jarFileOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatCall;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.constructor;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;
import static com.tngtech.archunit.testutil.TestUtils.namesOf;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assume.assumeTrue;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterTest {
    @Rule
    public final OutsideOfClassPathRule outsideOfClassPath = new OutsideOfClassPathRule();
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final LogTestRule logTest = new LogTestRule();
    @Rule
    public final IndependentClasspathRule independentClasspathRule = new IndependentClasspathRule();
    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();

    @Test
    public void imports_simple_package() {
        Set<String> expectedClassNames = Sets.newHashSet(
                ClassToImportOne.class.getName(),
                ClassToImportTwo.class.getName(),
                InterfaceToImport.class.getName(),
                EnumToImport.class.getName(),
                AnnotationToImport.class.getName(),
                AnnotationParameter.class.getName());

        Iterable<JavaClass> classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport"));

        assertThat(namesOf(classes)).containsOnlyElementsOf(expectedClassNames);
    }

    @Test
    public void imports_simple_class_details() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport"));

        assertThat(classes.get(ClassToImportOne.class))
                .isFullyImported(true)
                .matches(ClassToImportOne.class)
                .hasRawSuperclassMatching(Object.class)
                .hasNoInterfaces()
                .isInterface(false)
                .isEnum(false)
                .isAnnotation(false)
                .isRecord(false)
                .hasNoEnclosingClass()
                .isTopLevelClass(true)
                .isNestedClass(false)
                .isMemberClass(false)
                .isInnerClass(false)
                .isLocalClass(false)
                .isAnonymousClass(false);
        assertThat(classes.get(ClassToImportTwo.class))
                .hasOnlyModifiers(JavaModifier.PUBLIC, JavaModifier.FINAL);
    }

    @Test
    public void imports_simple_enum() {
        JavaClass javaClass = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport")).get(EnumToImport.class);

        assertThat(javaClass)
                .matches(EnumToImport.class)
                .hasRawSuperclassMatching(Enum.class)
                .hasNoInterfaces()
                .hasAllInterfacesMatchingInAnyOrder(Enum.class.getInterfaces())
                .isInterface(false)
                .isEnum(true)
                .isAnnotation(false)
                .isRecord(false);

        JavaEnumConstant constant = javaClass.getEnumConstant(EnumToImport.FIRST.name());
        assertThatType(constant.getDeclaringClass()).as("declaring class").isEqualTo(javaClass);
        assertThat(constant.name()).isEqualTo(EnumToImport.FIRST.name());
        assertThat(javaClass.getEnumConstants()).extractingResultOf("name").as("enum constant names")
                .containsOnly(EnumToImport.FIRST.name(), EnumToImport.SECOND.name());
    }

    @DataProvider
    public static Object[][] nested_static_classes() {
        return testForEach(ClassWithInnerClass.NestedStatic.class, ClassWithInnerClass.ImplicitlyNestedStatic.class);
    }

    @Test
    @UseDataProvider("nested_static_classes")
    public void imports_simple_static_nested_class(Class<?> nestedStaticClass) {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));

        assertThat(classes.get(nestedStaticClass))
                .matches(nestedStaticClass)
                .isTopLevelClass(false)
                .isNestedClass(true)
                .isMemberClass(true)
                .isInnerClass(false)
                .isLocalClass(false)
                .isAnonymousClass(false)
                .isRecord(false);
    }

    @Test
    public void imports_simple_inner_class() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));

        assertThat(classes.get(ClassWithInnerClass.Inner.class))
                .matches(ClassWithInnerClass.Inner.class)
                .isTopLevelClass(false)
                .isNestedClass(true)
                .isMemberClass(true)
                .isInnerClass(true)
                .isLocalClass(false)
                .isAnonymousClass(false)
                .isRecord(false);
    }

    @Test
    public void imports_simple_anonymous_class() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));
        JavaClass anonymousClass = classes.get(ClassWithInnerClass.class.getName() + "$1");

        assertThat(anonymousClass)
                .matches(Class.forName(anonymousClass.getName()))
                .isTopLevelClass(false)
                .isNestedClass(true)
                .isMemberClass(false)
                .isInnerClass(true)
                .isLocalClass(false)
                .isAnonymousClass(true)
                .isRecord(false);
    }

    @Test
    public void imports_simple_local_class() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));
        JavaClass localClass = classes.get(ClassWithInnerClass.class.getName() + "$1LocalCaller");

        assertThat(localClass)
                .matches(Class.forName(localClass.getName()))
                .isTopLevelClass(false)
                .isNestedClass(true)
                .isMemberClass(false)
                .isInnerClass(true)
                .isLocalClass(true)
                .isAnonymousClass(false)
                .isRecord(false);
    }

    @Test
    public void imports_simple_class_names_of_generated_types_correctly() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simplenames"));

        assertSameSimpleNameOfArchUnitAndReflection(classes, SimpleNameExamples.class);
        assertSameSimpleNameOfArchUnitAndReflection(classes, SimpleNameExamples.Crazy$InnerClass$$LikeAByteCodeGenerator_might_create.class);
        assertSameSimpleNameOfArchUnitAndReflection(classes, SimpleNameExamples.class.getName() + "$1");
        assertSameSimpleNameOfArchUnitAndReflection(classes, SimpleNameExamples.class.getName() + "$1Crazy$LocalClass");
        assertSameSimpleNameOfArchUnitAndReflection(classes,
                SimpleNameExamples.Crazy$InnerClass$$LikeAByteCodeGenerator_might_create.NestedInnerClass$Also$$_crazy.class);
        assertSameSimpleNameOfArchUnitAndReflection(classes,
                SimpleNameExamples.Crazy$InnerClass$$LikeAByteCodeGenerator_might_create.class.getName() + "$1");
        assertSameSimpleNameOfArchUnitAndReflection(classes,
                SimpleNameExamples.Crazy$InnerClass$$LikeAByteCodeGenerator_might_create.class.getName() + "$1Crazy$$NestedLocalClass");
    }

    @Test
    public void imports_interfaces() {
        JavaClass simpleInterface = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport")).get(InterfaceToImport.class);

        assertThat(simpleInterface)
                .matches(InterfaceToImport.class)
                .hasNoSuperclass()
                .hasNoInterfaces()
                .isInterface(true)
                .isEnum(false)
                .isRecord(false);
    }

    @Test
    public void imports_nested_classes() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/nestedimport"));

        assertThatTypes(classes).matchInAnyOrder(
                ClassWithNestedClass.class,
                ClassWithNestedClass.NestedClass.class,
                ClassWithNestedClass.StaticNestedClass.class,
                ClassWithNestedClass.NestedInterface.class,
                ClassWithNestedClass.StaticNestedInterface.class,
                Class.forName(ClassWithNestedClass.class.getName() + "$PrivateNestedClass"));
    }

    @Test
    public void handles_static_modifier_of_nested_classes() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/nestedimport"));

        assertThat(classes.get(ClassWithNestedClass.class).getModifiers()).as("modifiers of ClassWithNestedClass").doesNotContain(STATIC);
        assertThat(classes.get(ClassWithNestedClass.NestedClass.class).getModifiers()).as("modifiers of ClassWithNestedClass.NestedClass").doesNotContain(STATIC);
        assertThat(classes.get(ClassWithNestedClass.StaticNestedClass.class).getModifiers()).as("modifiers of ClassWithNestedClass.StaticNestedClass").contains(STATIC);
        assertThat(classes.get(ClassWithNestedClass.NestedInterface.class).getModifiers()).as("modifiers of ClassWithNestedClass.NestedInterface").contains(STATIC);
        assertThat(classes.get(ClassWithNestedClass.StaticNestedInterface.class).getModifiers()).as("modifiers of ClassWithNestedClass.StaticNestedInterface").contains(STATIC);
    }

    @Test
    public void handles_synthetic_modifiers() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/syntheticimport"));

        JavaField syntheticField = getOnlyElement(classes.get(ClassWithSynthetics.ClassWithSyntheticField.class).getFields());
        assertThat(syntheticField.getModifiers()).as("modifiers of field in ClassWithSynthetics.ClassWithSyntheticField").contains(SYNTHETIC);

        JavaMethod syntheticMethod = getOnlyElement(classes.get(ClassWithSynthetics.ClassWithSyntheticMethod.class).getMethods());
        assertThat(syntheticMethod.getModifiers()).as("modifiers of method in ClassWithSynthetics.ClassWithSyntheticMethod").contains(SYNTHETIC);

        JavaMethod compareMethod = classes.get(ClassWithSynthetics.class).getMethod("compare", Object.class, Object.class);
        assertThat(compareMethod.getModifiers()).as("modifiers of bridge method in ClassWithSynthetics").contains(BRIDGE, SYNTHETIC);
    }

    @Test
    public void imports_jdk_classes() {
        JavaClasses classes = new ClassFileImporter().importClasses(File.class);

        assertThatTypes(classes).matchExactly(File.class);
    }

    @Test
    public void imports_jdk_packages() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(File.class);

        assertThatTypes(classes).contain(File.class);
    }

    @Test
    public void creates_JavaPackages_for_each_JavaClass() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(getClass());

        JavaPackage javaPackage = classes.get(SomeClass.class).getPackage();

        assertThat(javaPackage.containsClass(SomeEnum.class)).as("Package contains " + SomeEnum.class).isTrue();
        assertThatTypes(javaPackage.getParent().get().getClasses()).contain(getClass());
    }

    @DataProvider
    public static Object[][] array_types() {
        return testForEach(ClassAccessingOneDimensionalArray.class, ClassAccessingTwoDimensionalArray.class);
    }

    // we want to diverge from the Reflection API in this place, because it is way more useful for dependency checks,
    // if com.some.SomeArray[].getPackageName() reports 'com.some' instead of '' (which would be ArchUnit's equivalent of null)
    @Test
    @UseDataProvider("array_types")
    public void adds_package_of_component_type_to_arrays(Class<?> classAccessingArray) {
        JavaClass javaClass = new ClassFileImporter().importPackagesOf(classAccessingArray)
                .get(classAccessingArray);

        JavaClass arrayType = getOnlyElement(javaClass.getFieldAccessesFromSelf()).getTarget().getRawType();

        assertThat(arrayType.getPackageName()).isEqualTo(ClassUsedInArray.class.getPackage().getName());
        assertThat(arrayType.getPackage().getName()).isEqualTo(ClassUsedInArray.class.getPackage().getName());
    }

    @Test
    public void imports_interfaces_and_classes() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass parentInterface = classes.get(ParentInterface.class);

        assertThat(baseClass).isInterface(false);
        assertThat(parentInterface).isInterface(true);
    }

    @Test
    public void imports_base_class_in_class_hierarchy_correctly() {
        JavaClass baseClass = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport")).get(BaseClass.class);

        assertThat(baseClass.getConstructors()).as("Constructors of " + BaseClass.class.getSimpleName()).hasSize(2);
        assertThat(baseClass.getFields()).as("Fields of " + BaseClass.class.getSimpleName()).hasSize(1);
        assertThat(baseClass.getMethods()).as("Methods of " + BaseClass.class.getSimpleName()).hasSize(2);
        assertThat(baseClass.getStaticInitializer().get().getMethodCallsFromSelf())
                .as("Calls from %s.<clinit>()", BaseClass.class.getSimpleName()).isNotEmpty();
    }

    @Test
    public void imports_subclass_in_class_hierarchy_correctly() {
        JavaClass subclass = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport")).get(Subclass.class);

        assertThat(subclass.getConstructors()).hasSize(3);
        assertThat(subclass.getFields()).hasSize(1);
        assertThat(subclass.getMethods()).hasSize(3);
        assertThat(subclass.getStaticInitializer().get().getMethodCallsFromSelf()).isNotEmpty();
    }

    @Test
    public void creates_relations_between_super_and_subclasses() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass subclass = classes.get(Subclass.class);
        JavaClass otherSubclass = classes.get(OtherSubclass.class);
        JavaClass subSubclass = classes.get(SubSubclass.class);
        JavaClass subSubSubclass = classes.get(SubSubSubclass.class);
        JavaClass subSubSubSubclass = classes.get(SubSubSubSubclass.class);

        assertThat(baseClass.getRawSuperclass().get().reflect()).isEqualTo(Object.class);
        assertThat(baseClass.getSubclasses()).containsOnly(subclass, otherSubclass);
        assertThat(baseClass.getSubclasses()).containsOnly(subclass, otherSubclass);
        assertThat(baseClass.getAllSubclasses()).containsOnly(subclass, otherSubclass, subSubclass, subSubSubclass, subSubSubSubclass);
        assertThat(baseClass.getAllSubclasses()).containsOnly(subclass, otherSubclass, subSubclass, subSubSubclass, subSubSubSubclass);
        assertThat(subclass.getRawSuperclass()).contains(baseClass);
        assertThat(subclass.getRawSuperclass()).contains(baseClass);
        assertThat(subclass.getAllSubclasses()).containsOnly(subSubclass, subSubSubclass, subSubSubSubclass);
        assertThat(subSubclass.getRawSuperclass()).contains(subclass);
    }

    @Test
    public void creates_relations_between_classes_and_interfaces() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass otherInterface = classes.get(OtherInterface.class);
        JavaClass subclass = classes.get(Subclass.class);
        JavaClass subinterface = classes.get(Subinterface.class);
        JavaClass otherSubclass = classes.get(OtherSubclass.class);
        JavaClass parentInterface = classes.get(ParentInterface.class);
        JavaClass grandParentInterface = classes.get(GrandParentInterface.class);
        JavaClass someCollection = classes.get(SomeCollection.class);
        JavaClass collectionInterface = classes.get(CollectionInterface.class);

        assertThat(baseClass.getRawInterfaces()).containsOnly(otherInterface);
        assertThat(baseClass.getAllRawInterfaces()).containsOnly(otherInterface, grandParentInterface);
        assertThat(subclass.getRawInterfaces()).containsOnly(subinterface);
        assertThat(subclass.getAllRawInterfaces()).containsOnly(
                subinterface, otherInterface, parentInterface, grandParentInterface);
        assertThat(otherSubclass.getRawInterfaces()).containsOnly(parentInterface);
        assertThat(otherSubclass.getAllRawInterfaces()).containsOnly(parentInterface, grandParentInterface, otherInterface);
        assertThat(someCollection.getRawInterfaces()).containsOnly(collectionInterface, otherInterface, subinterface);
        assertThat(someCollection.getAllRawInterfaces()).extractingResultOf("reflect").containsOnly(
                CollectionInterface.class, OtherInterface.class, Subinterface.class, ParentInterface.class,
                GrandParentInterface.class, Collection.class, Iterable.class);
    }

    @Test
    public void creates_relations_between_interfaces_and_interfaces() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass subinterface = classes.get(Subinterface.class);
        JavaClass parentInterface = classes.get(ParentInterface.class);
        JavaClass grandParentInterface = classes.get(GrandParentInterface.class);
        JavaClass collectionInterface = classes.get(CollectionInterface.class);

        assertThat(grandParentInterface.getAllRawInterfaces()).isEmpty();
        assertThat(parentInterface.getRawInterfaces()).containsOnly(grandParentInterface);
        assertThat(parentInterface.getAllRawInterfaces()).containsOnly(grandParentInterface);
        assertThat(subinterface.getRawInterfaces()).containsOnly(parentInterface);
        assertThat(subinterface.getAllRawInterfaces()).containsOnly(parentInterface, grandParentInterface);
        assertThat(collectionInterface.getRawInterfaces()).extractingResultOf("reflect").containsOnly(Collection.class);
    }

    @Test
    public void creates_relations_between_interfaces_and_subclasses() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass otherInterface = classes.get(OtherInterface.class);
        JavaClass subclass = classes.get(Subclass.class);
        JavaClass subSubclass = classes.get(SubSubclass.class);
        JavaClass subSubSubclass = classes.get(SubSubSubclass.class);
        JavaClass subSubSubSubclass = classes.get(SubSubSubSubclass.class);
        JavaClass subinterface = classes.get(Subinterface.class);
        JavaClass otherSubclass = classes.get(OtherSubclass.class);
        JavaClass parentInterface = classes.get(ParentInterface.class);
        JavaClass grandParentInterface = classes.get(GrandParentInterface.class);
        JavaClass someCollection = classes.get(SomeCollection.class);
        JavaClass collectionInterface = classes.get(CollectionInterface.class);

        assertThat(grandParentInterface.getSubclasses()).containsOnly(parentInterface, otherInterface);
        assertThat(grandParentInterface.getAllSubclasses()).containsOnly(
                parentInterface, subinterface, otherInterface,
                baseClass, subclass, otherSubclass, subSubclass, subSubSubclass, subSubSubSubclass, someCollection
        );
        assertThat(parentInterface.getSubclasses()).containsOnly(subinterface, otherSubclass);
        assertThat(parentInterface.getAllSubclasses()).containsOnly(
                subinterface, subclass, subSubclass, subSubSubclass, subSubSubSubclass, someCollection, otherSubclass);
        JavaClass collection = getOnlyElement(collectionInterface.getRawInterfaces());
        assertThat(collection.getAllSubclasses()).containsOnly(collectionInterface, someCollection);
    }

    @Test
    public void creates_superclass_and_interface_relations_missing_from_context() {
        JavaClass javaClass = new ClassFileImporter().importClass(SubSubSubSubclass.class);

        assertThat(javaClass.getAllRawSuperclasses()).extracting("name")
                .containsExactly(
                        SubSubSubclass.class.getName(),
                        SubSubclass.class.getName(),
                        Subclass.class.getName(),
                        BaseClass.class.getName(),
                        Object.class.getName());

        assertThat(javaClass.getAllRawInterfaces()).extracting("name")
                .containsOnly(
                        Subinterface.class.getName(),
                        YetAnotherInterface.class.getName(),
                        ParentInterface.class.getName(),
                        GrandParentInterface.class.getName(),
                        OtherInterface.class.getName());
    }

    @Test
    public void imports_enclosing_classes() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));
        JavaClass classWithInnerClass = classes.get(ClassWithInnerClass.class);
        JavaClass innerClass = classes.get(ClassWithInnerClass.Inner.class);
        JavaClass anonymousClass = classes.get(ClassWithInnerClass.class.getName() + "$1");
        JavaClass localClass = classes.get(ClassWithInnerClass.class.getName() + "$1LocalCaller");
        JavaMethod calledTarget = getOnlyElement(classes.get(CalledClass.class).getMethods());

        assertThat(innerClass.getEnclosingClass()).contains(classWithInnerClass);
        assertThat(anonymousClass.getEnclosingClass()).contains(classWithInnerClass);
        assertThat(localClass.getEnclosingClass()).contains(classWithInnerClass);

        JavaMethodCall call = getOnlyElement(innerClass.getCodeUnitWithParameterTypes("call").getMethodCallsFromSelf());
        assertThatCall(call).isFrom("call").isTo(calledTarget).inLineNumber(31);

        call = getOnlyElement(anonymousClass.getCodeUnitWithParameterTypes("call").getMethodCallsFromSelf());
        assertThatCall(call).isFrom("call").isTo(calledTarget).inLineNumber(11);

        call = getOnlyElement(localClass.getCodeUnitWithParameterTypes("call").getMethodCallsFromSelf());
        assertThatCall(call).isFrom("call").isTo(calledTarget).inLineNumber(21);
    }

    @Test
    public void imports_enclosing_method_of_local_class() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class ClassCreatingLocalClassInMethod {
            void someMethod() {
                class SomeLocalClass {
                }
            }
        }
        String localClassName = ClassCreatingLocalClassInMethod.class.getName() + "$1SomeLocalClass";
        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassCreatingLocalClassInMethod.class, Class.forName(localClassName)
        );
        JavaClass enclosingClass = classes.get(ClassCreatingLocalClassInMethod.class);
        JavaClass localClass = classes.get(localClassName);

        assertThat(localClass.getEnclosingCodeUnit()).contains(enclosingClass.getMethod("someMethod"));
        assertThat(localClass.getEnclosingClass()).contains(enclosingClass);
    }

    @Test
    public void imports_enclosing_constructor_of_local_class() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class ClassCreatingLocalClassInConstructor {
            ClassCreatingLocalClassInConstructor() {
                class SomeLocalClass {
                }
            }
        }
        String localClassName = ClassCreatingLocalClassInConstructor.class.getName() + "$1SomeLocalClass";
        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassCreatingLocalClassInConstructor.class, Class.forName(localClassName)
        );
        JavaClass enclosingClass = classes.get(ClassCreatingLocalClassInConstructor.class);
        JavaClass localClass = classes.get(localClassName);

        assertThat(localClass.getEnclosingCodeUnit()).contains(enclosingClass.getConstructor(getClass()));
        assertThat(localClass.getEnclosingClass()).contains(enclosingClass);
    }

    @Test
    public void imports_enclosing_method_of_anonymous_class() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class ClassCreatingAnonymousClassInMethod {
            void someMethod() {
                new Serializable() {
                };
            }
        }
        String anonymousClassName = ClassCreatingAnonymousClassInMethod.class.getName() + "$1";
        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassCreatingAnonymousClassInMethod.class, Class.forName(anonymousClassName)
        );
        JavaClass enclosingClass = classes.get(ClassCreatingAnonymousClassInMethod.class);
        JavaClass anonymousClass = classes.get(anonymousClassName);

        assertThat(anonymousClass.getEnclosingCodeUnit()).contains(enclosingClass.getMethod("someMethod"));
        assertThat(anonymousClass.getEnclosingClass()).contains(enclosingClass);
    }

    @Test
    public void imports_enclosing_constructor_of_anonymous_class() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class ClassCreatingAnonymousClassInConstructor {
            ClassCreatingAnonymousClassInConstructor() {
                new Serializable() {
                };
            }
        }
        String anonymousClassName = ClassCreatingAnonymousClassInConstructor.class.getName() + "$1";
        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassCreatingAnonymousClassInConstructor.class, Class.forName(anonymousClassName)
        );
        JavaClass enclosingClass = classes.get(ClassCreatingAnonymousClassInConstructor.class);
        JavaClass anonymousClass = classes.get(anonymousClassName);

        assertThat(anonymousClass.getEnclosingCodeUnit()).contains(enclosingClass.getConstructor(getClass()));
        assertThat(anonymousClass.getEnclosingClass()).contains(enclosingClass);
    }

    @Test
    public void imports_no_enclosing_code_unit_of_anonymous_class_defined_outside_of_method() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class ClassCreatingAnonymousClassInConstructor {
            final Serializable field = new Serializable() {
            };
        }
        String anonymousClassName = ClassCreatingAnonymousClassInConstructor.class.getName() + "$1";
        JavaClasses classes = new ClassFileImporter().importClasses(
                ClassCreatingAnonymousClassInConstructor.class, Class.forName(anonymousClassName)
        );
        JavaClass enclosingClass = classes.get(ClassCreatingAnonymousClassInConstructor.class);
        JavaClass anonymousClass = classes.get(anonymousClassName);

        assertThat(anonymousClass.getEnclosingClass()).contains(enclosingClass);
        assertThat(anonymousClass.getEnclosingCodeUnit()).isAbsent();
    }

    @Test
    public void reflect_works() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/innerclassimport"));

        JavaClass calledClass = classes.get(CalledClass.class);
        assertThat(calledClass.reflect()).isEqualTo(CalledClass.class);
        assertThat(calledClass.getField("someString").reflect()).isEqualTo(field(CalledClass.class, "someString"));
        assertThat(calledClass.getConstructor().reflect()).isEqualTo(constructor(CalledClass.class));
        assertThat(calledClass.getConstructor(String.class).reflect()).isEqualTo(constructor(CalledClass.class, String.class));
        assertThat(calledClass.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, String.class).reflect())
                .isEqualTo(constructor(CalledClass.class, String.class));

        JavaClass innerClass = classes.get(ClassWithInnerClass.Inner.class);
        assertThat(innerClass.reflect()).isEqualTo(ClassWithInnerClass.Inner.class);
        assertThat(innerClass.getMethod("call").reflect())
                .isEqualTo(method(ClassWithInnerClass.Inner.class, "call"));
    }

    @Test
    public void imports_urls_of_files() {
        Set<URL> urls = newHashSet(urlOf(ClassToImportOne.class), urlOf(ClassWithNestedClass.class));

        Set<JavaClass> classesFoundAtUrls = new HashSet<>();
        for (JavaClass javaClass : new ClassFileImporter().importUrls(urls)) {
            if (!Object.class.getName().equals(javaClass.getName())) {
                classesFoundAtUrls.add(javaClass);
            }
        }
        assertThat(classesFoundAtUrls).as("Number of classes at the given URLs").hasSize(2);
    }

    @Test
    public void imports_urls_of_folders() throws Exception {
        File testexamplesFolder = new File(new File(urlOf(getClass()).toURI()).getParentFile(), "testexamples");

        JavaClasses javaClasses = new ClassFileImporter().importUrl(testexamplesFolder.toURI().toURL());

        assertThatTypes(javaClasses).contain(SomeClass.class, OtherClass.class);
    }

    @Test
    public void imports_urls_of_jars() {
        Set<URL> urls = newHashSet(urlOf(Test.class), urlOf(RunWith.class));
        assumeTrue("We can't completely ensure that this will always be taken from a JAR file, though it's very likely",
                "jar".equals(urls.iterator().next().getProtocol()));

        JavaClasses classes = new ClassFileImporter().importUrls(urls)
                .that(DescribedPredicate.not(type(Annotation.class))); // NOTE @Test and @RunWith implement Annotation.class

        assertThat(classes).as("Number of classes at the given URLs").hasSize(2);
    }

    @Test
    public void imports_classes_outside_of_the_classpath() throws IOException {
        Path targetDir = outsideOfClassPath
                .onlyKeep(not(containsPattern("^Missing.*")))
                .setUp(getClass().getResource("testexamples/outsideofclasspath"));

        JavaClasses classes = new ClassFileImporter().importPath(targetDir);

        assertThat(classes).hasSize(5);
        assertThat(classes).extracting("name").containsOnly(
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.ChildClass",
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.MiddleClass",
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.ExistingDependency",
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.ChildClass$MySeed",
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.ExistingDependency$GimmeADescription"
        );

        JavaClass middleClass = findAnyByName(classes,
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.MiddleClass");
        assertThat(middleClass)
                .hasSimpleName("MiddleClass")
                .isInterface(false);
        assertThatCall(findAnyByName(middleClass.getMethodCallsFromSelf(), "println"))
                .isFrom(middleClass.getMethod("overrideMe"))
                .isTo(targetWithFullName(String.format("%s.println(%s)", PrintStream.class.getName(), String.class.getName())))
                .inLineNumber(12);
        assertThatCall(findAnyByName(middleClass.getMethodCallsFromSelf(), "getSomeString"))
                .isFrom(middleClass.getMethod("overrideMe"))
                .isTo(targetWithFullName(
                        "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.MissingDependency.getSomeString()"))
                .inLineNumber(12);

        JavaClass gimmeADescription = findAnyByName(classes,
                "com.tngtech.archunit.core.importer.testexamples.outsideofclasspath.ExistingDependency$GimmeADescription");
        assertThat(gimmeADescription)
                .hasSimpleName("GimmeADescription")
                .isInterface(true);
    }

    @Test
    public void resolve_missing_dependencies_from_classpath_can_be_toggled() {
        ArchConfiguration.get().unsetClassResolver();
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);
        JavaClass clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport")).get(ClassToImportOne.class);

        assertThat(clazz.getRawSuperclass().get().getMethods()).isNotEmpty();

        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/simpleimport")).get(ClassToImportOne.class);

        assertThat(clazz.getRawSuperclass().get().getMethods()).isEmpty();
    }

    @DataProvider
    public static Object[][] classes_not_fully_imported() {
        class Element {
        }
        @SuppressWarnings("unused")
        class DependsOnArray {
            Element[] array;
        }

        return ArchConfigurationRule.resetConfigurationAround(new Callable<Object[][]>() {
            @Override
            public Object[][] call() {
                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);
                JavaClass resolvedFromClasspath = new ClassFileImporter().importClasses(DependsOnArray.class)
                        .get(DependsOnArray.class).getField("array").getRawType().getComponentType();

                ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
                JavaClass stub = new ClassFileImporter().importClasses(DependsOnArray.class)
                        .get(DependsOnArray.class).getField("array").getRawType().getComponentType();

                return $$(
                        $("Resolved from classpath", resolvedFromClasspath),
                        $("Stub class", stub)
                );
            }
        });
    }

    @Test
    @UseDataProvider("classes_not_fully_imported")
    public void classes_not_fully_imported_have_flag_fullyImported_false_and_empty_dependencies(@SuppressWarnings("unused") String description, JavaClass notFullyImported) {
        assertThat(notFullyImported).isFullyImported(false);
        assertThat(notFullyImported.getDirectDependenciesFromSelf()).isEmpty();
        assertThat(notFullyImported.getDirectDependenciesToSelf()).isEmpty();
        assertThat(notFullyImported.getFieldAccessesToSelf()).isEmpty();
        assertThat(notFullyImported.getMethodCallsToSelf()).isEmpty();
        assertThat(notFullyImported.getConstructorCallsToSelf()).isEmpty();
        assertThat(notFullyImported.getAccessesToSelf()).isEmpty();
        assertThat(notFullyImported.getFieldsWithTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getMethodsWithParameterTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getMethodsWithReturnTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getMethodThrowsDeclarationsWithTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getConstructorsWithParameterTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getConstructorsWithThrowsDeclarationTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getAnnotationsWithTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getAnnotationsWithParameterTypeOfSelf()).isEmpty();
        assertThat(notFullyImported.getInstanceofChecksWithTypeOfSelf()).isEmpty();
    }

    @Test
    public void import_is_resilient_against_broken_class_files() throws Exception {
        Class<?> expectedClass = getClass();

        File folder = temporaryFolder.newFolder();
        copyClassFile(expectedClass, folder);
        Files.write(new File(folder, "Evil.class").toPath(), "broken".getBytes(UTF_8));

        logTest.watch(ClassFileProcessor.class, Level.WARN);

        JavaClasses classes = new ClassFileImporter().importPath(folder.toPath());

        assertThatTypes(classes).matchExactly(expectedClass);
        logTest.assertLogMessage(Level.WARN, "Evil.class");
    }

    @Test
    public void class_has_source_of_import() throws Exception {
        ArchConfiguration.get().setMd5InClassSourcesEnabled(true);

        JavaClass clazzFromFile = new ClassFileImporter().importClass(ClassToImportOne.class);
        Source source = clazzFromFile.getSource().get();
        assertThat(source.getUri()).isEqualTo(urlOf(ClassToImportOne.class).toURI());
        assertThat(source.getFileName()).contains(ClassToImportOne.class.getSimpleName() + ".java");
        assertThat(source.getMd5sum()).isEqualTo(md5sumOf(bytesAt(urlOf(ClassToImportOne.class))));

        clazzFromFile = new ClassFileImporter().importClass(ClassWithInnerClass.Inner.class);
        source = clazzFromFile.getSource().get();
        assertThat(source.getUri()).isEqualTo(urlOf(ClassWithInnerClass.Inner.class).toURI());
        assertThat(source.getFileName()).contains(ClassWithInnerClass.class.getSimpleName() + ".java");
        assertThat(source.getMd5sum()).isEqualTo(md5sumOf(bytesAt(urlOf(ClassWithInnerClass.Inner.class))));

        JavaClass clazzFromJar = new ClassFileImporter().importClass(Rule.class);
        source = clazzFromJar.getSource().get();
        assertThat(source.getUri()).isEqualTo(urlOf(Rule.class).toURI());
        assertThat(source.getFileName()).contains(Rule.class.getSimpleName() + ".java");
        assertThat(source.getMd5sum()).isEqualTo(md5sumOf(bytesAt(urlOf(Rule.class))));

        ArchConfiguration.get().setMd5InClassSourcesEnabled(false);
        source = new ClassFileImporter().importClass(ClassToImportOne.class).getSource().get();
        assertThat(source.getMd5sum()).isEqualTo(MD5_SUM_DISABLED);
    }

    @Test
    public void imports_class_objects() {
        JavaClasses classes = new ClassFileImporter().importClasses(ClassToImportOne.class, ClassToImportTwo.class);

        assertThatTypes(classes).matchInAnyOrder(ClassToImportOne.class, ClassToImportTwo.class);
    }

    /**
     * Compare {@link LocationsTest#locations_of_packages_within_JAR_URIs_that_do_not_contain_package_folder()}
     */
    @Test
    public void imports_packages_even_if_jar_entry_for_package_is_missing() {
        String packageToImport = independentClasspathRule.getIndependentTopLevelPackage();

        ClassFileImporter classFileImporter = new ClassFileImporter();
        JavaClasses classes = classFileImporter.importPackages(packageToImport);
        assertThat(classes).extracting("name")
                .doesNotContain(independentClasspathRule.getNameOfSomeContainedClass());

        independentClasspathRule.configureClasspath();

        classes = classFileImporter.importUrl(independentClasspathRule.getOnlyUrl());
        assertThat(classes).extracting("name")
                .containsAll(independentClasspathRule.getNamesOfClasses());
        assertThat(classes).extracting("packageName")
                .containsAll(independentClasspathRule.getPackagesOfClasses());

        classes = classFileImporter.importPackages(packageToImport);
        assertThat(classes).extracting("name").contains(independentClasspathRule.getNameOfSomeContainedClass());
    }

    @Test
    public void imports_paths() throws Exception {
        File exampleFolder = new File(new File(urlOf(getClass()).toURI()).getParentFile(), "testexamples");
        File folderOne = new File(exampleFolder, "pathone");
        File folderTwo = new File(exampleFolder, "pathtwo");

        JavaClasses classes = new ClassFileImporter()
                .importPaths(ImmutableList.of(folderOne.toPath(), folderTwo.toPath()));
        assertThatTypes(classes).matchInAnyOrder(Class11.class, Class12.class, Class21.class, Class22.class);

        classes = new ClassFileImporter().importPaths(folderOne.toPath(), folderTwo.toPath());
        assertThatTypes(classes).matchInAnyOrder(Class11.class, Class12.class, Class21.class, Class22.class);

        classes = new ClassFileImporter().importPaths(folderOne.getAbsolutePath(), folderTwo.getAbsolutePath());
        assertThatTypes(classes).matchInAnyOrder(Class11.class, Class12.class, Class21.class, Class22.class);

        classes = new ClassFileImporter().importPath(folderOne.toPath());
        assertThatTypes(classes).matchInAnyOrder(Class11.class, Class12.class);

        classes = new ClassFileImporter().importPath(folderOne.getAbsolutePath());
        assertThatTypes(classes).matchInAnyOrder(Class11.class, Class12.class);
    }

    @Test
    public void ImportOptions_are_respected() throws Exception {
        ClassFileImporter importer = new ClassFileImporter().withImportOption(importOnly(getClass(), Rule.class));

        assertThatTypes(importer.importPath(Paths.get(urlOf(getClass()).toURI()))).matchExactly(getClass());
        assertThatTypes(importer.importUrl(urlOf(getClass()))).matchExactly(getClass());
        assertThatTypes(importer.importJar(jarFileOf(Rule.class))).matchExactly(Rule.class);
    }

    @Test
    public void is_resilient_against_broken_ClassFileSources() throws MalformedURLException {
        JavaClasses classes = new ClassFileImporter().importUrl(new File("/broken.class").toURI().toURL());
        assertThat(classes).isEmpty();

        classes = new ClassFileImporter().importUrl(new File("/broken.jar").toURI().toURL());
        assertThat(classes).isEmpty();
    }

    private void assertSameSimpleNameOfArchUnitAndReflection(JavaClasses classes, String className) throws ClassNotFoundException {
        assertSameSimpleNameOfArchUnitAndReflection(classes, Class.forName(className));
    }

    private void assertSameSimpleNameOfArchUnitAndReflection(JavaClasses classes, Class<?> clazz) {
        assertThat(classes.get(clazz.getName())).hasSimpleName(clazz.getSimpleName());
    }

    private void copyClassFile(Class<?> clazz, File targetFolder) throws IOException, URISyntaxException {
        Files.copy(Paths.get(urlOf(clazz).toURI()), new File(targetFolder, clazz.getSimpleName() + ".class").toPath());
    }

    private ImportOption importOnly(final Class<?>... classes) {
        return new ImportOption() {
            @Override
            public boolean includes(Location location) {
                for (Class<?> c : classes) {
                    if (location.contains(urlOf(c).getFile())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private Condition<MethodCallTarget> targetWithFullName(final String name) {
        return new Condition<MethodCallTarget>(String.format("target with name '%s'", name)) {
            @Override
            public boolean matches(MethodCallTarget value) {
                return value.getFullName().equals(name);
            }
        };
    }
}
